package grpcclientapp.observers;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import servicestubs.ImageDownloadData;
import servicestubs.ImageDownloadedData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageDownloadedStreamObserver implements StreamObserver<ImageDownloadedData> {

    private final ImageDownloadData imageDownloadData;

    public ImageDownloadedStreamObserver(ImageDownloadData imageDownloadData) {
        this.imageDownloadData = imageDownloadData;
    }

    @Override
    public void onNext(ImageDownloadedData imageDownloadedData) {
        try {
            storeImageLocally(imageDownloadedData, imageDownloadData.getPath());
        } catch (IOException e) {
            System.out.println("\nError storing image: " + e.getMessage());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("\nError downloading image: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        System.out.println("\nImage downloaded successfully");
    }

    private static void storeImageLocally(ImageDownloadedData downloadedImage, String directory) throws IOException {
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
}
