package grpcclientapp;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import grpcclientapp.exceptions.FailedConnectionException;
import grpcclientapp.exceptions.NoServerIpException;
import grpcclientapp.observers.DownloadImageResponseStream;
import grpcclientapp.observers.GetFileNamesResponseStream;
import grpcclientapp.observers.GetImageCharacteristicsResponseStream;
import grpcclientapp.observers.UploadImageResponseStream;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class App {
    private static final int SERVER_PORT = 8000;
    private static final String DEFAULT_SERVER_ADDRESS = "34.159.185.158"; // "localhost" or "34.141.73.96" (GCP VM external IP)
    private static final String CLOUD_FUNCTION_IP_LOOKUP_URL = "https://europe-west3-cn2324-t1-g04.cloudfunctions.net/funcHttp?instance-group=instance-group-grpc-server";
    private static final boolean developmentMode = false; // TODO: Change to false for production
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceStub noBlockingFunctionalServiceStub;
    private static VisionFlowScalingServiceGrpc.VisionFlowScalingServiceStub noBlockingScalingServiceStub;
    private static ManagedChannel channel;
    private static final String[] languages = {"pt", "fr", "es", "de", "it", "ru", "ja", "ko", "zh"};

    public static void main(String[] args) {
        try {
            System.out.println("In development mode: " + developmentMode);
            establishConnectionToServer();
            noBlockingFunctionalServiceStub = VisionFlowFunctionalServiceGrpc.newStub(channel);
            noBlockingScalingServiceStub = VisionFlowScalingServiceGrpc.newStub(channel);
            int option;
            do {
                checkServerConnectionAndReconnectIfNeeded();
                System.out.println("\n######## MENU ##########");
                System.out.println("1: Upload Image");
                System.out.println("2: Download Image");
                System.out.println("3: Get Image Characteristics");
                System.out.println("4: Get Images by Date and Characteristic");
                System.out.println("5: List Managed Instance Groups");
                System.out.println("6: List Managed Instance Group VMs");
                System.out.println("7: Resize Managed Instance Group");
                System.out.println("0: Exit");
                System.out.println("########################");
                option = readInt("Enter an Option: \n");
                switch (option) {
                    case 1:
                        uploadImage();
                        break;
                    case 2:
                        downloadImage();
                        break;
                    case 3:
                        getImageCharacteristics();
                        break;
                    case 4:
                        getImagesByDateAndCharacteristic();
                        break;
                    case 5:
                        listManagedInstanceGroups();
                        break;
                    case 6:
                        listManagedInstanceGroupVMs();
                        break;
                    case 7:
                        resizeManagedInstanceGroup();
                        break;
                    case 0:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            } while (option != 0);
        } catch (Throwable e) {
            logger.severe("Error: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
            logger.info("Shutting down client...");
        }
    }

    private static void checkServerConnectionAndReconnectIfNeeded() throws Throwable {
        // get the current state of the channel
        ConnectivityState state = channel.getState(true);
        logger.info("Channel state: " + state);
        // IMPORTANT NOTE: grpc channel has a retry mechanism built-in with a backoff strategy
        //  but if the connection is broken, do not use the channel's retry mechanism
        //  use ours instead
        // SOURCE: https://grpc.github.io/grpc/core/md_doc_connectivity-semantics-and-api.html
        if (state != ConnectivityState.READY) {
            establishConnectionToServer();
        }
    }

    private static final Predicate<Throwable> isConnectionError = e -> e instanceof FailedConnectionException || e instanceof NoServerIpException;

    private static void establishConnectionToServer() throws Throwable {
        // Using a retry mechanism to establish connection to the server
        // Retry mechanism will try to connect to the server 5 times with a 3-second wait between each attempt
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(5) // 5 attempts (1 initial + 4 retries)
                .retryOnException(isConnectionError)
                // Wait 1 second before the first retry, then 2 seconds, 4 seconds, 8 seconds, etc.
                .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))
                .build();
        Retry retry = Retry.of("connect-to-server", config);
        retry.getEventPublisher().onEvent(event -> logger.info("Retry event: " + event));

        CheckedSupplier<ManagedChannel> supplier = Retry.decorateCheckedSupplier(retry, () -> {
            String serverIp = searchForAServerIp();
            if (serverIp == null) {
                throw new NoServerIpException("No server IP found");
            }

            channel = ManagedChannelBuilder.forAddress(serverIp, SERVER_PORT)
                    .usePlaintext()
                    .build();

            logger.info("Trying to establish connection to server...");

            do {
                // get the current state of the channel
                switch (channel.getState(true)) {
                    case READY:
                        logger.info("Successfully connected to server at: " + serverIp);
                        return channel;
                    case TRANSIENT_FAILURE:
                    case SHUTDOWN:
                        throw new FailedConnectionException("Failed to connect to server at: " + serverIp);
                    case CONNECTING:
                    case IDLE:
                    default:
                }
            } while (true);
        });

        channel = supplier.get();
    }

    private static String searchForAServerIp() {
        try {
            if (developmentMode) {
                return DEFAULT_SERVER_ADDRESS;
            }
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUD_FUNCTION_IP_LOOKUP_URL))
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                logger.severe("Error looking up service IP address: " + httpResponse.statusCode());
                return null;
            }
            String response = httpResponse.body();
            if (response.isEmpty() && response.isBlank()) {
                System.out.println("No IPs found.");
                return null;
            }
            String[] ips = response.split(";");
            System.out.println("IPs found:");
            for (int i = 0; i < ips.length; i++) {
                String ip = ips[i];
                System.out.println("[" + ((i)) + "]: " + ip);
            }
            // ask user to choose an IP to connect to
            int index = readInt("Enter the index of the IP to connect to: ");
            return ips[index];

        } catch (IOException | InterruptedException e) {
            logger.severe("Error looking up service IP address: " + e.getMessage());
            return null;
        }
    }

    private static void getImageCharacteristics() {
        String id = readString("Enter the image id to get characteristics (e.g., cat#7db8634f-8eed-4c27-aa05-f88b5b87a296): ");
        GetImageCharacteristicsRequest request = GetImageCharacteristicsRequest.newBuilder()
                .setId(id)
                .build();
        StreamObserver<GetImageCharacteristicsResponse> responseStream =
                new GetImageCharacteristicsResponseStream();
        noBlockingFunctionalServiceStub.getImageCharacteristics(request, responseStream);
    }

    private static void getImagesByDateAndCharacteristic() {
        String startDate = readString("Enter the start date (e.g., 20-04-2024): ");
        String endDate = readString("Enter the end date (e.g., 31-05-2024): ");
        String characteristic = readString("Enter the characteristic to filter by (e.g., Gato): ");
        GetFileNamesRequest request = GetFileNamesRequest.newBuilder()
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setCharacteristic(characteristic)
                .build();
        StreamObserver<GetFileNamesResponse> responseStream =
                new GetFileNamesResponseStream();
        noBlockingFunctionalServiceStub.getFileNamesByCharacteristic(request, responseStream);
    }

    private static void uploadImage() throws IOException {
        String imagePath = readString("Enter the path of the image to upload (e.g., project/grpc-client/src/main/java/resources/cat.jpg): ");
        String translationlang = readString("Enter the language to translate the image to [" + String.join(", ", languages) + "]: ");
        StreamObserver<UploadImageResponse> responseStream = new UploadImageResponseStream();
        StreamObserver<UploadImageRequest> streamToAddImageBytes = noBlockingFunctionalServiceStub.uploadImage(responseStream);
        // Read bytes from file and send to server
        final Path path = Paths.get(imagePath);
        // parse path to get file name (e.g. /path/to/image.jpg -> image)
        final String fileName = path.getFileName().toString().split("\\.")[0];
        // get content type of the image (e.g. image/jpeg)
        final String contentType = Files.probeContentType(path);
        // Define buffer size for reading chunks (e.g., 4 KB)
        final int bufferSize = 4096;
        final byte[] buffer = new byte[bufferSize];

        try (InputStream inputStream = Files.newInputStream(path)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                ByteString chunk = ByteString.copyFrom(buffer, 0, bytesRead);
                UploadImageRequest imageUploadData = UploadImageRequest.newBuilder()
                        .setName(fileName)
                        .setContentType(contentType)
                        .setTranslationLang(translationlang)
                        .setChunk(chunk)
                        .build();
                streamToAddImageBytes.onNext(imageUploadData);
            }
        } catch (IOException e) {
            // Handle IO exception
            logger.severe("Error reading file: " + e.getMessage());
        }
        streamToAddImageBytes.onCompleted();
    }

    private static void downloadImage() {
        String id = readString("Enter the image id to download (e.g., cat#7db8634f-8eed-4c27-aa05-f88b5b87a296): ");
        String dir = readString("Enter the directory to download the image to (e.g., project/grpc-client/downloaded-imgs): ");
        DownloadImageRequest imageDownloadData = DownloadImageRequest.newBuilder()
                .setId(id)
                .setPath(dir)
                .build();
        StreamObserver<DownloadImageResponse> responseStream = new DownloadImageResponseStream(imageDownloadData);
        noBlockingFunctionalServiceStub.downloadImage(imageDownloadData, responseStream);
    }

    private static void listManagedInstanceGroups() {
        StreamObserver<ManagedInstanceGroupResponse> responseStream = new StreamObserver<>() {
            @Override
            public void onNext(ManagedInstanceGroupResponse managedInstanceGroupResponse) {
                System.out.println("Managed Instance Group: " + managedInstanceGroupResponse.getName());
            }

            @Override
            public void onError(Throwable throwable) {
                logger.severe("Error listing managed instance groups: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("Listed managed instance groups");
            }
        };
        noBlockingScalingServiceStub.listManagedInstanceGroups(Empty.newBuilder().build(), responseStream);
    }

    private static void resizeManagedInstanceGroup() {
        String instanceGroupName = readString("Enter the instance group name to resize (e.g., instance-group-grpc-server): ");
        int newSize = readInt("Enter the new size for the instance group: (e.g., 3): ");
        ManagedInstanceGroupResizeRequest request = ManagedInstanceGroupResizeRequest.newBuilder()
                .setName(instanceGroupName)
                .setNewSize(newSize)
                .build();
        StreamObserver<Empty> responseStream = new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
                System.out.println("Resizing instance group " + instanceGroupName + " to " + newSize + " instances");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error resizing instance group: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Resizing instance group completed");
            }
        };
        noBlockingScalingServiceStub.resizeManagedInstanceGroup(request, responseStream);
    }

    private static void listManagedInstanceGroupVMs() {
        String instanceGroupName = readString("Enter the instance group name to list VMs (e.g., instance-group-grpc-server): ");
        ManagedInstanceNameRequest request = ManagedInstanceNameRequest.newBuilder()
                .setName(instanceGroupName)
                .build();
        StreamObserver<ManagedInstanceGroupVMResponse> responseStream = new StreamObserver<>() {
            @Override
            public void onNext(ManagedInstanceGroupVMResponse managedInstanceGroupVMResponse) {
                System.out.println("VM Name: " + managedInstanceGroupVMResponse.getName() + "with status: " + managedInstanceGroupVMResponse.getStatus());
            }

            @Override
            public void onError(Throwable throwable) {
                logger.severe("Error listing managed instance group VMs: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("Listed managed instance group VMs");
            }
        };
        noBlockingScalingServiceStub.listManagedInstanceGroupVMs(request, responseStream);
    }

    private static String readString(String msg) {
        Scanner input = new Scanner(System.in);
        if (msg != null) {
            System.out.print(msg);
        }
        return input.nextLine().trim();
    }

    private static int readInt(String msg) {
        Scanner input = new Scanner(System.in);
        if (msg != null) {
            System.out.print(msg);
        }
        return input.nextInt();
    }

}
