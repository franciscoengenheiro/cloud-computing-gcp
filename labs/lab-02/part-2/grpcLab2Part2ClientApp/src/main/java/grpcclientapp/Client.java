package grpcclientapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import servicestubs.ForumGrpc;

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
        TODO();
    }

    static void topicUnSubscribe() {
        TODO();
    }

    static void getAllTopics() {
        TODO();
    }

    static void publishMessage() {
        TODO();
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

    private static String read(Scanner input) {
        System.out.println("Press Enter to end");
        return input.nextLine();
    }

}
