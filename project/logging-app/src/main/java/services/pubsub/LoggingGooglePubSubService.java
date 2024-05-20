package services.pubsub;

import receiver.LoggingMessageHandlerFunction;
import receiver.LoggingMessageReceiveHandler;

public class LoggingGooglePubSubService {

    private final GooglePubSub googlePubSub;

    public LoggingGooglePubSubService(GooglePubSub googlePubSub) {
        this.googlePubSub = googlePubSub;
    }

    public void subscribe(String projectId, String subscriptionId, LoggingMessageHandlerFunction handlerFunction) {
        googlePubSub.subscribe(projectId, subscriptionId, new LoggingMessageReceiveHandler(handlerFunction));
    }
}
