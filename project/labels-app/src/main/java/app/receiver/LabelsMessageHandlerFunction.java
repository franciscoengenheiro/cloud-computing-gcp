package app.receiver;

@FunctionalInterface
public interface LabelsMessageHandlerFunction {
    void handleRequest(
            String requestId,
            String imageName,
            String bucketName,
            String blobName,
            String translation
    );
}
