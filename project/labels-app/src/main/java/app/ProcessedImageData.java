package app;

import java.util.List;

public class ProcessedImageData {
    private final String id;
    private final String translationLang;
    private final List<String> labels;
    private final List<String> translatedLabels;

    public ProcessedImageData(String id, List<String> labels, List<String> translatedLabels, String translationLang) {
        this.id = id;
        this.labels = labels;
        this.translatedLabels = translatedLabels;
        this.translationLang = translationLang;
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
