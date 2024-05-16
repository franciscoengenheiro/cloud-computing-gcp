package grcpserver.services.labels;

public record PubSubImageMessage(
        String id,
        String bucketName,
        String blobName,
        String translationLang
) {

}
