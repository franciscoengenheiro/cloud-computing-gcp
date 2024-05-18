package app;

import app.services.firestore.LabelsAppFirestoreOperations;
import app.services.firestore.ProcessedImageData;
import app.services.pubsub.GooglePubSub;
import app.services.pubsub.LabelsGooglePubSubService;
import com.google.cloud.storage.BlobId;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.cloud.vision.v1.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LabelsApp {
    private final static String PROJECT_ID = "cn2324-t1-g04";
    private final static String SUBSCRIPTION_ID = "labelsApp";
    private static final Logger logger = Logger.getLogger(LabelsApp.class.getName());
    private static final LabelsAppFirestoreOperations firestoreOperations;

    static {
        try {
            firestoreOperations = new LabelsAppFirestoreOperations();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        logger.info("LabelsApp started");
        LabelsGooglePubSubService labelsPubSubService = new LabelsGooglePubSubService(new GooglePubSub());
        labelsPubSubService.subscribe(PROJECT_ID, SUBSCRIPTION_ID,
                (String requestId, String imageName, String bucketName, String blobName, String translationLang) -> {
                    try {
                        logger.info("Request ID: " + requestId);
                        // create blob id from bucket name and blob name to get the image
                        BlobId blobId = BlobId.of(bucketName, blobName);
                        // detect labels in pictures
                        List<String> labels = detectLabels(blobId.toGsUtilUri());
                        System.out.println("Labels detected: " + labels);
                        // translate labels to a specific language
                        List<String> labelsTranslated = translateLabels(labels, translationLang);
                        System.out.println("Labels translated: " + labelsTranslated);
                        // save processed image in a data structure
                        ProcessedImageData processedImageData = new ProcessedImageData(
                                requestId,
                                imageName,
                                labels,
                                labelsTranslated,
                                translationLang
                        );
                        firestoreOperations.saveImage(processedImageData);
                    } catch (Exception ex) {
                        logger.severe("Error processing image: " + ex.getMessage());
                    }
                }
        );
    }

    public static List<String> detectLabels(String gsURI) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        // Obtém imagem diretamente de um ficheiro: para testes locais
        // (EDIT: atualizado para usar os ficheiros da pasta resources)
        // Obtém imagem diretamente do serviço Storage usando um gs URI (gs://...) para o Blob com imagem
        System.out.println("Detecting labels in image: " + gsURI);
        Image img = Image.newBuilder()
                .setSource(ImageSource.newBuilder().setImageUri(gsURI).build())
                .build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        // Initialize a client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    logger.severe("Error: " + res.getError().getMessage());
                } else {
                    // For the full list of available annotations, see http://g.co/cloud/vision/docs
                    for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                        labels.add(annotation.getDescription());
                    }
                }
            }
        }
        return labels;
    }

    static List<String> translateLabels(List<String> labels, String translationLang) {
        System.out.println("Translating labels to: " + translationLang);
        List<String> labelsTranslated = null;
        try {
            Translate translateService = TranslateOptions.getDefaultInstance().getService();
            labelsTranslated = new ArrayList<>();
            for (String label : labels) {
                Translation translation = translateService.translate(
                        label,
                        Translate.TranslateOption.sourceLanguage("en"),
                        Translate.TranslateOption.targetLanguage(translationLang));
                labelsTranslated.add(translation.getTranslatedText());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            return labelsTranslated;
        }
    }

}

