package grpcserverapp;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import servicestubs.ExistingTopics;
import servicestubs.ForumGrpc;
import servicestubs.ForumMessage;
import servicestubs.SubscribeUnSubscribe;

import java.util.Random;

public class Service extends ForumGrpc.ForumImplBase {

    public Service(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public void topicSubscribe(SubscribeUnSubscribe request, StreamObserver<ForumMessage> responseObserver) {
        TODO();
    }

    @Override
    public void topicUnSubscribe(SubscribeUnSubscribe request, StreamObserver<Empty> responseObserver) {
        TODO();
    }

    @Override
    public void getAllTopics(Empty request, StreamObserver<ExistingTopics> responseObserver) {
        TODO();
    }

    @Override
    public void publishMessage(ForumMessage request, StreamObserver<Empty> responseObserver) {
        TODO();
    }

    private void simulateExecutionTime() {
        try {
            // simulate processing time between 200ms and 3s
            Thread.sleep(new Random().nextInt(2800) + 200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
