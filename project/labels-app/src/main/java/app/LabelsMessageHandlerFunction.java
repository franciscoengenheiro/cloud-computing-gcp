package app;

@FunctionalInterface
public interface LabelsMessageHandlerFunction {
    public void handleRequest(
            String requestId,
            String imageName,
            String timestamp,
            String bucketName,
            String blobName,
            String translation
    );
}
