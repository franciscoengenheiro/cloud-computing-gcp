package app.services.pubsub;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;

public class GooglePubSub {

    public void subscribe(String projectId, String subscriptionId, MessageReceiver handler) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder()
                .setExecutorThreadCount(1)  // only one thread on the handler
                .build();

        Subscriber subscriber = Subscriber.newBuilder(subscriptionName, handler)
                .setExecutorProvider(executorProvider)
                .build();

        subscriber.startAsync().awaitTerminated();
    }
}
