package com.student.tctranslator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// this is what gets saved to the H2 database for the leaderboard
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appName;
    private double trustScore;
    private String trustLabel;      // PREDATORY, SUSPICIOUS, etc.
    private String trustEmoji;

    @Column(length = 500)
    private String oneLineSummary;

    private LocalDateTime analyzedAt;

    // word count and stuff for the shock stats
    private int wordCount;
    private int sentenceCount;
    private String readingLevel;

    // constructors
    public Analysis() {}

    public Analysis(String appName, double trustScore, String trustLabel,
                    String trustEmoji, String oneLineSummary,
                    int wordCount, int sentenceCount, String readingLevel) {
        this.appName = appName;
        this.trustScore = trustScore;
        this.trustLabel = trustLabel;
        this.trustEmoji = trustEmoji;
        this.oneLineSummary = oneLineSummary;
        this.wordCount = wordCount;
        this.sentenceCount = sentenceCount;
        this.readingLevel = readingLevel;
        this.analyzedAt = LocalDateTime.now();
    }

    // helper to get formatted date for the leaderboard
    public String getFormattedDate() {
        if (analyzedAt == null) return "Unknown";
        return analyzedAt.format(DateTimeFormatter.ofPattern("MMM yyyy"));
    }

    // getters and setters - I know this is verbose but whatever
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public double getTrustScore() { return trustScore; }
    public void setTrustScore(double trustScore) { this.trustScore = trustScore; }

    public String getTrustLabel() { return trustLabel; }
    public void setTrustLabel(String trustLabel) { this.trustLabel = trustLabel; }

    public String getTrustEmoji() { return trustEmoji; }
    public void setTrustEmoji(String trustEmoji) { this.trustEmoji = trustEmoji; }

    public String getOneLineSummary() { return oneLineSummary; }
    public void setOneLineSummary(String oneLineSummary) { this.oneLineSummary = oneLineSummary; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getSentenceCount() { return sentenceCount; }
    public void setSentenceCount(int sentenceCount) { this.sentenceCount = sentenceCount; }

    public String getReadingLevel() { return readingLevel; }
    public void setReadingLevel(String readingLevel) { this.readingLevel = readingLevel; }
}
