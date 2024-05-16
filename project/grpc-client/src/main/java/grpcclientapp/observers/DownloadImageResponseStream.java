package grpcclientapp.observers;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import servicestubs.DownloadImageRequest;
import servicestubs.DownloadImageResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DownloadImageResponseStream implements StreamObserver<DownloadImageResponse> {

    private final DownloadImageRequest request;

    public DownloadImageResponseStream(DownloadImageRequest request) {
        this.request = request;
    }

    @Override
    public void onNext(DownloadImageResponse downloadImageResponse) {
        try {
            storeImageLocally(downloadImageResponse, request.getPath());
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

    private static void storeImageLocally(
            DownloadImageResponse downloadedImage,
            String directory
    ) throws IOException {
        ByteString imageDataBytes = downloadedImage.getData();
        createDirectoryIfNotExists(directory);
        // Write the image data to a file
        // parse the image name from the id (e.g. cat#jpeg -> cat.jpeg)
        String imageNameWithExt = downloadedImage.getName().replace("#", ".");
        String filePath = directory + "/" + imageNameWithExt;
        System.out.println("\nStoring image into file: " + filePath);
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
