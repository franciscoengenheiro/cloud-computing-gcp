package grpcclientapp.observers;

import io.grpc.stub.StreamObserver;
import servicestubs.UploadImageResponse;

import java.util.logging.Logger;

public class UploadImageResponseStream implements StreamObserver<UploadImageResponse> {

    private final Logger logger = Logger.getLogger(UploadImageResponseStream.class.getName());

    @Override
    public void onNext(UploadImageResponse uploadImageResponse) {
        System.out.println("\nImage uploadeded with id: " + uploadImageResponse.getId());
    }

    @Override
    public void onError(Throwable throwable) {
        logger.warning("Error uploading image: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        // logger.info("Image upload completed");
    }
}
