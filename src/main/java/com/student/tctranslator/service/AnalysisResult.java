package com.student.tctranslator.service;

import java.util.List;
import java.util.ArrayList;

// this is basically just a data holder for the Claude API response
// not using a separate model class for this since it's just for display
public class AnalysisResult {

    private double trustScore;
    private String appName;
    private String trustLabel;
    private String trustEmoji;
    private String oneLineSummary;

    private List<Clause> criticalClauses = new ArrayList<>();
    private List<Clause> warningClauses = new ArrayList<>();
    private List<Clause> fairClauses = new ArrayList<>();

    private List<String> certificateBullets = new ArrayList<>();

    // shock stats - calculated from the raw text, not from Claude
    private int wordCount;
    private int sentenceCount;
    private String readingLevel;
    private String readingTimeFormatted;
    private long analysisTimeMs;

    // inner class for individual clauses
    public static class Clause {
        private String title;
        private String normalExplanation;
        private String savageExplanation;
        private String originalQuote;

        public Clause() {}

        public Clause(String title, String normalExplanation, String savageExplanation, String originalQuote) {
            this.title = title;
            this.normalExplanation = normalExplanation;
            this.savageExplanation = savageExplanation;
            this.originalQuote = originalQuote;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getNormalExplanation() { return normalExplanation; }
        public void setNormalExplanation(String normalExplanation) { this.normalExplanation = normalExplanation; }

        public String getSavageExplanation() { return savageExplanation; }
        public void setSavageExplanation(String savageExplanation) { this.savageExplanation = savageExplanation; }

        public String getOriginalQuote() { return originalQuote; }
        public void setOriginalQuote(String originalQuote) { this.originalQuote = originalQuote; }
    }

    // getters and setters for everything

    public double getTrustScore() { return trustScore; }
    public void setTrustScore(double trustScore) { this.trustScore = trustScore; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getTrustLabel() { return trustLabel; }
    public void setTrustLabel(String trustLabel) { this.trustLabel = trustLabel; }

    public String getTrustEmoji() { return trustEmoji; }
    public void setTrustEmoji(String trustEmoji) { this.trustEmoji = trustEmoji; }

    public String getOneLineSummary() { return oneLineSummary; }
    public void setOneLineSummary(String oneLineSummary) { this.oneLineSummary = oneLineSummary; }

    public List<Clause> getCriticalClauses() { return criticalClauses; }
    public void setCriticalClauses(List<Clause> criticalClauses) { this.criticalClauses = criticalClauses; }

    public List<Clause> getWarningClauses() { return warningClauses; }
    public void setWarningClauses(List<Clause> warningClauses) { this.warningClauses = warningClauses; }

    public List<Clause> getFairClauses() { return fairClauses; }
    public void setFairClauses(List<Clause> fairClauses) { this.fairClauses = fairClauses; }

    public List<String> getCertificateBullets() { return certificateBullets; }
    public void setCertificateBullets(List<String> certificateBullets) { this.certificateBullets = certificateBullets; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getSentenceCount() { return sentenceCount; }
    public void setSentenceCount(int sentenceCount) { this.sentenceCount = sentenceCount; }

    public String getReadingLevel() { return readingLevel; }
    public void setReadingLevel(String readingLevel) { this.readingLevel = readingLevel; }

    public String getReadingTimeFormatted() { return readingTimeFormatted; }
    public void setReadingTimeFormatted(String readingTimeFormatted) { this.readingTimeFormatted = readingTimeFormatted; }

    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public void setAnalysisTimeMs(long analysisTimeMs) { this.analysisTimeMs = analysisTimeMs; }
}
