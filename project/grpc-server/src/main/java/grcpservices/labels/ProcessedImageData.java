package grcpservices.labels;

import java.util.List;

public record ProcessedImageData(
        String id,
        List<String> labels,
        List<String> labelsTranslated,
        String translationLang
) {
}
