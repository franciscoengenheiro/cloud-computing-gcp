package grcpserver.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import grcpserver.services.cloudpubsub.CloudPubSubOperations;
import grcpserver.services.cloudstorage.CloudStorageOperations;
import grcpserver.services.cloudstorage.DownloadedBlobData;
import grcpserver.services.firestore.FirestoreOperations;
import grcpserver.services.firestore.ProcessedImageData;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class VisionFlowFunctionalService extends VisionFlowFunctionalServiceGrpc.VisionFlowFunctionalServiceImplBase {
    private final String bucketName = "lab3-bucket-g04-europe";
    private final CloudStorageOperations cloudStorageOperations;
    private final CloudPubSubOperations cloudPubSubOperations;
    private final FirestoreOperations firestoreOperations;
    private final Logger logger = Logger.getLogger(VisionFlowFunctionalService.class.getName());

    public VisionFlowFunctionalService(String projectId) {
        StorageOptions storageOperations = StorageOptions.getDefaultInstance();
        Storage storage = storageOperations.getService();
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
                    // TODO: send to blob storage
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
                UUID requestId = UUID.randomUUID();
                String blobName = imageName + "#" + requestId;
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
                    onError(e);
                }
            }
        };
    }

    @Override
    public void downloadImage(DownloadImageRequest request, StreamObserver<DownloadImageResponse> responseObserver) {
        // parse request id by # to get the blob name
        BlobId blobId = BlobId.of(bucketName, request.getId());
        try {
            DownloadedBlobData downloadBlobData = cloudStorageOperations.downloadBlobFromBucket(blobId);
            DownloadImageResponse response = DownloadImageResponse.newBuilder()
                    .setData(ByteString.copyFrom(downloadBlobData.getData()))
                    .setContentType(downloadBlobData.getContentType())
                    .setName(request.getId().split("#")[0])
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.severe("Error downloading image: " + e.getMessage());
        }
    }

    @Override
    public void getImageCharacteristics(
            GetImageCharacteristicsRequest request,
            StreamObserver<GetImageCharacteristicsResponse> responseObserver
    ) {
        try {
            String id = request.getId();
            ProcessedImageData processedImageData = firestoreOperations.getImageCharacteristics(id);
            GetImageCharacteristicsResponse response = GetImageCharacteristicsResponse.newBuilder()
                    .setDate(processedImageData.getTimestamp().toString())
                    .addAllLabels(processedImageData.getLabels())
                    .addAllTranslations(processedImageData.getTranslatedLabels())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.severe("Error getting image characteristics: " + e.getMessage());
        }
    }

    @Override
    public void getFileNamesByCharacteristic(
            GetFileNamesRequest request,
            StreamObserver<GetFileNamesResponse> responseObserver
    ) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
            Date startDate = formatter.parse(request.getStartDate());
            Date endDate = formatter.parse(request.getEndDate());
            String characteristic = request.getCharacteristic();
            List<ProcessedImageData> processedImageDataList = firestoreOperations.getImagesByDateAndCharacteristic(startDate, endDate, characteristic);
            GetFileNamesResponse.Builder responseBuilder = GetFileNamesResponse.newBuilder();
            for (ProcessedImageData processedImageData : processedImageDataList) {
                responseBuilder.addIds(processedImageData.getId());
            }
            GetFileNamesResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.severe("Error getting file names by characteristic: " + e.getMessage());
        }
    }


}
