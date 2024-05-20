package receiver;

@FunctionalInterface
public interface LoggingMessageHandlerFunction {
    void handleRequest(
            String requestId,
            String imageName,
            String bucketName,
            String blobName,
            String translation
    );
}
