import grcpserver.GrpcServer;

import java.util.Scanner;

public class App {
    static GrpcServer gRPCServer;

    public static void main(String[] args) {
        try {
            gRPCServer = new GrpcServer();
            int option;
            do {
                System.out.println("\n######## MENU ##########");
                System.out.println("1: Upload Image");
                System.out.println("0: Exit");
                System.out.println("########################");
                System.out.print("Enter an Option: ");
                Scanner scanner = new Scanner(System.in);
                option = scanner.nextInt();
                switch (option) {
                    case 1:
                        String res = gRPCServer.uploadImage();
                        System.out.println("Image uploaded to: " + res);
                        break;
                    case 0:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            } while (option != 0);
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }
}
