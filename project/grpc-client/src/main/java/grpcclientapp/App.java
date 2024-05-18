package grpcclientapp;

import com.google.protobuf.ByteString;
import grpcclientapp.observers.DownloadImageResponseStream;
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

public class App {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceStub noBlockingStub;

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }
            System.out.println("connect to " + svcIP + ":" + svcPort);
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
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
                System.out.println("0: Exit");
                System.out.println("########################");
                System.out.print("Enter an Option: ");
                Scanner scanner = new Scanner(System.in);
                option = scanner.nextInt();
                switch (option) {
                    case 1:
                        String path = read("Enter the path of the image to upload (e.g., project/grpc-client/src/main/java/resources/cat.jpg): ");
                        String translationlang = read("Enter the language to translate the image to (e.g., pt): ");
                        uploadImage(path, translationlang);
                        break;
                    case 2:
                        String id = read("Enter the image id to download (e.g., gs://lab3-bucket-g04-europe/cat#jpeg): ");
                        String dirToDownloadTo = read("Enter the directory to download the image to (e.g., project/grpc-client/downloaded-imgs): ");
                        downloadImage(id, dirToDownloadTo);
                        break;
                    case 0:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            } while (option != 0);
        } catch (Exception e) {
            System.out.println("An error occurred" + e);
        }
    }

    private static String read(String msg) {
        Scanner input = new Scanner(System.in);
        System.out.print(msg);
        return input.nextLine().trim();
    }

    private static void uploadImage(String imagePath, String translationlang) throws IOException {
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
                System.out.println("Sent chunk of image data");
            }
        } catch (IOException e) {
            // Handle IO exception
            e.printStackTrace();
        }
        streamToAddImageBytes.onCompleted();
    }

    private static void downloadImage(String id, String dirToDownloadTo) {
        DownloadImageRequest imageDownloadData = DownloadImageRequest.newBuilder()
                .setId(id)
                .setPath(dirToDownloadTo)
                .build();
        StreamObserver<DownloadImageResponse> responseStream = new DownloadImageResponseStream(imageDownloadData);
        noBlockingStub.downloadImage(imageDownloadData, responseStream);
    }

}
