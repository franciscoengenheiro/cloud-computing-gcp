package grcpserver.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import grcpserver.services.cloudpubsub.CloudPubSubOperations;
import grcpserver.services.cloudstorage.CloudStorageOperations;
import grcpserver.services.firestore.FirestoreOperations;
import grcpserver.services.firestore.ProcessedImageData;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class VisionFlowFunctionalService extends VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceImplBase {
    private final String bucketName = "lab3-bucket-g04-europe";
    private final CloudStorageOperations cloudStorageOperations;
    private final CloudPubSubOperations cloudPubSubOperations;
    private final FirestoreOperations firestoreOperations;
    private final Logger logger = Logger.getLogger(VisionFlowFunctionalService.class.getName());

    public VisionFlowFunctionalService() {
        StorageOptions storageOperations = StorageOptions.getDefaultInstance();
        Storage storage = storageOperations.getService();
        String projectId = storageOperations.getProjectId();
        Objects.requireNonNull(projectId, "GOOGLE_APPLICATION_CREDENTIALS environment variable not set");
        logger.info("Connected to project: " + projectId);
        this.cloudStorageOperations = new CloudStorageOperations(storage);
        this.cloudPubSubOperations = new CloudPubSubOperations(projectId);
        this.firestoreOperations = new FirestoreOperations(storage.getOptions().getCredentials());
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(StreamObserver<UploadImageResponse> responseObserver) {
        return new StreamObserver<>() {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // To accumulate image bytes
            String contentType;
            String imageName;
            String translationLang;

            @Override
            public void onNext(UploadImageRequest request) {
                // on the first request, get all the metadata
                if (contentType == null) {
                    contentType = request.getContentType();
                    imageName = request.getName();
                    translationLang = request.getTranslationLang();
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
                logger.severe("Error uploading image: " + throwable.getMessage());
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                // parse extension from content type (e.g. image/jpeg -> jpeg)
                String extension = contentType.split("/")[1];
                // create the blob name by concatenating the image name and the extension (e.g. cat#jpeg),
                // because blob name cant be used with "."
                String blobName = imageName + "#" + extension;
                // All image bytes have been received, get the accumulated image bytes
                byte[] imageData = outputStream.toByteArray();
                try {
                    cloudStorageOperations.uploadBlobToBucket(bucketName, blobName, imageData, contentType);
                    UploadImageResponse response = UploadImageResponse.newBuilder()
                            .setId(blobName)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    cloudPubSubOperations.publishMessage(blobName, imageName, bucketName, blobName, translationLang);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void downloadImage(DownloadImageRequest request, StreamObserver<DownloadImageResponse> responseObserver) {
        BlobId blobId = BlobId.of(bucketName, request.getId());
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

    @Override
    public void getImageCharacteristics(
            GetImageCharacteristicsRequest request,
            StreamObserver<GetImageCharacteristicsResponse> responseObserver
    ) {
        try {
            ProcessedImageData processedImageData = firestoreOperations.getImageCharacteristcs(request.getId());
            GetImageCharacteristicsResponse response = GetImageCharacteristicsResponse.newBuilder()
                    .setDate(processedImageData.getTimestamp())
                    .addAllLabels(processedImageData.getLabels())
                    .addAllTranslations(processedImageData.getTranslatedLabels())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
