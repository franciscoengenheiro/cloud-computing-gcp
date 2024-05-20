package grcpserver.services.cloudstorage;

public class DownloadedBlobData {
    private final byte[] data;
    private final String contentType;

    public DownloadedBlobData(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }
}
