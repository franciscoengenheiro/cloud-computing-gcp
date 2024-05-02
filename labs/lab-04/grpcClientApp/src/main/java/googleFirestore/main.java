package googleFirestore;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static googleFirestore.model.OcupacaoBuilder.insertDocuments;

public class main {
    public static void main(String[] args) {
        try {
            //TODO: ADD MENU

            Firestore db = FirestoreOptions.getDefaultInstance().getService();
            String documentId = "path/to/document";
            String fieldName = "fieldToBeDeleted";
            deleteField(db, documentId, fieldName);
            new main().fireInsert();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fireInsert() throws Exception {
        GoogleCredentials credentials =
                GoogleCredentials.getApplicationDefault();

        FirestoreOptions options = FirestoreOptions
                .newBuilder().setDatabaseId("lab04-db").setCredentials(credentials)
                .build();
        Firestore db = options.getService();

        insertDocuments("C:\\Users\\Francisco Saraiva\\Desktop\\6 Semestre\\CN\\Laboratorios\\Lab4\\CN-2324-G04\\labs\\lab-04\\grpcClientApp\\src\\main\\java\\res\\OcupacaoEspacosPublicos.csv", db, "lab04");
    }


    private static void  deleteField(Firestore db, String documentId, String fieldName) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.document(documentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, FieldValue.delete());
        ApiFuture<WriteResult> writeResult = docRef.update(updates);
        System.out.println("Update time : " + writeResult.get());

    }


    private static void queryDocuments(Firestore db) throws ExecutionException, InterruptedException {
        CollectionReference eventsRef = db.collection("events");
        Query query = eventsRef.whereGreaterThanOrEqualTo("dtInicio", "31/01/2017")
                .whereLessThanOrEqualTo("dtFinal", "01/03/2017");
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            System.out.println("Document ID: " + document.getId());
            System.out.println("dtInicio: " + document.getString("dtInicio"));
            System.out.println("dtFinal: " + document.getString("dtFinal"));
        }
    }
}
