package com.fram.insurance_manager.config.mail;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class MailgunService {

    @Value("${mailgun.api-key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    public void sendHtmlMessage(String to, String subject, String htmlContent) throws UnirestException {
        Unirest.post("https://api.mailgun.net/v3/" + domain + "/messages")
                .basicAuth("api", apiKey)
                .queryString("from", "Mailgun Sandbox <postmaster@" + domain + ">")
                .queryString("to", to)
                .queryString("subject", subject)
                .queryString("html", htmlContent)
                .asJson();
    }

    public void sendMessageWithAttachment(String to, String subject, String htmlContent, String filename, byte[] attachmentBytes) throws UnirestException {
        Unirest.post("https://api.mailgun.net/v3/" + domain + "/messages")
                .basicAuth("api", apiKey)
                .queryString("from", "Mailgun Sandbox <postmaster@" + domain + ">")
                .queryString("to", to)
                .queryString("subject", subject)
                .queryString("html", htmlContent)
                .field("attachment",
                        new ByteArrayInputStream(attachmentBytes),
                        filename).asJson();
    }
}