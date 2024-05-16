package app;

public class PubSubImageMessage {
    private final String id;
    private final String bucketName;
    private final String blobName;
    private final String translationLang;

    public PubSubImageMessage(String id, String bucketName, String blobName, String translationLang) {
        this.id = id;
        this.bucketName = bucketName;
        this.blobName = blobName;
        this.translationLang = translationLang;
    }

    public String getId() {
        return id;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBlobName() {
        return blobName;
    }

    public String getTranslationLang() {
        return translationLang;
    }
}
