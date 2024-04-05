package grpcserverapp;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import servicestubs.ExistingTopics;
import servicestubs.ForumGrpc;
import servicestubs.ForumMessage;
import servicestubs.SubscribeUnSubscribe;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Service extends ForumGrpc.ForumImplBase {

    private final ConcurrentMap<String, ConcurrentMap<String, StreamObserver<ForumMessage>>> topicUserMap = new ConcurrentHashMap<>();

    public Service(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public void topicSubscribe(SubscribeUnSubscribe request, StreamObserver<ForumMessage> responseObserver) {
        String topic = request.getTopicName();
        String username = request.getUsrName();

        topicUserMap.computeIfAbsent(topic, k -> new ConcurrentHashMap<>()).put(username, responseObserver);
    }

    @Override
    public void topicUnSubscribe(SubscribeUnSubscribe request, StreamObserver<Empty> responseObserver) {
        String topic = request.getTopicName();
        String username = request.getUsrName();

        ConcurrentMap<String, StreamObserver<ForumMessage>> userMap = topicUserMap.get(topic);
        if (userMap != null) {
            userMap.remove(username);
        }

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTopics(Empty request, StreamObserver<ExistingTopics> responseObserver) {
        ExistingTopics.Builder existingTopicsBuilder = ExistingTopics.newBuilder();

        existingTopicsBuilder.addAllTopicName(topicUserMap.keySet());

        responseObserver.onNext(existingTopicsBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void publishMessage(ForumMessage request, StreamObserver<Empty> responseObserver) {
        String topic = request.getTopicName();

        ConcurrentMap<String, StreamObserver<ForumMessage>> userMap = topicUserMap.get(topic);
        if (userMap != null) {
            for (StreamObserver<ForumMessage> observer : userMap.values()) {
                observer.onNext(request);
            }
        }

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
