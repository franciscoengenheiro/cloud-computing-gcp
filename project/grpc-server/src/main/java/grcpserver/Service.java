package grcpserver;

import com.google.cloud.storage.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import servicestubs.*;
import storageOperations.StorageOperations;

import java.util.Arrays;

public class Service extends VisionFlowServiceGrpc.VisionFlowServiceImplBase {
    private final Storage storage = StorageOptions.getDefaultInstance().getService();
    private final String bucketName = "lab3-bucket-g04-europe";
    private final StorageOperations storageOperations = new StorageOperations(storage);

    public Service(int svcPort) {
        System.out.println("Service started on port " + svcPort);
    }

    @Override
    public void uploadAnImage(ImageId request, StreamObserver<ImageBlobId> responseObserver) {
        String path = request.getPath();
        String bloblName = path.substring(path.lastIndexOf("/") + 1);
        try {
            BlobId id = storageOperations.uploadBlobToBucket(bucketName, bloblName, path);
            ImageBlobId response = ImageBlobId.newBuilder().setGsUri(id.toGsUtilUri()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public StreamObserver<ImageIds> uploadImages(final StreamObserver<ImageBlobIds> responseObserver) {
        return new StreamObserver<ImageIds>() {
            @Override
            public void onNext(ImageIds imageIds) {
                // TODO: Implement the logic to upload multiple images to Google Cloud Storage
                // For now, just print the received image paths
                System.out.println("Received image paths: " + imageIds.getPathList());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                // For now, just return a dummy response
                ImageBlobIds response = ImageBlobIds.newBuilder().addGsUri("gs://bucket/blob").build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void downloadAnImage(ImageBlobId request, StreamObserver<Empty> responseObserver) {
        BlobId blobId = BlobId.fromGsUtilUri(request.getGsUri());
        try {
            storageOperations.downloadBlobFromBucket(blobId);
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}