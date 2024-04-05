package grpcclientapp;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import servicestubs.ExistingTopics;
import servicestubs.ForumGrpc;
import servicestubs.ForumMessage;
import servicestubs.SubscribeUnSubscribe;

import java.util.Scanner;

public class Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static ForumGrpc.ForumBlockingStub blockingStub;
    private static ForumGrpc.ForumStub noBlockStub;

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0]; svcPort = Integer.parseInt(args[1]);
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
            boolean end = false;
            while (!end) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            topicSubscribe(); break;
                        case 2:
                            topicUnSubscribe(); break;
                        case 3:
                            getAllTopics(); break;
                        case 4:
                            publishMessage(); break;
                        case 99:  System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error  !");
                    ex.printStackTrace();
                }
            }
            read(new Scanner(System.in));
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    static void topicSubscribe() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter topic name:");
        String topic = scanner.nextLine();
        System.out.println("Enter username:");
        String username = scanner.nextLine();

        SubscribeUnSubscribe request = SubscribeUnSubscribe.newBuilder()
                .setTopicName(topic)
                .setUsrName(username)
                .build();

        ForumMessage response = blockingStub.topicSubscribe(request).next();
        System.out.println("Subscribed to topic: " + response.getTopicName());
    }

    static void topicUnSubscribe() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter topic name:");
        String topic = scanner.nextLine();
        System.out.println("Enter username:");
        String username = scanner.nextLine();

        SubscribeUnSubscribe request = SubscribeUnSubscribe.newBuilder()
                .setTopicName(topic)
                .setUsrName(username)
                .build();

        Empty response = blockingStub.topicUnSubscribe(request);
        System.out.println("Unsubscribed from topic: " + topic);
    }

    static void getAllTopics() {
        Empty request = Empty.newBuilder().build();
        ExistingTopics response = blockingStub.getAllTopics(request);

        System.out.println("Existing topics:");
        for (String topic : response.getTopicNameList()) {
            System.out.println(topic);
        }
    }

    static void publishMessage() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter topic name:");
        String topic = scanner.nextLine();
        System.out.println("Enter message:");
        String message = scanner.nextLine();

        ForumMessage request = ForumMessage.newBuilder()
                .setTopicName(topic)
                .setTxtMsg(message)
                .build();

        Empty response = blockingStub.publishMessage(request);
        System.out.println("Message published to topic: " + topic);
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Subscribe to a topic");
            System.out.println(" 2 - Unsubscribe to a topic");
            System.out.println(" 3 - Get all topics");
            System.out.println(" 4 - Publish a message");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?: ");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 5) || op == 99));
        return op;
    }

    private static void read(Scanner input) {
        System.out.println("Press Enter to end");
        input.nextLine();
    }
}
