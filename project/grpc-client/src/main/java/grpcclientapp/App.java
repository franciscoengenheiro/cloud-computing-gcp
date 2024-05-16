package grpcclientapp;

import grpcclientapp.observers.ImageDownloadedStreamObserver;
import grpcclientapp.observers.ImageUploadedStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.util.Scanner;

import static grpcclientapp.observers.ImageUploadedStreamObserver.createImageFile;

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
                        ImageUploadData imageFile = createImageFile(path);
                        StreamObserver<ImageUploadedData> uploadImageStreamObserver = new ImageUploadedStreamObserver(imageFile);
                        noBlockingStub.uploadImage(imageFile, uploadImageStreamObserver);
                        break;
                    case 2:
                        String id = read("Enter the image id to download (e.g., gs://lab3-bucket-g04-europe/cat#jpeg): ");
                        String dirToDownloadTo = read("Enter the directory to download the image to (e.g., project/grpc-client/downloaded-imgs): ");
                        ImageDownloadData imageDownloadData = ImageDownloadData.newBuilder()
                                .setId(id)
                                .setPath(dirToDownloadTo)
                                .build();
                        StreamObserver<ImageDownloadedData> downloadImageStreamObserver
                                = new ImageDownloadedStreamObserver(imageDownloadData);
                        noBlockingStub.downloadImage(imageDownloadData, downloadImageStreamObserver);
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

}
