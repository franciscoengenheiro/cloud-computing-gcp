import services.firestore.LoggingFirestoreOperations;
import services.firestore.UnProcessedImageData;
import services.pubsub.GooglePubSub;
import services.pubsub.LoggingGooglePubSubService;

import java.io.IOException;
import java.util.logging.Logger;

public class LoggingApp {
    private final static String PROJECT_ID = "cn2324-t1-g04";
    private final static String SUBSCRIPTION_ID = "loggingApp";
    private static final Logger logger = Logger.getLogger(LoggingApp.class.getName());
    private static final LoggingFirestoreOperations firestoreOperations;

    static {
        try {
            firestoreOperations = new LoggingFirestoreOperations();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        logger.info("LoggingApp started");
        LoggingGooglePubSubService labelsPubSubService = new LoggingGooglePubSubService(new GooglePubSub());
        labelsPubSubService.subscribe(PROJECT_ID, SUBSCRIPTION_ID,
                (String requestId, String imageName, String bucketName, String blobName, String translationLang) -> {
                    try {
                        logger.info("Request ID: " + requestId);
                        UnProcessedImageData unProcessImageData = new UnProcessedImageData(
                                requestId,
                                imageName,
                                bucketName,
                                blobName,
                                translationLang
                        );
                        firestoreOperations.saveImage(unProcessImageData);
                    } catch (Exception ex) {
                        logger.severe("Error processing image: " + ex.getMessage());
                    }
                }
        );
    }

}

