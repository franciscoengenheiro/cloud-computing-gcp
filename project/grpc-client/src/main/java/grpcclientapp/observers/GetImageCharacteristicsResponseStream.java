package grpcclientapp.observers;

import io.grpc.stub.StreamObserver;
import servicestubs.GetImageCharacteristicsResponse;

import java.util.logging.Logger;

public class GetImageCharacteristicsResponseStream implements StreamObserver<GetImageCharacteristicsResponse> {

    private final Logger logger = Logger.getLogger(GetImageCharacteristicsResponseStream.class.getName());

    public GetImageCharacteristicsResponseStream() {
    }

    @Override
    public void onNext(GetImageCharacteristicsResponse getImageCharacteristicsResponse) {
        System.out.println("\nLabels: " + getImageCharacteristicsResponse.getLabelsList());
        System.out.println("Translated Labels: " + getImageCharacteristicsResponse.getTranslationsList());
        System.out.println("Date: " + getImageCharacteristicsResponse.getDate());
    }

    @Override
    public void onError(Throwable throwable) {
        logger.severe("Error getting image characteristics: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        // logger.info("Image characteristics received successfully");
    }
}
