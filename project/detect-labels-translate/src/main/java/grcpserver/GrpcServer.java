package grcpserver;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import storageOperations.StorageOperations;

import java.util.Scanner;

public class GrpcServer {
    static StorageOperations storageOperations;

    public static String uploadImage() {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        storageOperations = new StorageOperations(storage);
        try {
            Scanner scan = new Scanner(System.in);
            System.out.println("Enter the name of the Bucket? ");
            String bucketName = scan.nextLine();
            System.out.println("Enter the name of the Blob? ");
            String blobName = scan.nextLine();
            System.out.println("Enter the pathname of the file to upload? ");
            String absFileName = scan.nextLine();
            BlobId id = storageOperations.uploadBlobToBucket(bucketName, blobName, absFileName);
            return id.toGsUtilUri();
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return null;
        }
    }
}
