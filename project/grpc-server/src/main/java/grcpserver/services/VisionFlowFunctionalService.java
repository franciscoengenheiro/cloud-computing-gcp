package grcpserver.services;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
    private final Storage storage;

    public VisionFlowFunctionalService(String projectId) {
        StorageOptions storageOperations = StorageOptions.getDefaultInstance();
        Storage storage = storageOperations.getService();
        this.cloudStorageOperations = new CloudStorageOperations(storage);
        this.cloudPubSubOperations = new CloudPubSubOperations(projectId);
        this.firestoreOperations = new FirestoreOperations(storage.getOptions().getCredentials());
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public StreamObserver<UploadImageRequest> uploadImage(StreamObserver<UploadImageResponse> responseObserver) {
        return new StreamObserver<>() {
            WriteChannel writer;
            String contentType;
            String imageName;
            String translationLang;
            String blobName;

            @Override
            public void onNext(UploadImageRequest request) {
                // on the first request, get all the metadata
                if (contentType == null) {
                    contentType = request.getContentType();
                    imageName = request.getName();
                    translationLang = request.getTranslationLang();
                    UUID requestId = UUID.randomUUID();
                    blobName = imageName + "#" + requestId;
                    BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, blobName).setContentType(contentType).build();
                    logger.info("Blob info: " + blobInfo);
                    writer = storage.writer(blobInfo);
                }
                byte[] chunk = new byte[1024];

                try (InputStream imageStream = new ByteArrayInputStream(request.getChunk().toByteArray())) {
                    logger.info("Uploading image chunk");
                    int bytesRead;
                    while ((bytesRead = imageStream.read(chunk)) >= 0) {
                        try {
                            writer.write(ByteBuffer.wrap(chunk, 0, bytesRead));
                        } catch (IOException e) {
                            logger.severe("Error uploading image chunk: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
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
                // All image bytes have been received, get the accumulated image bytes
                try {
                    UploadImageResponse response = UploadImageResponse.newBuilder()
                            .setId(blobName)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                    if (writer != null)
                        writer.close();
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
