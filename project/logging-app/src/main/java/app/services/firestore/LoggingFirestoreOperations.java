package app.services.firestore;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;
import java.util.logging.Logger;

public class LoggingFirestoreOperations {
    final Logger logger = Logger.getLogger(LoggingFirestoreOperations.class.getName());
    final String databaseId = "vision-flow-db";
    final GoogleCredentials credentials =
            GoogleCredentials.getApplicationDefault();

    final FirestoreOptions options = FirestoreOptions.newBuilder()
            .setDatabaseId(databaseId)
            .setCredentials(credentials)
            .build();

    final Firestore db = options.getService();
    final String collectionId = "logs";

    public LoggingFirestoreOperations() throws IOException {
    }

    public void saveImage(UnProcessedImageData unProcessedImageData) {
        // save image in Firestore
        logger.info("Saving image in Firestore");
        // not waiting for the result
        db.collection(collectionId)
                .document(unProcessedImageData.getId())
                .set(unProcessedImageData);
    }
}
