package com.aidetective.backend.analysis.service;

import com.aidetective.backend.analysis.exception.BadRequestException;
import com.aidetective.backend.config.ExtractionProperties;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class ArticleExtractionService {

    private final ExtractionProperties extractionProperties;

    public ArticleExtractionService(ExtractionProperties extractionProperties) {
        this.extractionProperties = extractionProperties;
    }

    public ExtractedArticle extract(String rawUrl) {
        var url = validateUrl(rawUrl);
        try {
            Document document = Jsoup.connect(url)
                .userAgent(extractionProperties.getUserAgent())
                .timeout(15000)
                .get();

            var title = firstNonBlank(
                document.select("meta[property=og:title]").attr("content"),
                document.title(),
                URI.create(url).getHost()
            );

            var paragraphs = document.select("article p, main p, section p, p").stream()
                .map(element -> element.text().trim())
                .filter(text -> text.length() > 35)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            var text = paragraphs.stream()
                .collect(Collectors.joining("\n\n"));

            if (text.isBlank()) {
                throw new BadRequestException("The URL was loaded, but no article text could be extracted.");
            }

            if (text.length() > extractionProperties.getMaxTextLength()) {
                text = text.substring(0, extractionProperties.getMaxTextLength());
            }

            return new ExtractedArticle(url, title, text);
        } catch (IOException exception) {
            throw new BadRequestException("Unable to fetch the URL for analysis.");
        }
    }

    private String validateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BadRequestException("Please provide a valid URL.");
        }
        try {
            var uri = URI.create(rawUrl.trim());
            var scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new BadRequestException("Only HTTP and HTTPS URLs are supported.");
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Please provide a valid URL.");
        }
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "Untitled source";
    }

    public record ExtractedArticle(String url, String title, String text) {
    }
}
