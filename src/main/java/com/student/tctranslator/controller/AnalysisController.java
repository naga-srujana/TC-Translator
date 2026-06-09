package com.student.tctranslator.controller;

import com.student.tctranslator.service.AnalysisResult;
import com.student.tctranslator.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// handles all the web routes
// keeping it thin - actual logic is in the service
@Controller
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    // home page
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // handle the form submission and run analysis
    @PostMapping("/analyze")
    public String analyze(
            @RequestParam("tcText") String tcText,
            @RequestParam(value = "appName", required = false, defaultValue = "") String appName,
            Model model) {

        // basic input validation - don't crash on empty input
        if (tcText == null || tcText.trim().length() < 50) {
            model.addAttribute("error", "Please paste at least some text from the Terms & Conditions (minimum 50 characters).");
            return "index";
        }

        if (tcText.trim().length() > 100000) {
            model.addAttribute("error", "Text is too long! Please paste the first part of the T&C (max 100,000 characters).");
            return "index";
        }

        try {
            AnalysisResult result = analysisService.analyze(tcText.trim(), appName);
            model.addAttribute("result", result);

            // also pass today's date for the certificate
            model.addAttribute("today", java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

            return "result";

        } catch (Exception e) {
            // something went wrong - show error on home page
            System.err.println("Analysis failed: " + e.getMessage());
            e.printStackTrace();

            String errorMsg = "Analysis failed: ";
            if (e.getMessage() != null && e.getMessage().contains("API key")) {
                errorMsg += "Invalid API key. Make sure CLAUDE_API_KEY is set correctly.";
            } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                errorMsg += "Authentication failed. Check your API key.";
            } else if (e.getMessage() != null && e.getMessage().contains("429")) {
                errorMsg += "Too many requests. Please wait a moment and try again.";
            } else {
                errorMsg += "Something went wrong. Check the logs for details.";
            }

            model.addAttribute("error", errorMsg);
            return "index";
        }
    }

    // leaderboard page
    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("mostPredatory", analysisService.getMostPredatory());
        model.addAttribute("mostHonest", analysisService.getMostHonest());
        return "leaderboard";
    }
}
