package grpcclientapp;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.ExistingTopics;
import servicestubs.ForumGrpc;
import servicestubs.ForumMessage;
import servicestubs.SubscribeUnSubscribe;

import java.util.Iterator;
import java.util.Scanner;

public class Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "34.29.185.196";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static ForumGrpc.ForumBlockingStub blockingStub;
    private static ForumGrpc.ForumStub noBlockStub;

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }
            System.out.println("connect to " + svcIP + ":" + svcPort);
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    // Channels are secure by default (via SSL/TLS).
                    // For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build();
            blockingStub = ForumGrpc.newBlockingStub(channel);
            noBlockStub = ForumGrpc.newStub(channel);
            // Call service operations:
            while (true) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            topicSubscribe();
                            break;
                        case 2:
                            topicUnSubscribe();
                            break;
                        case 3:
                            getAllTopics();
                            break;
                        case 4:
                            publishMessage();
                            break;
                        case 5:
                            topicSubscribeAsync();
                            break;
                        case 6:
                            topicUnSubscribeAsync();
                            break;
                        case 7:
                            getAllTopicsAsync();
                            break;
                        case 8:
                            publishMessageAsync();
                            break;
                        case 99:
                            System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error  !");
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    private static void topicSubscribe() {
        SubscribeUnSubscribe request = createSubscribeUnSubscribeRequest();

        Iterator<ForumMessage> response = blockingStub.topicSubscribe(request);
        while (response.hasNext()) {
            ForumMessage forumMessage = response.next();
            String topic = forumMessage.getTopicName();
            String message = forumMessage.getTxtMsg();
            String user = forumMessage.getFromUser();
            System.out.println("Received message from <" + user + "> on topic <" + topic + "> : " + message);
        }
    }

    private static void topicUnSubscribe() {
        SubscribeUnSubscribe request = createSubscribeUnSubscribeRequest();
        String topic = request.getTopicName();

        Empty iterator = blockingStub.topicUnSubscribe(request);
        System.out.println("Unsubscribed from topic: " + topic);
    }

    private static void getAllTopics() {
        Empty request = Empty.newBuilder().build();
        ExistingTopics response = blockingStub.getAllTopics(request);

        System.out.println("Existing topics:");
        for (String topic : response.getTopicNameList()) {
            System.out.println(topic);
        }
    }

    private static void publishMessage() {
        ForumMessage request = createMessageRequest();
        String topic = request.getTopicName();

        Empty response = blockingStub.publishMessage(request);
        System.out.println("Message published to topic: " + topic);
    }

    private static void topicSubscribeAsync() {
        SubscribeUnSubscribe request = createSubscribeUnSubscribeRequest();
        String topic = request.getTopicName();

        TopicSubscriptionStream responseObserver = new TopicSubscriptionStream();
        noBlockStub.topicSubscribe(request, responseObserver);

        // create new thread to wait for messages
        Thread thread = new Thread(() -> {
            while (!responseObserver.isCompleted()) {
                System.out.println("[Async] Waiting for more messages on topic: " + topic);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    private static void topicUnSubscribeAsync() {
        SubscribeUnSubscribe request = createSubscribeUnSubscribeRequest();
        String topic = request.getTopicName();

        noBlockStub.topicUnSubscribe(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty empty) {
                System.out.println("Unsubscribed from topic: " + topic);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error unsubscribing from topic: " + topic);
            }

            @Override
            public void onCompleted() {
                System.out.println("Unsubcription request completed");
            }
        });
    }

    private static void getAllTopicsAsync() {
        Empty request = Empty.newBuilder().build();
        noBlockStub.getAllTopics(request, new StreamObserver<ExistingTopics>() {
            @Override
            public void onNext(ExistingTopics existingTopics) {
                System.out.println("Existing topics:");
                for (String topic : existingTopics.getTopicNameList()) {
                    System.out.println(topic);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error getting topics");
            }

            @Override
            public void onCompleted() {
                System.out.println("Get all topics request completed");
            }
        });
    }

    private static void publishMessageAsync() {
        ForumMessage request = createMessageRequest();
        String topic = request.getTopicName();

        noBlockStub.publishMessage(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty empty) {
                System.out.println("Message published to topic: " + topic);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error publishing message to topic: " + topic);
            }

            @Override
            public void onCompleted() {
                System.out.println("Message publishing request completed");
            }
        });
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" Synchrounous GRPC Client");
            System.out.println(" 1 - Subscribe to a topic");
            System.out.println(" 2 - Unsubscribe to a topic");
            System.out.println(" 3 - Get all topics");
            System.out.println(" 4 - Publish a message");
            System.out.println(" Asynchronous GRPC Client");
            System.out.println(" 5 - Subscribe to a topic");
            System.out.println(" 6 - Unsubscribe to a topic");
            System.out.println(" 7 - Get all topics");
            System.out.println(" 8 - Publish a message");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?: ");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 8) || op == 99));
        return op;
    }

    private static String read(String message) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(message);
        return scanner.nextLine();
    }

    private static ForumMessage createMessageRequest() {
        String topic = read("Enter topic name:");
        String username = read("Enter username:");
        String message = read("Enter message to send:");

        return ForumMessage.newBuilder()
                .setTopicName(topic)
                .setFromUser(username)
                .setTxtMsg(message)
                .build();
    }

    private static SubscribeUnSubscribe createSubscribeUnSubscribeRequest() {
        String topic = read("Enter topic name:");
        String username = read("Enter username:");

        return SubscribeUnSubscribe.newBuilder()
                .setTopicName(topic)
                .setUsrName(username)
                .build();
    }

}
