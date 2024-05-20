package grcpserver.services.firestore;

import com.google.api.core.ApiFuture;
import com.google.auth.Credentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class FirestoreOperations {
    private final Logger logger = Logger.getLogger(FirestoreOperations.class.getName());
    private final String databaseId = "vision-flow-db";
    private final String collectionId = "labels-app";
    private final Firestore db;

    public FirestoreOperations(Credentials credentials) {
        final FirestoreOptions options = FirestoreOptions.newBuilder()
                .setDatabaseId(databaseId)
                .setCredentials(credentials)
                .build();
        db = options.getService();
    }

    public ProcessedImageData getImageCharacteristics(String id) throws ExecutionException, InterruptedException {
        // download image from Firestore
        logger.info("Downloading image from Firestore");
        // not waiting for the result
        ApiFuture<DocumentSnapshot> future = db.collection(collectionId)
                .document(id)
                .get();

        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(ProcessedImageData.class);
        } else {
            throw new RuntimeException("Image not found in Firestore");
        }
    }
}
