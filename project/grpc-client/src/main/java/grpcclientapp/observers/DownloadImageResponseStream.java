package grpcclientapp.observers;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import servicestubs.DownloadImageRequest;
import servicestubs.DownloadImageResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class DownloadImageResponseStream implements StreamObserver<DownloadImageResponse> {

    private final Logger logger = Logger.getLogger(DownloadImageResponseStream.class.getName());
    private final DownloadImageRequest request;

    public DownloadImageResponseStream(DownloadImageRequest request) {
        this.request = request;
    }

    @Override
    public void onNext(DownloadImageResponse downloadImageResponse) {
        try {
            storeImageLocally(downloadImageResponse, request.getPath());
        } catch (IOException e) {
            logger.severe("Error storing image: " + e.getMessage());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.severe("Error downloading image: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        logger.info("Image downloaded successfully");
    }

    private static void storeImageLocally(
            DownloadImageResponse downloadedImage,
            String directory
    ) throws IOException {
        ByteString imageDataBytes = downloadedImage.getData();
        createDirectoryIfNotExists(directory);
        // Write the image data to a file
        System.out.println(downloadedImage.getName());
        System.out.println(downloadedImage.getContentType());
        // getContentType() returns the file extension (e.g., "image.jpeg")
        String contentType = downloadedImage.getContentType().split("/")[1];
        String filePath = directory + "/" + downloadedImage.getName() + "." + contentType;
        System.out.println("Storing image at: " + filePath);
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
