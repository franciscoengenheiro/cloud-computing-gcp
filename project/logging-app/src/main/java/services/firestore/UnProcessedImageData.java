package services.firestore;

public class UnProcessedImageData {
    private final String id;
    private final String imageName;
    private final String bucketName;
    private final String blobName;
    private final String translationLang;

    public UnProcessedImageData(String id,
                                String imageName,
                                String bucketName,
                                String blobName,
                                String translationLang) {
        this.id = id;
        this.imageName = imageName;
        this.bucketName = bucketName;
        this.blobName = blobName;
        this.translationLang = translationLang;
    }

    public String getId() {
        return id;
    }

    public String getImageName() {
        return imageName;
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
