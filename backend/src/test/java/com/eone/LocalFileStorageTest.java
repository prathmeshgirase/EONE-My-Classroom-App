package com.eone;

import com.eone.service.LocalFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class LocalFileStorageTest {
    @TempDir Path directory;
    @Test void storageRejectsTraversalAndNeverOverwritesExistingBytes() throws Exception {
        var storage = new LocalFileStorage(directory.toString());
        String key = UUID.randomUUID().toString();
        storage.write(key, new byte[]{1,2,3});
        assertThatThrownBy(() -> storage.write(key, new byte[]{4})).isInstanceOf(FileAlreadyExistsException.class);
        assertThat(storage.read(key).getContentAsByteArray()).containsExactly(1,2,3);
        assertThatThrownBy(() -> storage.read("../secret")).isInstanceOf(IllegalArgumentException.class);
        storage.delete(key);
        assertThatThrownBy(() -> storage.read(key)).isInstanceOf(NoSuchFileException.class);
    }
}
