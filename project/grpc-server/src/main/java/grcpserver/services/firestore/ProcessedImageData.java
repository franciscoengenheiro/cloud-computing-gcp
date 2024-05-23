package grcpserver.services.firestore;

import com.google.cloud.Timestamp;

import java.util.List;

public class ProcessedImageData {
    private String id;
    private String name;
    private String translationLang;
    private Timestamp timestamp;
    private List<String> labels;
    private List<String> translatedLabels;

    public ProcessedImageData() {
        this.id = "";
        this.name = "";
        this.labels = null;
        this.translatedLabels = null;
        this.translationLang = "";
        this.timestamp = null;
    }

    public ProcessedImageData(String id,
                              String name,
                              List<String> labels,
                              List<String> translatedLabels,
                              String translationLang,
                              Timestamp timestamp) {
        this.id = id;
        this.name = name;
        this.labels = labels;
        this.translatedLabels = translatedLabels;
        this.translationLang = translationLang;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getId() {
        return id;
    }

    public String getTranslationLang() {
        return translationLang;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<String> getTranslatedLabels() {
        return translatedLabels;
    }
    @Override
    public String toString() {
        return "ProcessedImageData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", translationLang='" + translationLang + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", labels=" + labels +
                ", translatedLabels=" + translatedLabels +
                '}';
    }
}
