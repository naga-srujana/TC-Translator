package com.student.tctranslator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// using OpenRouter API - free and reliable
// updated prompt to be friendly and funny, not mean
@Service
public class ClaudeService {

    @Value("${claude.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public String analyzeTermsAndConditions(String tcText) throws Exception {

        String prompt = buildPrompt(tcText);

        String requestBody = "{"
                + "\"model\": \"openrouter/auto\","
                + "\"max_tokens\": 4000,"
                + "\"messages\": ["
                + "  {"
                + "    \"role\": \"system\","
                + "    \"content\": \"You are a friendly legal document analyzer who helps regular people understand Terms and Conditions. You explain things simply like a helpful older sibling. Your normal explanations are clear and easy. Your savage explanations are funny and cheeky but never mean or offensive. Respond ONLY with pure JSON. No markdown, no backticks, no explanation before or after the JSON.\""
                + "  },"
                + "  {"
                + "    \"role\": \"user\","
                + "    \"content\": " + escapeJsonString(prompt)
                + "  }"
                + "]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "TC Translator")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println("OpenRouter status: " + response.statusCode());
        System.out.println("OpenRouter preview: " +
                response.body().substring(0, Math.min(400, response.body().length())));

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenRouter API error: "
                    + response.statusCode() + " - " + response.body());
        }

        return extractContent(response.body());
    }

    private String buildPrompt(String tcText) {
        String truncated = tcText;
        if (tcText.length() > 12000) {
            truncated = tcText.substring(0, 12000) + "\n\n[... document continues ...]";
        }

        return "Analyze this Terms and Conditions document and return ONLY valid JSON.\n\n"
                + "IMPORTANT TONE RULES:\n"
                + "- normalExplanation: Simple, clear, friendly. Like explaining to a younger sibling. Max 2 sentences.\n"
                + "- savageExplanation: Funny, cheeky, witty. Like a funny friend who just noticed something sneaky. Use emojis! Be amusing but NEVER mean, offensive, or use bad language. Max 2-3 sentences.\n"
                + "- oneLineSummary: Honest but friendly summary. No harsh words.\n\n"
                + "Return ONLY this JSON (start directly with {, no markdown):\n"
                + "{\n"
                + "  \"trustScore\": <number 0-10>,\n"
                + "  \"appName\": \"<detected app name or Unknown>\",\n"
                + "  \"criticalClauses\": [\n"
                + "    {\n"
                + "      \"title\": \"<short friendly title>\",\n"
                + "      \"normalExplanation\": \"<simple plain English, 1-2 sentences, friendly tone>\",\n"
                + "      \"savageExplanation\": \"<same point but funny and cheeky with emojis, never mean>\",\n"
                + "      \"originalQuote\": \"<actual clause text max 25 words>\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"warningClauses\": [<same structure, 2-4 items>],\n"
                + "  \"fairClauses\": [<same structure, 1-3 items>],\n"
                + "  \"certificateBullets\": [\"<4-5 short funny bullets of what user agreed to, start each with a verb>\"],\n"
                + "  \"oneLineSummary\": \"<one honest friendly sentence summarizing these terms>\"\n"
                + "}\n\n"
                + "Critical = user loses important rights (data sharing, no right to sue, owns your content)\n"
                + "Warning = worth knowing about (auto renewal, tracking, account deletion)\n"
                + "Fair = they actually did something good (data deletion, clear process)\n\n"
                + "Savage mode example - Normal: 'They can use your photos in ads.'\n"
                + "Savage: 'Surprise! You just became their unpaid model. 📸 Your selfies could show up in their ads and you agreed to it for free. Hope you were having a good hair day! 😄'\n\n"
                + "Terms and Conditions:\n" + truncated;
    }

    // extract the content field from OpenRouter's response
    // format: choices[0].message.content
    private String extractContent(String responseBody) {
        int contentIdx = responseBody.indexOf("\"content\":");
        if (contentIdx == -1) {
            throw new RuntimeException("No content in response: "
                    + responseBody.substring(0, Math.min(300, responseBody.length())));
        }

        int quoteStart = responseBody.indexOf("\"", contentIdx + 10) + 1;
        if (quoteStart == 0) {
            throw new RuntimeException("Malformed response");
        }

        StringBuilder result = new StringBuilder();
        int i = quoteStart;
        while (i < responseBody.length()) {
            char c = responseBody.charAt(i);
            if (c == '\\' && i + 1 < responseBody.length()) {
                char next = responseBody.charAt(i + 1);
                switch (next) {
                    case '"':  result.append('"');  i += 2; continue;
                    case '\\': result.append('\\'); i += 2; continue;
                    case 'n':  result.append('\n'); i += 2; continue;
                    case 'r':  result.append('\r'); i += 2; continue;
                    case 't':  result.append('\t'); i += 2; continue;
                    case 'u':
                        if (i + 5 < responseBody.length()) {
                            try {
                                String hex = responseBody.substring(i + 2, i + 6);
                                result.append((char) Integer.parseInt(hex, 16));
                                i += 6;
                                continue;
                            } catch (NumberFormatException e) {
                                result.append(c); i++; continue;
                            }
                        }
                    default: result.append(c); i++; continue;
                }
            }
            if (c == '"') break;
            result.append(c);
            i++;
        }

        String content = result.toString().trim();

        // strip markdown fences if model added them
        if (content.startsWith("```")) {
            int firstNewline = content.indexOf('\n');
            if (firstNewline != -1) content = content.substring(firstNewline + 1);
            if (content.endsWith("```")) {
                content = content.substring(0, content.lastIndexOf("```")).trim();
            }
        }

        // strip any text before the opening {
        int jsonStart = content.indexOf('{');
        if (jsonStart > 0) content = content.substring(jsonStart);

        return content;
    }

    private String escapeJsonString(String input) {
        String escaped = input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
