package grcpserver.services.cloudstorage;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CloudStorageOperations {

    Storage storage;
    int ONE_MB = 1024 * 1024;

    public CloudStorageOperations(Storage storage) {
        this.storage = storage;
    }

    public void uploadBlobToBucket(
            String bucketName,
            String blobName,
            byte[] data,
            String contentType
    ) throws IOException {
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        if (data.length > ONE_MB) {
            // When content is not available or large (1MB or more) it is recommended
            // to write it in chunks via the blob's channel writer.
            try (WriteChannel writer = storage.writer(blobInfo)) {
                ByteBuffer buffer = ByteBuffer.wrap(data);
                while (buffer.hasRemaining()) {
                    writer.write(buffer);
                }
            }
        } else {
            // create the blob in one request.
            storage.create(blobInfo, data);
        }
        System.out.println("Created blob <" + blobName + "> in bucket <" + bucketName + ">");
    }

    public byte[] downloadBlobFromBucket(BlobId blobId) {
        Blob blob = storage.get(blobId);
        return blob.getContent();
    }
}
