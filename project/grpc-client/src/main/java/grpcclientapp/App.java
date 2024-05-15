package grpcclientapp;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import servicestubs.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class App {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceBlockingStub blockingStub;

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
            blockingStub = VisionFlowFunctionalServiceGrpc.newBlockingStub(channel);
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
                        ImageUploadedData res = blockingStub.uploadImage(imageFile);
                        System.out.println("Image uploaded with id: " + res.getId());
                        break;
                    case 2:
                        String id = read("Enter the image id to download (e.g., gs://lab3-bucket-g04-europe/cat#jpeg): ");
                        String dirToDownloadTo = read("Enter the directory to download the image to (e.g., project/grpc-client/downloaded-imgs): ");
                        ImageDownloadData imageDownloadData = ImageDownloadData.newBuilder()
                                .setId(id)
                                .setPath(dirToDownloadTo)
                                .build();
                        ImageDownloadedData donwloadedImage = blockingStub.downloadImage(imageDownloadData);
                        storeImageLocally(donwloadedImage, imageDownloadData.getPath());
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

    public static void storeImageLocally(ImageDownloadedData downloadedImage, String directory) throws IOException {
        ByteString imageDataBytes = downloadedImage.getData();
        createDirectoryIfNotExists(directory);
        // Write the image data to a file
        // parse the image name from the id (e.g. cat#jpeg -> cat.jpeg)
        String imageNameWithExt = downloadedImage.getName().replace("#", ".");
        String filePath = directory + "/" + imageNameWithExt;
        System.out.println("Storing image into file: " + filePath);
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(imageDataBytes.toByteArray());
        }
    }

    private static void createDirectoryIfNotExists(String directory) {
        // Create directory if it doesn't exist
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static String read(String msg) {
        Scanner input = new Scanner(System.in);
        System.out.print(msg);
        return input.nextLine().trim();
    }

    private static ImageUploadData createImageFile(String imagePath) throws Exception {
        Path path = Paths.get(imagePath);
        // parse path to get file name (e.g. /path/to/image.jpg -> image)
        String fileName = path.getFileName().toString().split("\\.")[0];
        // get content type of the image (e.g. image/jpeg)
        String contentType = Files.probeContentType(path);
        // read file content
        ByteString data = ByteString.readFrom(Files.newInputStream(path));
        return ImageUploadData.newBuilder()
                .setName(fileName)
                .setContentType(contentType)
                .setData(data)
                .build();
    }

}
