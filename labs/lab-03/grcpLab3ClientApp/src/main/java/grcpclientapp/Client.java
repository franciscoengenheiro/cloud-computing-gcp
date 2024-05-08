package grcpclientapp;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.Empty;
import forum.ForumGrpc;
import forum.ForumMessage;
import forum.SubscribeUnSubscribe;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

public class Client {
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static ForumGrpc.ForumBlockingStub blockingStub;
    private static ForumGrpc.ForumStub noBlockStub;

    public static void main(String[] args) {
        // Connect to the server
        channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                .usePlaintext()
                .build();
        blockingStub = ForumGrpc.newBlockingStub(channel);
        noBlockStub = ForumGrpc.newStub(channel);

        // Subscribe to a topic
        SubscribeUnSubscribe request = SubscribeUnSubscribe.newBuilder()
                .setUsrName("username")
                .setTopicName("topic")
                .build();

        StreamObserver<ForumMessage> responseObserver = new StreamObserver<ForumMessage>() {
            @Override
            public void onNext(ForumMessage forumMessage) {
                System.out.println("Received message: " + forumMessage.getTxtMsg());
                String[] parts = forumMessage.getTxtMsg().split(";");
                if (parts.length == 3) {
                    String bucketName = parts[1];
                    String blobName = parts[2];
                    downloadBlob(bucketName, blobName);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        };
        noBlockStub.topicSubscribe(request, responseObserver);

        // Publish a message
        publishMessage();

        // Keep the main thread alive
        while (true) {
        }
    }

    private static void downloadBlob(String bucketName, String blobName) {
        String absFileName = Paths.get("").toAbsolutePath().toString();
        BlobId blobId = BlobId.of(bucketName, blobName);
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Blob blob = storage.get(blobId);
        String downloadTo = absFileName + "/" + blobName + ".png";

        try (OutputStream writeTo = new FileOutputStream(downloadTo)) {
            if (blob.getSize() < 1_000_000) {
                // Blob is small to read all blob content in one request
                blob.downloadTo(writeTo);
            } else {
                // If Blob size is big, use a blob's channel reader
                try (ReadChannel reader = blob.reader()) {
                    ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
                    while (reader.read(bytes) > 0) {
                        bytes.flip();
                        while (bytes.hasRemaining()) {
                            writeTo.write(bytes.get());
                        }
                        bytes.clear();
                    }
                }
            }
            System.out.println("Downloaded blob to: " + downloadTo);
        } catch (Exception e) {
            System.out.println("Error downloading blob: " + e.getMessage());
        }
    }

    private static void publishMessage() {
        // Create a message
        ForumMessage message = ForumMessage.newBuilder()
                .setFromUser("username")
                .setTopicName("topic")
                .setTxtMsg("message;lab3-bucket-g04-europe;lab3-blob-g04-europe")
                .build();

        // Publish the message
        noBlockStub.publishMessage(message, new StreamObserver<>() {
            @Override
            public void onNext(Empty empty) {
                System.out.println("Message published");
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Publishing completed");
            }
        });
    }
}
