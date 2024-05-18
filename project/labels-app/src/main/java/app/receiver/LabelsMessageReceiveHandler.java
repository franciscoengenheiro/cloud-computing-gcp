package app.receiver;

import app.LabelsMessageHandlerFunction;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;

import java.util.Map;

public class LabelsMessageReceiveHandler implements MessageReceiver {

    private final LabelsMessageHandlerFunction labelsMessageHandler;

    public LabelsMessageReceiveHandler(LabelsMessageHandlerFunction labelsMessageHandlerFunction) {
        this.labelsMessageHandler = labelsMessageHandlerFunction;
    }

    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {

        final Map<String, String> attributes = pubsubMessage.getAttributesMap();

        final String requestId = attributes.get("request_id");
        final String imageName = attributes.get("image_name");
        final String timestamp = attributes.get("timestamp");
        final String bucketName = attributes.get("bucket_name");
        final String blobName = attributes.get("blob_name");
        final String translation = attributes.get("translation");

        labelsMessageHandler.handleRequest(requestId, imageName, timestamp, bucketName, blobName, translation);

        ackReplyConsumer.ack();
    }
}

