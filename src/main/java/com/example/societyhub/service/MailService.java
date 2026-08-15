package com.example.societyhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends emails via the Brevo HTTP API (uses HTTPS port 443).
 * This avoids SMTP port restrictions on Render and other cloud platforms.
 *
 * Required environment variable: BREVO_API_KEY
 */
@Service
public class MailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${mail.from:societyhub18@gmail.com}")
    private String fromEmail;

    @Value("${mail.from.name:Syntora Society Management}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendEmail(
            String to,
            String subject,
            String body,
            byte[] attachment,
            String filename
    ) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new RuntimeException(
                "BREVO_API_KEY is not configured. Please set this environment variable."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of("name", fromName, "email", fromEmail));
        payload.put("to", List.of(Map.of("email", to)));
        payload.put("subject", subject);
        // Send body as HTML paragraph, preserving line breaks
        payload.put("htmlContent", "<pre style='font-family:sans-serif'>" + escapeHtml(body) + "</pre>");

        // Attach PDF if provided (Brevo supports base64 attachments)
        if (attachment != null && attachment.length > 0) {
            Map<String, String> att = new HashMap<>();
            att.put("name", filename);
            att.put("content", Base64.getEncoder().encodeToString(attachment));
            payload.put("attachment", List.of(att));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                    "Brevo API error " + response.getStatusCode() + ": " + response.getBody()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", "<br/>");
    }
}
