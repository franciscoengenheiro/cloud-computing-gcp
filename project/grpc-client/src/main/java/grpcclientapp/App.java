package grpcclientapp;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.InstanceGroupManagersClient;
import com.google.cloud.compute.v1.ListManagedInstancesInstanceGroupManagersRequest;
import com.google.cloud.compute.v1.ManagedInstance;
import com.google.cloud.compute.v1.Operation;
import com.google.protobuf.ByteString;
import grpcclientapp.observers.DownloadImageResponseStream;
import grpcclientapp.observers.GetFileNamesResponseStream;
import grpcclientapp.observers.GetImageCharacteristicsResponseStream;
import grpcclientapp.observers.UploadImageResponseStream;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class App {
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceStub noBlockingStub;
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static final String PROJECT_ID = "cn2324-t1-g05";
    private static final String ZONE = "europe-west1-b";
    private static final String LABELS_APP_INSTANCE_GROUP_NAME = "instance-group-labels-app";
    private static final String GRPC_SERVER_INSTANCE_GROUP_NAME = "instance-group-grpc-server";

    private static InstanceGroupManagersClient managersClient;

    public static void main(String[] args) {
        try {
            managersClient = InstanceGroupManagersClient.create();
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }
            logger.info("connect to " + svcIP + ":" + svcPort);
            // Channels are secure by default (via SSL/TLS).
            // For the example we disable TLS to avoid
            // needing certificates.
            ManagedChannel channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    // Channels are secure by default (via SSL/TLS).
                    // For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build();
            noBlockingStub = VisionFlowFunctionalServiceGrpc.newStub(channel);
            int option;
            do {
                System.out.println("\n######## MENU ##########");
                System.out.println("1: Upload Image");
                System.out.println("2: Download Image");
                System.out.println("3: Get Image Characteristics");
                System.out.println("4: Get Images by Date and Characteristic");
                System.out.println("5: List gRPC Server VM instances");
                System.out.println("6: Resize gRPC Server VM instances");
                System.out.println("7: List Labels App VM instances");
                System.out.println("8: Resize Labels App VM instances");
                System.out.println("0: Exit");
                System.out.println("########################");
                System.out.print("Enter an Option: ");
                option = readInt(null);
                switch (option) {
                    case 1: uploadImage(); break;
                    case 2: downloadImage(); break;
                    case 3: getImageCharacteristics(); break;
                    case 4: getImagesByDateAndCharacteristic(); break;
                    case 5: listManagedInstanceGroupVMs(GRPC_SERVER_INSTANCE_GROUP_NAME); break;
                    case 6: resizeManagedInstanceGroup(GRPC_SERVER_INSTANCE_GROUP_NAME); break;
                    case 7: listManagedInstanceGroupVMs(LABELS_APP_INSTANCE_GROUP_NAME); break;
                    case 8: resizeManagedInstanceGroup(LABELS_APP_INSTANCE_GROUP_NAME); break;
                    case 0: System.out.println("Exiting..."); break;
                    default: System.out.println("Invalid option. Please try again.");
                }
            } while (option != 0);
        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
        }
    }

    private static void getImageCharacteristics() {
        String id = read("Enter the image id to get characteristics (e.g., cat#7db8634f-8eed-4c27-aa05-f88b5b87a296): ");
        GetImageCharacteristicsRequest request = GetImageCharacteristicsRequest.newBuilder()
                .setId(id)
                .build();
        StreamObserver<GetImageCharacteristicsResponse> responseStream =
                new GetImageCharacteristicsResponseStream();
        noBlockingStub.getImageCharacteristics(request, responseStream);
    }

    private static void getImagesByDateAndCharacteristic() {
        String startDate = read("Enter the start date (e.g., 20-04-2024): ");
        String endDate = read("Enter the end date (e.g., 31-05-2024): ");
        String characteristic = read("Enter the characteristic to filter by (e.g., Gato): ");
        GetFileNamesRequest request = GetFileNamesRequest.newBuilder()
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setCharacteristic(characteristic)
                .build();
        StreamObserver<GetFileNamesResponse> responseStream =
                new GetFileNamesResponseStream();
        noBlockingStub.getFileNamesByCharacteristic(request, responseStream);
    }

    private static void uploadImage() throws IOException {
        String imagePath = read("Enter the path of the image to upload (e.g., project/grpc-client/src/main/java/resources/cat.jpg): ");
        String translationlang = read("Enter the language to translate the image to (e.g., pt, fr, es): ");
        StreamObserver<UploadImageResponse> responseStream = new UploadImageResponseStream();
        StreamObserver<UploadImageRequest> streamToAddImageBytes = noBlockingStub.uploadImage(responseStream);
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
            e.printStackTrace();
        }
        streamToAddImageBytes.onCompleted();
    }

    private static void downloadImage() {
        String id = read("Enter the image id to download (e.g., cat#7db8634f-8eed-4c27-aa05-f88b5b87a296): ");
        String dir = read("Enter the directory to download the image to (e.g., project/grpc-client/downloaded-imgs): ");
        DownloadImageRequest imageDownloadData = DownloadImageRequest.newBuilder()
                .setId(id)
                .setPath(dir)
                .build();
        StreamObserver<DownloadImageResponse> responseStream = new DownloadImageResponseStream(imageDownloadData);
        noBlockingStub.downloadImage(imageDownloadData, responseStream);
    }

    static void listManagedInstanceGroupVMs(String instanceGroupName) {
        ListManagedInstancesInstanceGroupManagersRequest request =
                ListManagedInstancesInstanceGroupManagersRequest.newBuilder()
                        .setInstanceGroupManager(instanceGroupName)
                        .setProject(PROJECT_ID)
                        .setReturnPartialSuccess(true)
                        .setZone(ZONE)
                        .build();

        System.out.println("Instances of instance group: " + instanceGroupName);
        for (ManagedInstance instance :
                managersClient.listManagedInstances(request).iterateAll()) {
            System.out.println(instance.getInstance() + " with STATUS = " + instance.getInstanceStatus());
        }
    }

    static void resizeManagedInstanceGroup(String instanceGroupName) throws InterruptedException, ExecutionException {
        int newSize = readInt("Enter the new size for " + instanceGroupName + ": ");
        OperationFuture<Operation, Operation> result = managersClient.resizeAsync(
                PROJECT_ID,
                ZONE,
                instanceGroupName,
                newSize
        );
        Operation oper = result.get();
        System.out.println("Resizing with status " + oper.getStatus());
    }

    private static String read(String msg) {
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
