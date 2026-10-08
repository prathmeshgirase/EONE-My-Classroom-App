package com.eone.service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @RequiredArgsConstructor @ConditionalOnProperty(name="eone.mail.enabled",havingValue="true")
public class RecoveryMailDelivery {
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final java.net.http.HttpClient http=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    @Value("${eone.mail.from}") private String from;
    @Value("${eone.mail.brevo-key:}") private String key;
    @jakarta.annotation.PostConstruct void validate(){
        if(from.isBlank() || key.isBlank()) throw new IllegalArgumentException("Configure verified MAIL_FROM and BREVO_API_KEY before enabling recovery email");
    }
    @Async @TransactionalEventListener
    public void send(RecoveryService.ResetMail event){
        try{
            var body=java.util.Map.of("sender",java.util.Map.of("email",from,"name","EONE"),
                "to",java.util.List.of(java.util.Map.of("email",event.email())),"subject","Reset your EONE password",
                "textContent","Enter this single-use code in EONE's password recovery screen within 30 minutes:\n\n"+event.token()+"\n\nIf you did not request this, ignore this email.");
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(java.time.Duration.ofSeconds(20)).header("api-key",key).header("Content-Type","application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var result=http.send(request,java.net.http.HttpResponse.BodyHandlers.discarding());
            if(result.statusCode()<200 || result.statusCode()>=300) failed();
        }catch(InterruptedException e){Thread.currentThread().interrupt();failed();}
        catch(Exception e){failed();}
    }
    private void failed(){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password recovery email delivery failed");}
    @Configuration @EnableAsync static class AsyncConfiguration {}
}
