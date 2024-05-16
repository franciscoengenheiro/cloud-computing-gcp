package googleFirestore;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static googleFirestore.model.OcupacaoBuilder.insertDocuments;

public class FireStoreLab4 {

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
            int option;
            do {
                System.out.println("\n######## MENU ##########");
                System.out.println("1: Insert data from local CSV file into Firestore");
                System.out.println("2: Show all documents in collection");
                System.out.println("3: Get document by ID");
                System.out.println("4: Delete document field");
                System.out.println("5: Obtain all documents of a certain freguesia");
                System.out.println("6: Obtain all documents with id bigger than a specific value, of a predefined freguesia, with a specific event type (composed indexed query)");
                System.out.println("7: Query documents with events that started in February 2017");
                System.out.println("8: Query documents with events fully realized in February 2017");
                System.out.println("0: Exit");
                System.out.println("########################");
                System.out.print("Enter an Option: ");
                Scanner scanner = new Scanner(System.in);
                option = scanner.nextInt();
                switch (option) {
                    case 1:
                        insertLocalDataIntoFirestore();
                        break;
                    case 2:
                        showCollectionData();
                        break;
                    case 3:
                        String docId = read("Enter the document ID (e.g., Lab4-2014): ");
                        getDocumentById(docId);
                        break;
                    case 4:
                        String docId2 = read("Enter the document ID (e.g., Lab4-2014): ");
                        String fieldName = read("Enter the field name to delete (e.g., location.coord): ");
                        deleteField(docId2, fieldName);
                        break;
                    case 5:
                        String freguesia = read("Enter the freguesia (e.g., Alvalade): ");
                        getAllDocsWithFreguesia(freguesia);
                        break;
                    case 6:
                        String idValue = read("Id field to be greater than (e.g., 2014): ");
                        String freguesia4 = read("Enter the freguesia (e.g., Alvalade): ");
                        String eventType4 = read("Enter the event type (e.g., Filmagem): ");
                        obtainAllDocumentsWithAComposedIndexedQuery(idValue, freguesia4, eventType4);
                        break;
                    case 7:
                        queryDocumentsWithEventsThatStartedInFebruary();
                        break;
                    case 8:
                        queryDocumentsWithEventsThatFinishedInFebruary(db);
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

    private static void showCollectionData() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = db.collection(collectionName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (documents.isEmpty()) {
            System.out.println("No documents found in collection " + collectionName);
        } else {
            System.out.println("Documents in collection " + collectionName + ":");
            for (QueryDocumentSnapshot document : documents) {
                System.out.println("Document ID: " + document.getId());
                System.out.println("Document data: " + document.getData());
            }
        }
    }

    // insert documents from local CSV file into Firestore
    private static void insertLocalDataIntoFirestore() throws Exception {
        insertDocuments("labs/lab-04/grpcClientApp/src/main/java/res/OcupacaoEspacosPublicos.csv", db, collectionName);
    }

    // alinea a): get document by ID
    private static void getDocumentById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            System.out.println("Document data: " + document.getData());
        } else {
            System.out.println("No such document!");
        }
    }

    // alinea b): delete a document field
    private static void deleteField(String documentId, String fieldName) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.document(collectionName + "/" + documentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, FieldValue.delete());
        ApiFuture<WriteResult> writeResult = docRef.update(updates);
        WriteResult result = writeResult.get();
        System.out.println("Update time : " + result.getUpdateTime());
    }

    // alinea c): obtain all documents of a certain freguesia
    private static void getAllDocsWithFreguesia(String freguesia) throws ExecutionException, InterruptedException {
        Query query = db.collection(collectionName).whereEqualTo("location.freguesia", freguesia);
        showQueriedData(query);
    }

    // alinea d): (composed indexed query) obtain all documents with:
    // - with id bigger than a specific value
    // - of a predefined freguesia
    // - with a specific event type
    private static void obtainAllDocumentsWithAComposedIndexedQuery(String idValue, String freguesia, String eventType) throws ExecutionException, InterruptedException {
        Query query = db.collection(collectionName)
                .whereGreaterThan(FieldPath.documentId(), "Lab4-" + idValue)
                .whereEqualTo("location.freguesia", freguesia)
                .whereEqualTo("event.tipo", eventType);

        showQueriedData(query);
    }

    // alinea e): query documents with events that started in February 2017
    private static void queryDocumentsWithEventsThatStartedInFebruary() throws ExecutionException, InterruptedException, ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = formatter.parse("31/01/2017");
        Date endDate = formatter.parse("01/03/2017");

        Query query = db.collection(collectionName)
                .whereGreaterThan("event.dtInicio", startDate)
                .whereLessThan("event.dtInicio", endDate);

        showQueriedData(query);
    }

    // alinea f): query documents with events fully realized in February 2017
    private static void queryDocumentsWithEventsThatFinishedInFebruary(Firestore db) throws ExecutionException, InterruptedException, ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = formatter.parse("31/01/2017");
        Date endDate = formatter.parse("01/03/2017");

        Query query = db.collection(collectionName)
                .whereGreaterThan("event.dtInicio", startDate);
                // cant do this (applying inequalities on different fields in the same composite query)
                // .whereLessThan("event.dtFinal", endDate);

        // Iterate through the results and filter by the end date,
        // because firestore does not allow composite queries with inequality on multiple fields
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        int counter = 0;
        for (QueryDocumentSnapshot document : documents) {
            Timestamp dtFinalField = (Timestamp) document.get("event.dtFinal");
            Objects.requireNonNull(dtFinalField);
            Date dtFinal = dtFinalField.toDate();
            if (dtFinal.before(endDate)) {
                counter++;
                System.out.println("Document ID: " + document.getId());
                System.out.println("Document data: " + document.getData());
            }
        }
        System.out.println("Total documents found: " + counter);

    }

    private static void showQueriedData(Query query) throws InterruptedException, ExecutionException {
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
        if (documents.isEmpty()) {
            System.out.println("No documents found in query.");
        } else {
            int nrDocs = documents.size();
            for (QueryDocumentSnapshot document : documents) {
                System.out.println("Document ID: " + document.getId());
                System.out.println("Document data: " + document.getData());
            }
            System.out.println("Total documents found: " + nrDocs);
        }
    }

    // helper method to read input from user
    private static String read(String message) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(message);
        return scanner.nextLine();
    }

}
