package grcpservices;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.util.Objects;

public class VisionFlowFunctionalService extends VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceImplBase {
    // TODO: should the bucket be created if it doesn't exist yet?
    private final String bucketName = "lab3-bucket-g04-europe";
    private final StorageOperations storageOperations;

    public VisionFlowFunctionalService() {
        StorageOptions storageOperations = StorageOptions.getDefaultInstance();
        Storage storage = storageOperations.getService();
        String projectId = storageOperations.getProjectId();
        Objects.requireNonNull(projectId, "GOOGLE_APPLICATION_CREDENTIALS environment variable not set");
        System.out.println("Connected to storage for project: " + projectId);
        this.storageOperations = new StorageOperations(storage);
    }

    @Override
    public void uploadImage(ImageUploadData request, StreamObserver<ImageUploadedData> responseObserver) {
        System.out.println("Uploading image to bucket");
        // get the content type of the image (e.g. image/jpeg)
        String contentType = request.getContentType();
        // parse extension from content type (e.g. image/jpeg -> jpeg)
        String extension = contentType.split("/")[1];
        // create the blob name by concatenating the image name and the extension (e.g. cat#jpeg),
        // because blob name cant be used with "."
        String blobName = request.getName() + "#" + extension;
        byte[] imageData = request.getData().toByteArray();
        try {
            BlobId id = storageOperations.uploadBlobToBucket(bucketName, blobName, imageData, contentType);
            ImageUploadedData response = ImageUploadedData.newBuilder()
                    .setId(id.toGsUtilUri()) // gs://bucketName/blobName
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void downloadImage(ImageDownloadData request, StreamObserver<ImageDownloadedData> responseObserver) {
        System.out.println("Downloading image from bucket");
        BlobId blobId = BlobId.fromGsUtilUri(request.getId());
        try {
            byte[] imageBytes = storageOperations.downloadBlobFromBucket(blobId);
            ImageDownloadedData response = ImageDownloadedData.newBuilder()
                    .setData(ByteString.copyFrom(imageBytes))
                    .setName(blobId.getName())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
