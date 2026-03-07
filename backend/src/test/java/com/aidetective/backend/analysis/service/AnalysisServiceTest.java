package com.aidetective.backend.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aidetective.backend.analysis.dto.AnalyzeRequest;
import com.aidetective.backend.analysis.dto.AsiInvestigationResult;
import com.aidetective.backend.analysis.dto.AsiSignalExtractionResult;
import com.aidetective.backend.analysis.dto.FollowUpQuestionRequest;
import com.aidetective.backend.analysis.exception.BadRequestException;
import com.aidetective.backend.analysis.integration.AsiOneClient;
import com.aidetective.backend.analysis.model.AnalysisRecord;
import com.aidetective.backend.analysis.model.AnalysisStatus;
import com.aidetective.backend.analysis.model.InputType;
import com.aidetective.backend.analysis.model.Verdict;
import com.aidetective.backend.analysis.repository.AnalysisRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private ArticleExtractionService articleExtractionService;

    @Mock
    private AsiOneClient asiOneClient;

    @Mock
    private DocumentExtractionService documentExtractionService;

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    void shouldMapVerdictByScoreBands() {
        assertThat(Verdict.fromScore(10)).isEqualTo(Verdict.LIKELY_FAKE);
        assertThat(Verdict.fromScore(45)).isEqualTo(Verdict.QUESTIONABLE);
        assertThat(Verdict.fromScore(72)).isEqualTo(Verdict.LIKELY_TRUE);
        assertThat(Verdict.fromScore(94)).isEqualTo(Verdict.VERIFIED);
    }

    @Test
    void shouldRejectWhenSeveralInputsAreProvided() {
        var request = new AnalyzeRequest("text", "https://example.com", null, null, null, null);

        assertThatThrownBy(() -> analysisService.analyze(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Provide exactly one input: text, URL, image, or video URL.");
    }

    @Test
    void shouldAnalyzeImageAndPersistModelMetadata() {
        var request = new AnalyzeRequest(
            null,
            null,
            "aGVsbG8=",
            "image/png",
            "suspicious-post.png",
            null
        );
        var aiResult = new AsiInvestigationResult(
            "Image summary",
            List.of(new AsiInvestigationResult.AsiClaim(
                "Claim",
                "Suspicion",
                "Hint",
                "Contradicted",
                84,
                List.of("The visible logo does not match the claimed institution."),
                List.of("https://example.com/fact-check"),
                "Check the official institution website."
            )),
            List.of(new AsiInvestigationResult.AsiEntity("Entity", "Organization", "Context")),
            "Hinglish",
            List.of("misinformation", "public safety"),
            List.of("https://example.com/fact-check"),
            81,
            "Likely True",
            "Reasoning",
            List.of("The image was analyzed without original metadata."),
            List.of("Run a reverse image search.")
        );
        var extractionResult = new AsiSignalExtractionResult(
            "Visual summary",
            List.of(new AsiSignalExtractionResult.AsiClaimCandidate("Claim", "It could influence a safety decision.")),
            List.of(new AsiSignalExtractionResult.AsiEntity("Entity", "Organization", "Context")),
            "Hinglish",
            List.of("misinformation", "public safety")
        );

        when(asiOneClient.extractImageSignals("aGVsbG8=", "image/png", "suspicious-post.png", "suspicious-post.png"))
            .thenReturn(extractionResult);
        when(asiOneClient.analyzeInvestigation(null, InputType.IMAGE, null, "suspicious-post.png", "suspicious-post.png", extractionResult))
            .thenReturn(new AsiOneClient.AiAnalysis(aiResult, "asi1-vision"));
        when(analysisRepository.save(any(AnalysisRecord.class))).thenAnswer(invocation -> {
            AnalysisRecord record = invocation.getArgument(0);
            record.setId("img-1");
            return record;
        });

        var response = analysisService.analyze(request);

        assertThat(response.inputType()).isEqualTo(InputType.IMAGE);
        assertThat(response.imageMimeType()).isEqualTo("image/png");
        assertThat(response.modelUsed()).isEqualTo("asi1-vision");
        assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(response.contentLanguage()).isEqualTo("Hinglish");
        assertThat(response.riskLabels()).containsExactly("misinformation", "public safety");
        assertThat(response.claims().getFirst().assessment()).isEqualTo("Contradicted");
        assertThat(response.claims().getFirst().sourceUrls()).containsExactly("https://example.com/fact-check");
        assertThat(response.sourceTitle()).isEqualTo("suspicious-post.png");

        ArgumentCaptor<AnalysisRecord> captor = ArgumentCaptor.forClass(AnalysisRecord.class);
        verify(analysisRepository).save(captor.capture());
        assertThat(captor.getValue().getModelUsed()).isEqualTo("asi1-vision");
        assertThat(captor.getValue().getImageMimeType()).isEqualTo("image/png");
        assertThat(captor.getValue().getContentLanguage()).isEqualTo("Hinglish");
        assertThat(captor.getValue().getRiskLabels()).containsExactly("misinformation", "public safety");
    }

    @Test
    void shouldMarkBrokenVideoLinksAsCannotAnalyze() {
        var request = new AnalyzeRequest(
            null,
            null,
            null,
            null,
            null,
            "https://www.youtube.com/watch?v=ad79nYk2keg"
        );
        var extractionResult = new AsiSignalExtractionResult(
            "The submission points to a video URL.",
            List.of(),
            List.of(),
            "Unknown",
            List.of("broken_link")
        );
        var aiResult = new AsiInvestigationResult(
            "The YouTube video returns a 404 error and is inaccessible.",
            List.of(),
            List.of(),
            "Unknown",
            List.of("broken_link", "video inaccessible"),
            List.of("https://www.youtube.com/watch?v=ad79nYk2keg"),
            0,
            "Likely Fake",
            "No video content, metadata, or transcript can be retrieved, so the submission cannot be fact-checked.",
            List.of(),
            List.of()
        );

        when(asiOneClient.extractSignals(any(), any(), any(), any(), any())).thenReturn(extractionResult);
        when(asiOneClient.analyzeInvestigation(any(), any(), any(), any(), any(), any()))
            .thenReturn(new AsiOneClient.AiAnalysis(aiResult, "asi1"));
        when(analysisRepository.save(any(AnalysisRecord.class))).thenAnswer(invocation -> {
            AnalysisRecord record = invocation.getArgument(0);
            record.setId("vid-1");
            return record;
        });

        var response = analysisService.analyze(request);

        assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.CANNOT_ANALYZE);
        assertThat(response.statusReason()).contains("video URL is inaccessible");
        assertThat(response.credibilityScore()).isNull();
        assertThat(response.verdict()).isNull();
    }

    @Test
    void shouldRemapLegacyBrokenVideoReportsOnRead() {
        var record = new AnalysisRecord();
        record.setId("legacy-video");
        record.setInputType(InputType.VIDEO);
        record.setSummary("The YouTube video returns a 404 error and is inaccessible.");
        record.setReasoning("No video content, metadata, or transcript can be retrieved, so the submission cannot be fact-checked.");
        record.setRiskLabels(List.of("broken_link", "video inaccessible"));
        record.setVisitedUrls(List.of("https://www.youtube.com/watch?v=ad79nYk2keg"));
        record.setCredibilityScore(0);
        record.setVerdict(Verdict.LIKELY_FAKE);

        when(analysisRepository.findById("legacy-video")).thenReturn(Optional.of(record));

        var response = analysisService.getById("legacy-video");

        assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.CANNOT_ANALYZE);
        assertThat(response.statusReason()).contains("video URL is inaccessible");
        assertThat(response.credibilityScore()).isNull();
        assertThat(response.verdict()).isNull();
    }

    @Test
    void shouldAnswerFollowUpFromStoredReport() {
        var record = new AnalysisRecord();
        record.setId("case-1");
        record.setInputType(InputType.TEXT);
        record.setSourceLabel("Manual text submission");
        record.setContentLanguage("English");
        record.setVerdict(Verdict.QUESTIONABLE);
        record.setCredibilityScore(48);
        record.setSummary("A viral claim about school closures.");
        record.setReasoning("The message lacks attributable sourcing.");
        record.setVisitedUrls(List.of("https://example.com/fact-check"));

        when(analysisRepository.findById("case-1")).thenReturn(Optional.of(record));
        when(asiOneClient.followUp(record, "What is the weakest claim?"))
            .thenReturn(new AsiOneClient.FollowUpAnswer(
                "The weakest claim is the blanket closure statement because it lacks an attributable source.",
                List.of("https://example.com/fact-check"),
                List.of("Check the local education department website.")
            ));

        var response = analysisService.followUp("case-1", new FollowUpQuestionRequest("What is the weakest claim?"));

        assertThat(response.answer()).contains("weakest claim");
        assertThat(response.sourceUrls()).containsExactly("https://example.com/fact-check");
        assertThat(response.suggestedChecks()).containsExactly("Check the local education department website.");
    }
}
