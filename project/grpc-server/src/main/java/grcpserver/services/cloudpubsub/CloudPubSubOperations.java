package grcpserver.services.cloudpubsub;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.threeten.bp.LocalDateTime;


public class CloudPubSubOperations {
    private final String projectId;
    private final String TOPIC_ID = "vision-flow-topic";


    public CloudPubSubOperations(String projectId) {
        this.projectId = projectId;
    }

    public void publishMessage(String requestId, String imageName, String bucket_name, String blobName, String translation) {
        TopicName topicName = TopicName.ofProjectTopicName(projectId, TOPIC_ID);
        System.out.println("Topic Name=" + topicName);
        try {
            Publisher publisher = Publisher.newBuilder(topicName).build();
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .putAttributes("request_id", requestId)
                    .putAttributes("image_name", imageName)
                    .putAttributes("bucket_name", bucket_name)
                    .putAttributes("blob_name", blobName)
                    .putAttributes("translation", translation)
                    .putAttributes("timestamp", LocalDateTime.now().toString())
                    .build();

            ApiFuture<String> future = publisher.publish(pubsubMessage);

            String msgID = future.get();
            System.out.println("Message Published with ID=" + msgID);

            publisher.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}