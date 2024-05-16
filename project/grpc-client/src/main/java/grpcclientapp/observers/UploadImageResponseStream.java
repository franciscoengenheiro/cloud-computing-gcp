package grpcclientapp.observers;

import io.grpc.stub.StreamObserver;
import servicestubs.UploadImageResponse;

public class UploadImageResponseStream implements StreamObserver<UploadImageResponse> {

    @Override
    public void onNext(UploadImageResponse uploadImageResponse) {
        System.out.print("\nImage uploaded with id: " + uploadImageResponse.getId());
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.print("\nonError: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        System.out.print("\nImage uploaded successfully");
    }
}
