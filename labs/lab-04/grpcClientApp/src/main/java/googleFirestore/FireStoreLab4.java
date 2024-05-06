package googleFirestore;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static googleFirestore.model.OcupacaoBuilder.insertDocuments;

public class FireStoreLab4 {
    public FireStoreLab4() throws IOException {
    }

    static GoogleCredentials credentials;
    static FirestoreOptions options;
    static Firestore db;
    static String collectionName = "lab04";

    public static void main(String[] args) {
        try {
            credentials = GoogleCredentials.getApplicationDefault();
            options = FirestoreOptions
                    .newBuilder().setDatabaseId("lab04-db").setCredentials(credentials)
                    .build();
            db = options.getService();
            Scanner scanner = new Scanner(System.in);
            int option;
            do {
                System.out.println("######## MENU ##########");
                System.out.println("1: Insert documents");
                System.out.println("2: Get document by ID");
                System.out.println("3: Get documents by event date");
                System.out.println("0: Exit");
                System.out.println("########################");
                System.out.print("Enter an Option: ");
                option = scanner.nextInt();
                switch (option) {
                    case 1:
                        fireInsert();
                        break;
                    case 2:
                        System.out.print("Enter the document ID: ");
                        String docId = scanner.next();
                        getDocumentById(docId);
                        break;
                    case 3:
                        getDocumentsByEventDate();
                        break;
                    case 0:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            } while (option != 0);
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private static void fireInsert() throws Exception {
        insertDocuments("labs/lab-04/grpcClientApp/src/main/java/res/OcupacaoEspacosPublicos.csv", db, collectionName);
    }

    private static void deleteField(Firestore db, String documentId, String fieldName) throws ExecutionException, InterruptedException {
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

    // Get document by ID -  alinea a)
    private static void getDocumentById(String id) throws IOException, ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document;
        try {
            document = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (document.exists()) {
            System.out.println("Document data: " + document.getData());
        } else {
            System.out.println("No such document!");
        }
    }


    // Get documents by event date - alinea e)
    private static void getDocumentsByEventDate() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = formatter.parse("31/01/2017");
        Date endDate = formatter.parse("01/03/2017");

        Query query = db.collection(collectionName)
                .whereGreaterThanOrEqualTo("event.dtInicio", startDate)
                .whereLessThan("event.dtInicio", endDate);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            System.out.println("Document ID: " + document.getId());
            System.out.println("Document data: " + document.getData());
        }
    }
}


