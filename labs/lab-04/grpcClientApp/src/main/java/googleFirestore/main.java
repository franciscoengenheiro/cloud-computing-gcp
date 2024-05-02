package googleFirestore;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;

import static googleFirestore.model.OcupacaoBuilder.insertDocuments;

public class main {
    public static void main(String[] args) {
        try {
            //TODO: ADD MENU
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

        insertDocuments("C:\\Coding\\CN-2324-G04\\labs\\lab-04\\grpcClientApp\\src\\main\\java\\res\\OcupacaoEspacosPublicos.csv", db, "lab04");
    }
}
