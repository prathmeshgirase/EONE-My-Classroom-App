package com.eone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="eone.storage.provider", havingValue="local", matchIfMissing=true)
public class LocalFileStorage implements FileStorage {
    private final Path root;
    public LocalFileStorage(@Value("${eone.storage.directory:./data/uploads}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }
    private Path path(String key) {
        if (!key.matches("[0-9a-f-]{36}")) throw new IllegalArgumentException("Invalid storage key");
        return root.resolve(key);
    }
    public void write(String key, byte[] bytes) throws IOException {
        Files.createDirectories(root);
        Path target = path(key);
        // CREATE_NEW must never replace (or delete) an existing object on collision.
        var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
        try (output) { output.write(bytes); }
        catch (IOException e) { Files.deleteIfExists(target); throw e; }
    }
    public Resource read(String key) throws IOException {
        Path target = path(key);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) throw new NoSuchFileException(key);
        return new FileSystemResource(target);
    }
    public void delete(String key) throws IOException { Files.deleteIfExists(path(key)); }
}
