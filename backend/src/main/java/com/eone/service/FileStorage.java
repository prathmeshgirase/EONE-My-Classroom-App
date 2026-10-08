package com.eone.service;
import org.springframework.core.io.Resource;
import java.io.IOException;
public interface FileStorage {
    void write(String key, byte[] bytes) throws IOException;
    Resource read(String key) throws IOException;
    void delete(String key) throws IOException;
}
