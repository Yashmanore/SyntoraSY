package com.example.societyhub.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PdfService {

    private final ThymeleafViewResolver thymeleafViewResolver;

    public PdfService(ThymeleafViewResolver thymeleafViewResolver) {
        this.thymeleafViewResolver = thymeleafViewResolver;
    }

    public byte[] generatePdf(String template, Map<String, String> data)
            throws Exception {

        Context context = new Context();
        context.setVariable("formData", data);

        String html = thymeleafViewResolver
                .getTemplateEngine()
                .process(template, context);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        HtmlConverter.convertToPdf(
                new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)),
                output
        );

        return output.toByteArray();
    }
}
