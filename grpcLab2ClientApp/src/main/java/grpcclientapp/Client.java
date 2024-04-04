package grpcclientapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import servicestubs.*;

import java.util.*;

public class Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "34.67.66.245";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static ServiceGrpc.ServiceStub noBlockStub;

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
            noBlockStub = ServiceGrpc.newStub(channel);
            // Call service operations for example ping server
            boolean end = false;
            while (!end) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            findPrimes();
                            break;
                        case 99:
                            System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error  !");
                    ex.printStackTrace();
                }
            }
            read("prima enter to end", new Scanner(System.in));
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    static void findPrimes() throws InterruptedException {
        // Asynchronous non blocking call
        int N = Integer.parseInt(read("How many prime numbers?", new Scanner(System.in)));
        PrimeNumbersStream primeStream = new PrimeNumbersStream();
        int prevN = 1;
        for (int i = 1; i <= 5; i++) {
            noBlockStub.findPrimes(IntervalNumbers.newBuilder().setStart(prevN).setEnd(i * 100).build(), primeStream);
            prevN = i * 100;
        }
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Case server stream: find N prime numbers: Asynchronous call");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 5) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}
