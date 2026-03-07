package com.aidetective.backend.analysis.controller;

import com.aidetective.backend.analysis.dto.AnalysisHistoryItemResponse;
import com.aidetective.backend.analysis.dto.AnalysisResponse;
import com.aidetective.backend.analysis.dto.AnalyzeRequest;
import com.aidetective.backend.analysis.dto.FollowUpQuestionRequest;
import com.aidetective.backend.analysis.dto.FollowUpResponse;
import com.aidetective.backend.analysis.service.AnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return analysisService.analyze(request);
    }

    @PostMapping(value = "/analyze/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse analyzeUpload(
        @RequestPart("file") MultipartFile file,
        @RequestParam(name = "ocrEnabled", defaultValue = "true") boolean ocrEnabled
    ) {
        return analysisService.analyzeUpload(file, ocrEnabled);
    }

    @GetMapping("/history")
    public List<AnalysisHistoryItemResponse> getHistory() {
        return analysisService.getHistory();
    }

    @GetMapping("/history/{id}")
    public AnalysisResponse getById(@PathVariable String id) {
        return analysisService.getById(id);
    }

    @PostMapping("/history/{id}/follow-up")
    public FollowUpResponse followUp(@PathVariable String id, @Valid @RequestBody FollowUpQuestionRequest request) {
        return analysisService.followUp(id, request);
    }
}
