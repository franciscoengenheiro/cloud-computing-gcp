package app.services.firestore;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;
import java.util.logging.Logger;

public class LabelsAppFirestoreOperations {
    final Logger logger = Logger.getLogger(LabelsAppFirestoreOperations.class.getName());
    final String databaseId = "vision-flow-db";
    final GoogleCredentials credentials =
            GoogleCredentials.getApplicationDefault();

    final FirestoreOptions options = FirestoreOptions.newBuilder()
            .setDatabaseId(databaseId)
            .setCredentials(credentials)
            .build();

    final Firestore db = options.getService();
    final String collectionId = "labels-app";

    public LabelsAppFirestoreOperations() throws IOException {
    }

    public void saveImage(ProcessedImageData processedImageData) {
        // save image in Firestore
        logger.info("Saving image in Firestore");
        // not waiting for the result
        db.collection(collectionId)
                .document(processedImageData.getId())
                .set(processedImageData);
    }
}
