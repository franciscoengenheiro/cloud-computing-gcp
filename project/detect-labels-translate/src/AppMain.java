package gcpservices;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppMain {

    static String bucketName;
    static String PROJECT_ID;
    static String blobName;

    public static void main(String[] args) {
        // Assume:
        // - Vision API enabled no projeto GCP
        // - A variável de ambiente GOOGLE_APPLICATION_CREDENTIALS com uma chave de uma
        //   conta de serviço com roles "VisionAI Admin" e "Storage Admin"
        // - A API Translate não necessita de Role especifica, funcionando
        //    com qualquer chave válida numa conta de serviço do projeto GCP
        try {
            if (args.length != 3) {
                System.out.println("Use with arguments:<projectId> <bucket> <blob>");
                System.exit(-1);
            } else {
                PROJECT_ID = args[0];
                bucketName = args[1];
                blobName = args[2];
            }
            // detect labels in pictures
            List<String> labels = detectLabels("gs://" + bucketName + "/" + blobName);
            labels.forEach(label -> {
                System.out.println("Label detected:" + label);
            });
            // Para usar só Translatae API
                // List<String> labels = new ArrayList<>();
                // Collections.addAll(labels, "water", "car", "bike");
            // Translate Labels
            List<String> translatedLabels = TranslateLabels(labels);
            translatedLabels.forEach(translatedLabel -> {
                System.out.println("Label translated (en->pt): " + translatedLabel);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static List<String> detectLabels(String gsURI) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Obtém imagem diretamente de um ficheiro: para testes locais
            // ByteString imgBytes = ByteString.readFrom(new FileInputStream("cat.jpg"));
            //Image img = Image.newBuilder().setContent(imgBytes).build();
        // Obtém imagem diretamente do serviço Storage usando um gs URI (gs://...) para o Blob com imagem
        Image img = Image.newBuilder()
                .setSource(ImageSource.newBuilder().setImageUri(gsURI).build())
                .build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                } else {
                    // For full list of available annotations, see http://g.co/cloud/vision/docs
                    for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                        labels.add(annotation.getDescription());
//                        annotation.getAllFields()
//                                .forEach((k, v) -> System.out.format("%s : %s%n", k, v.toString()));
                    }
                }
            }
        }
        return labels;
    }

    static List<String> TranslateLabels(List<java.lang.String> labels) {
        List<java.lang.String> labelsTranslated = null;
        try {
            Translate translateService = TranslateOptions.getDefaultInstance().getService();
            labelsTranslated = new ArrayList<>();
            for (java.lang.String label : labels) {
                Translation translation = translateService.translate(
                        label,
                        Translate.TranslateOption.sourceLanguage("en"),
                        Translate.TranslateOption.targetLanguage("pt"));
                labelsTranslated.add(translation.getTranslatedText());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            return labelsTranslated;
        }
    }

}
