package grpcclientapp.observers;


import io.grpc.stub.StreamObserver;
import servicestubs.GetFileNamesResponse;

import java.util.logging.Logger;

public class GetFileNamesResponseStream implements StreamObserver<GetFileNamesResponse> {
    private final Logger logger = Logger.getLogger(GetFileNamesResponseStream.class.getName());

    public GetFileNamesResponseStream() {

    }

    @Override
    public void onNext(GetFileNamesResponse getFileNamesResponse) {
        System.out.println();
        for (String name : getFileNamesResponse.getIdsList()) {
            System.out.println("File Name: " + name);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.severe("Error getting file names by characteristic: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {

    }
}
