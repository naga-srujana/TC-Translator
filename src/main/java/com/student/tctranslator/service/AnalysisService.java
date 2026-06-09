package com.student.tctranslator.service;

import com.student.tctranslator.model.Analysis;
import com.student.tctranslator.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// this service coordinates everything - calls Claude, parses the response,
// calculates the shock stats, and saves to the database
@Service
public class AnalysisService {

    @Autowired
    private ClaudeService claudeService;

    @Autowired
    private AnalysisRepository analysisRepository;

    public AnalysisResult analyze(String tcText, String appNameHint) throws Exception {
        long startTime = System.currentTimeMillis();

        // calculate shock stats before hitting the API
        // these are just string manipulation, fast and easy
        int wordCount = countWords(tcText);
        int sentenceCount = countSentences(tcText);
        String readingLevel = getReadingLevel(wordCount, sentenceCount);
        String readingTime = formatReadingTime(wordCount);

        // hit the Claude API - this is the slow part
        String jsonResponse = claudeService.analyzeTermsAndConditions(tcText);

        long analysisTimeMs = System.currentTimeMillis() - startTime;

        // parse Claude's JSON response into our AnalysisResult object
        AnalysisResult result = parseClaudeResponse(jsonResponse);

        // override app name if user provided one
        if (appNameHint != null && !appNameHint.trim().isEmpty()) {
            result.setAppName(appNameHint.trim());
        }
        if (result.getAppName() == null || result.getAppName().isEmpty()) {
            result.setAppName("Unknown App");
        }

        // set shock stats
        result.setWordCount(wordCount);
        result.setSentenceCount(sentenceCount);
        result.setReadingLevel(readingLevel);
        result.setReadingTimeFormatted(readingTime);
        result.setAnalysisTimeMs(analysisTimeMs);

        // set trust label and emoji based on score
        setTrustLabelAndEmoji(result);

        // save to leaderboard database
        try {
            Analysis savedAnalysis = new Analysis(
                    result.getAppName(),
                    result.getTrustScore(),
                    result.getTrustLabel(),
                    result.getTrustEmoji(),
                    result.getOneLineSummary(),
                    wordCount,
                    sentenceCount,
                    readingLevel
            );
            analysisRepository.save(savedAnalysis);
        } catch (Exception e) {
            // don't crash the whole thing if db save fails
            System.err.println("Warning: couldn't save to leaderboard: " + e.getMessage());
        }

        return result;
    }

    // I know this is a lot of string manipulation but it works and I understand it
    private AnalysisResult parseClaudeResponse(String json) {
        AnalysisResult result = new AnalysisResult();

        try {
            // extract trust score
            double trustScore = extractDouble(json, "trustScore");
            // clamp between 0 and 10 just in case
            trustScore = Math.max(0, Math.min(10, trustScore));
            result.setTrustScore(Math.round(trustScore * 10.0) / 10.0);

            // extract app name
            result.setAppName(extractString(json, "appName"));

            // extract one line summary
            result.setOneLineSummary(extractString(json, "oneLineSummary"));

            // extract clause arrays
            result.setCriticalClauses(extractClauses(json, "criticalClauses"));
            result.setWarningClauses(extractClauses(json, "warningClauses"));
            result.setFairClauses(extractClauses(json, "fairClauses"));

            // extract certificate bullets
            result.setCertificateBullets(extractStringArray(json, "certificateBullets"));

        } catch (Exception e) {
            // if parsing fails, give a fallback so we don't crash
            System.err.println("Error parsing Claude response: " + e.getMessage());
            System.err.println("Raw response was: " + json.substring(0, Math.min(500, json.length())));

            if (result.getTrustScore() == 0 && result.getOneLineSummary() == null) {
                result.setTrustScore(5.0);
                result.setOneLineSummary("Analysis completed but response parsing had issues.");
                result.setCriticalClauses(new ArrayList<>());
                result.setWarningClauses(new ArrayList<>());
                result.setFairClauses(new ArrayList<>());
                result.setCertificateBullets(new ArrayList<>());
            }
        }

        return result;
    }

    // extract a double value from JSON by key name
    private double extractDouble(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return 5.0; // default

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return 5.0;

        // skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        // find end of number
        int valueEnd = valueStart;
        while (valueEnd < json.length() &&
               (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.')) {
            valueEnd++;
        }

        try {
            return Double.parseDouble(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 5.0;
        }
    }

    // extract a string value from JSON by key name
    private String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1) return "";

        int quoteStart = json.indexOf("\"", colonIndex + 1);
        if (quoteStart == -1) return "";

        // find closing quote, handling escape sequences
        StringBuilder sb = new StringBuilder();
        int i = quoteStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n': sb.append('\n'); i += 2; continue;
                    case 'r': sb.append('\r'); i += 2; continue;
                    case 't': sb.append('\t'); i += 2; continue;
                    default: sb.append(c); i++; continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    // extract a string array from JSON
    private List<String> extractStringArray(String json, String key) {
        List<String> results = new ArrayList<>();
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return results;

        int arrayStart = json.indexOf("[", keyIndex);
        if (arrayStart == -1) return results;

        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayEnd == -1) return results;

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);

        // extract each quoted string from the array
        int pos = 0;
        while (pos < arrayContent.length()) {
            int quoteStart = arrayContent.indexOf("\"", pos);
            if (quoteStart == -1) break;

            StringBuilder sb = new StringBuilder();
            int i = quoteStart + 1;
            while (i < arrayContent.length()) {
                char c = arrayContent.charAt(i);
                if (c == '\\' && i + 1 < arrayContent.length()) {
                    char next = arrayContent.charAt(i + 1);
                    if (next == '"') { sb.append('"'); i += 2; continue; }
                    if (next == '\\') { sb.append('\\'); i += 2; continue; }
                    if (next == 'n') { sb.append('\n'); i += 2; continue; }
                    sb.append(c); i++; continue;
                }
                if (c == '"') break;
                sb.append(c);
                i++;
            }
            results.add(sb.toString());
            pos = i + 1;
        }

        return results;
    }

    // extract an array of clause objects from JSON
    private List<AnalysisResult.Clause> extractClauses(String json, String key) {
        List<AnalysisResult.Clause> clauses = new ArrayList<>();

        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return clauses;

        int arrayStart = json.indexOf("[", keyIndex);
        if (arrayStart == -1) return clauses;

        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayEnd == -1) return clauses;

        // find each object {...} in the array
        String arrayContent = json.substring(arrayStart + 1, arrayEnd);
        int pos = 0;
        while (pos < arrayContent.length()) {
            int objStart = arrayContent.indexOf("{", pos);
            if (objStart == -1) break;

            int objEnd = findMatchingBracket(arrayContent, objStart, '{', '}');
            if (objEnd == -1) break;

            String objContent = arrayContent.substring(objStart, objEnd + 1);

            AnalysisResult.Clause clause = new AnalysisResult.Clause();
            clause.setTitle(extractString(objContent, "title"));
            clause.setNormalExplanation(extractString(objContent, "normalExplanation"));
            clause.setSavageExplanation(extractString(objContent, "savageExplanation"));
            clause.setOriginalQuote(extractString(objContent, "originalQuote"));

            // only add if it has at least a title or explanation
            if (!clause.getTitle().isEmpty() || !clause.getNormalExplanation().isEmpty()) {
                clauses.add(clause);
            }

            pos = objEnd + 1;
        }

        return clauses;
    }

    // helper to find matching bracket - handles nested brackets
    private int findMatchingBracket(String text, int openPos, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && inString) {
                i++; // skip escaped char
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private void setTrustLabelAndEmoji(AnalysisResult result) {
        double score = result.getTrustScore();
        if (score < 2) {
            result.setTrustLabel("PREDATORY");
            result.setTrustEmoji("🦈");
        } else if (score < 4) {
            result.setTrustLabel("SUSPICIOUS");
            result.setTrustEmoji("🐍");
        } else if (score < 6) {
            result.setTrustLabel("AVERAGE");
            result.setTrustEmoji("😐");
        } else if (score < 8) {
            result.setTrustLabel("RESPECTFUL");
            result.setTrustEmoji("🤝");
        } else {
            result.setTrustLabel("HONEST");
            result.setTrustEmoji("🌟");
        }
    }

    // count words - just split by whitespace
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    // count sentences - split by .!? basically
    private int countSentences(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        String[] sentences = text.split("[.!?]+");
        int count = 0;
        for (String s : sentences) {
            if (!s.trim().isEmpty()) count++;
        }
        return Math.max(1, count);
    }

    // reading level based on words per sentence (simple heuristic)
    private String getReadingLevel(int words, int sentences) {
        if (sentences == 0) return "Unknown";
        double avgWordsPerSentence = (double) words / sentences;

        if (words > 10000 || avgWordsPerSentence > 30) {
            return "Legal Nightmare";
        } else if (words > 3000 || avgWordsPerSentence > 20) {
            return "Moderate";
        } else {
            return "Simple";
        }
    }

    // format reading time as "Xh Ym" or "Y min"
    // assuming 200 words per minute (average adult reading speed)
    private String formatReadingTime(int wordCount) {
        int totalMinutes = (int) Math.ceil(wordCount / 200.0);
        if (totalMinutes >= 60) {
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            return hours + "h " + minutes + "m";
        } else {
            return totalMinutes + " min";
        }
    }

    public List<Analysis> getMostPredatory() {
        return analysisRepository.findTop10ByOrderByTrustScoreAsc();
    }

    public List<Analysis> getMostHonest() {
        return analysisRepository.findTop10ByOrderByTrustScoreDesc();
    }
}
