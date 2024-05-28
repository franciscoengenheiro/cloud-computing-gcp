package app.receiver;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;

import java.util.Map;
import java.util.logging.Logger;

public class LoggingMessageReceiveHandler implements MessageReceiver {

    private final Logger logger = Logger.getLogger(LoggingMessageReceiveHandler.class.getName());
    private final LoggingMessageHandlerFunction labelsMessageHandler;

    public LoggingMessageReceiveHandler(LoggingMessageHandlerFunction loggingMessageHandlerFunction) {
        this.labelsMessageHandler = loggingMessageHandlerFunction;
    }

    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {

        final Map<String, String> attributes = pubsubMessage.getAttributesMap();

        logger.info("Message received with id " + pubsubMessage.getMessageId());
        final String requestId = attributes.get("request_id");
        final String imageName = attributes.get("image_name");
        final String bucketName = attributes.get("bucket_name");
        final String blobName = attributes.get("blob_name");
        final String translation = attributes.get("translation");

        labelsMessageHandler.handleRequest(requestId, imageName, bucketName, blobName, translation);

        ackReplyConsumer.ack();
        logger.info("Message with id " + pubsubMessage.getMessageId() + " acknowledged");
    }
}

