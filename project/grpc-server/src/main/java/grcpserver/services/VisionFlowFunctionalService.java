package grcpserver.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import grcpserver.services.cloudstorage.CloudStorageOperations;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class VisionFlowFunctionalService extends VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceImplBase {
    // TODO: should the bucket be created if it doesn't exist yet?
    private final String bucketName = "lab3-bucket-g04-europe";
    private final CloudStorageOperations cloudStorageOperations;

    public VisionFlowFunctionalService() {
        StorageOptions storageOperations = StorageOptions.getDefaultInstance();
        Storage storage = storageOperations.getService();
        String projectId = storageOperations.getProjectId();
        Objects.requireNonNull(projectId, "GOOGLE_APPLICATION_CREDENTIALS environment variable not set");
        System.out.println("Connected to storage for project: " + projectId);
        this.cloudStorageOperations = new CloudStorageOperations(storage);
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(StreamObserver<UploadImageResponse> responseObserver) {
        return new StreamObserver<>() {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // To accumulate image bytes
            String contentType;
            String imageName;

            @Override
            public void onNext(UploadImageRequest request) {
                // concatenate received chunks of image data
                System.out.println("[Server] Received chunk of image data");
                if (contentType == null) {
                    contentType = request.getContentType();
                    imageName = request.getName();
                }
                byte[] chunk = request.getChunk().toByteArray();
                try {
                    outputStream.write(chunk); // append chunk to the output stream
                } catch (IOException e) {
                    onError(e);
                }

            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error uploading image to server");
            }

            @Override
            public void onCompleted() {
                System.out.println("Uploading image to bucket");
                // parse extension from content type (e.g. image/jpeg -> jpeg)
                String extension = contentType.split("/")[1];
                // create the blob name by concatenating the image name and the extension (e.g. cat#jpeg),
                // because blob name cant be used with "."
                String blobName = imageName + "#" + extension;
                // All image bytes have been received, get the accumulated image bytes
                byte[] imageData = outputStream.toByteArray();
                try {
                    cloudStorageOperations.uploadBlobToBucket(bucketName, blobName, imageData, contentType);
                    BlobId id = BlobId.of(bucketName, blobName);
                    UploadImageResponse response = UploadImageResponse.newBuilder()
                            .setId(id.toGsUtilUri())
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
    }

    @Override
    public void downloadImage(DownloadImageRequest request, StreamObserver<DownloadImageResponse> responseObserver) {
        System.out.println("Downloading image from bucket");
        BlobId blobId = BlobId.fromGsUtilUri(request.getId());
        try {
            byte[] imageBytes = cloudStorageOperations.downloadBlobFromBucket(blobId);
            DownloadImageResponse response = DownloadImageResponse.newBuilder()
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
