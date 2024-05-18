package app.services.pubsub;

import app.receiver.LabelsMessageHandlerFunction;
import app.receiver.LabelsMessageReceiveHandler;

public class LabelsGooglePubSubService {

    private final GooglePubSub googlePubSub;

    public LabelsGooglePubSubService(GooglePubSub googlePubSub) {
        this.googlePubSub = googlePubSub;
    }

    public void subscribe(String projectId, String subscriptionId, LabelsMessageHandlerFunction handlerFunction) {
        googlePubSub.subscribe(projectId, subscriptionId, new LabelsMessageReceiveHandler(handlerFunction));
    }
}
