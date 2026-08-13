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
 * Sends emails via the Resend HTTP API (uses HTTPS port 443).
 * This avoids SMTP port restrictions on Render and other cloud platforms.
 *
 * Free tier: 3,000 emails/month — https://resend.com
 *
 * Required environment variable: RESEND_API_KEY
 */
@Service
public class MailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api.key:}")
    private String resendApiKey;

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
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new RuntimeException(
                "RESEND_API_KEY is not configured. Please set this environment variable."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromName + " <" + fromEmail + ">");
        payload.put("to", List.of(to));
        payload.put("subject", subject);
        // Send body as HTML paragraph, preserving line breaks
        payload.put("html", "<pre style='font-family:sans-serif'>" + escapeHtml(body) + "</pre>");

        // Attach PDF if provided (Resend supports base64 attachments)
        if (attachment != null && attachment.length > 0) {
            Map<String, String> att = new HashMap<>();
            att.put("filename", filename);
            att.put("content", Base64.getEncoder().encodeToString(attachment));
            payload.put("attachments", List.of(att));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(RESEND_API_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                    "Resend API error " + response.getStatusCode() + ": " + response.getBody()
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
