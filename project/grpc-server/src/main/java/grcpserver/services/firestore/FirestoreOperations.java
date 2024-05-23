package grcpserver.services.firestore;

import com.google.api.core.ApiFuture;
import com.google.auth.Credentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    public List<ProcessedImageData> getImagesByDateAndCharacteristic(Date startDate, Date endDate, String characteristic) throws ExecutionException, InterruptedException {
        Timestamp startTimestamp = Timestamp.of(startDate);
        Timestamp endTimestamp = Timestamp.of(endDate);

        Query query = db.collection(collectionId)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        logger.info("GetImages by date and characteristic");
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        List<ProcessedImageData> results = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {

            ProcessedImageData data = document.toObject(ProcessedImageData.class);
            if (data.getTranslatedLabels() != null && data.getTranslatedLabels().contains(characteristic)) {
                results.add(data);
            }
        }


        return results;
    }
}
