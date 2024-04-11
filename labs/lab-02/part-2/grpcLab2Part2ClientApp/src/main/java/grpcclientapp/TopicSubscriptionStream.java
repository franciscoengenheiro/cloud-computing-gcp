package grpcclientapp;

import io.grpc.stub.StreamObserver;
import servicestubs.ForumMessage;

public class TopicSubscriptionStream implements StreamObserver<ForumMessage> {
    boolean completed = false;

    @Override
    public void onNext(ForumMessage forumMessage) {
        String topic = forumMessage.getTopicName();
        String message = forumMessage.getTxtMsg();
        String user = forumMessage.getFromUser();
        System.out.println("Received message from <" + user + "> on topic <" + topic + "> : " + message);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Completed with error:" + throwable.getMessage());
        completed = true;
    }

    @Override
    public void onCompleted() {
        System.out.println("Completed");
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }
}
