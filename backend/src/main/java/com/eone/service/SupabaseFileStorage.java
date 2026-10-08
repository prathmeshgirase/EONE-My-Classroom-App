package com.eone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.NoSuchFileException;
import java.time.Duration;

@Component @ConditionalOnProperty(name="eone.storage.provider", havingValue="supabase")
public class SupabaseFileStorage implements FileStorage {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String base, bucket, secret;
    public SupabaseFileStorage(@Value("${eone.storage.supabase.url:}") String url,
            @Value("${eone.storage.supabase.bucket:eone-private}") String bucket,
            @Value("${eone.storage.supabase.service-key:}") String secret) {
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null
                || !(uri.getPath().isEmpty() || uri.getPath().equals("/")) || !bucket.matches("[a-z0-9-]{1,63}") || secret.isBlank())
            throw new IllegalArgumentException("Configure HTTPS Supabase origin, private bucket and server-side service key");
        this.base = url.replaceAll("/+$", "") + "/storage/v1/object/"; this.bucket=bucket; this.secret=secret;
    }
    private HttpRequest.Builder request(String suffix) {
        return HttpRequest.newBuilder(URI.create(base+suffix)).timeout(Duration.ofSeconds(45))
                .header("apikey", secret).header("Authorization", "Bearer " + secret);
    }
    private String key(String key) {
        if (!key.matches("[0-9a-f-]{36}")) throw new IllegalArgumentException("Invalid storage key");
        return bucket + "/" + key;
    }
    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try { return http.send(request, HttpResponse.BodyHandlers.ofByteArray()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Storage request interrupted"); }
    }
    private void check(HttpResponse<?> response) throws IOException {
        if (response.statusCode()<200 || response.statusCode()>=300) throw new IOException("Storage provider request failed");
    }
    public void write(String key, byte[] bytes) throws IOException {
        check(send(request(key(key)).header("Content-Type", "application/octet-stream").header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build()));
    }
    public Resource read(String key) throws IOException {
        var response=send(request("authenticated/"+key(key)).GET().build());
        if (response.statusCode()==404) throw new NoSuchFileException(key);
        check(response); return new ByteArrayResource(response.body());
    }
    public void delete(String key) throws IOException {
        var response=send(request(key(key)).DELETE().build());
        if (response.statusCode()!=404) check(response);
    }
}
