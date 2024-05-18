package app.services.firestore;

import java.time.LocalDateTime;
import java.util.List;

public class ProcessedImageData {
    private final String id;
    private final String name;
    private final String translationLang;
    private final String timestamp;
    private final List<String> labels;
    private final List<String> translatedLabels;

    public ProcessedImageData(String id,
                              String name,
                              List<String> labels,
                              List<String> translatedLabels,
                              String translationLang) {
        this.id = id;
        this.name = name;
        this.labels = labels;
        this.translatedLabels = translatedLabels;
        this.translationLang = translationLang;
        this.timestamp = LocalDateTime.now().toString();
    }

    public String getName() {
        return name;
    }

    public String getTimestamp() {
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
}
