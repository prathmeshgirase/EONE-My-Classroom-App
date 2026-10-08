package com.eone.service;

import com.eone.api.*;
import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(rollbackFor = IOException.class)
public class UploadService {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    private final FileStorage storage;
    private final StoredFileRepository files;
    private final ChapterRepository chapters;
    private final AssignmentRepository assignments;
    private final LearningMaterialRepository materials;
    private final UserRepository users;
    private final AccessService access;
    private final AssignmentLifecycleService lifecycle;
    private final NotificationService notifications;
    private final AuditService audit;

    public Dtos.MaterialView material(UUID chapterId, UUID actor, String title, String description, MultipartFile upload) throws IOException {
        Chapter chapter = chapters.findById(chapterId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chapter not found"));
        access.requireTeach(actor, chapter.getClassSubject().getId());
        if (title == null || title.isBlank() || title.length() > 180) bad("Enter a title up to 180 characters");
        StoredFile file = store(upload, actor, chapter, null);
        LearningMaterial material = new LearningMaterial();
        material.setChapter(chapter); material.setCreatedBy(users.getReferenceById(actor));
        material.setTitle(title.trim()); material.setDescription(description); material.setType(MaterialType.FILE);
        material.setUrl(url(file)); material = materials.save(material);
        notifications.notifyClassroomStudents(chapter.getClassSubject().getClassroom(), "New learning material", title,
                NotificationType.GENERAL, "MATERIAL", material.getId());
        audit.record(chapter.getClassSubject().getClassroom().getProgram().getDepartment().getId(),actor, "UPLOAD_MATERIAL", "LearningMaterial", material.getId(), title);
        return DtoMapper.material(material);
    }

    public Dtos.SubmissionView submission(UUID assignmentId, UUID actor, String answer, MultipartFile upload) throws IOException {
        Assignment assignment = assignments.findLockedById(assignmentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        access.requireViewClassSubject(actor, assignment.getClassSubject().getId());
        StoredFile file = store(upload, actor, null, assignment);
        // Lifecycle checks run in the same transaction; rejected submissions remove the new blob on rollback.
        return lifecycle.submit(assignmentId, actor, new Dtos.SubmitAssignmentRequest(answer, url(file)));
    }

    private StoredFile store(MultipartFile upload, UUID actor, Chapter chapter, Assignment assignment) throws IOException {
        if (upload.isEmpty()) bad("Choose a non-empty PDF, PNG or JPEG file");
        if (upload.getSize() > MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 10 MB or smaller");
        byte[] bytes;
        try (var input = upload.getInputStream()) { bytes = input.readNBytes(MAX_BYTES + 1); }
        if (bytes.length > MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 10 MB or smaller");
        String name = Optional.ofNullable(upload.getOriginalFilename()).orElse("upload").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._ -]", "_");
        if (name.length() > 180) name = name.substring(name.length() - 180);
        String lower = name.toLowerCase(Locale.ROOT);
        String type;
        if (lower.endsWith(".pdf") && starts(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII))) type = "application/pdf";
        else if (lower.endsWith(".png") && starts(bytes, new byte[]{(byte)137,80,78,71,13,10,26,10})) type = "image/png";
        else if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && starts(bytes, new byte[]{(byte)255,(byte)216,(byte)255})) type = "image/jpeg";
        else throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, PNG and JPEG files with matching contents are accepted");
        String key = UUID.randomUUID().toString();
        storage.write(key, bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { storage.delete(key); }
                    catch (IOException e) { org.slf4j.LoggerFactory.getLogger(UploadService.class).error("Rolled-back upload cleanup failed for {}", key); }
                }
            }
        });
        StoredFile file = new StoredFile(); file.setOwnerId(actor); file.setChapter(chapter); file.setAssignment(assignment);
        file.setStorageKey(key); file.setFilename(name); file.setContentType(type); file.setByteSize(bytes.length);
        return files.save(file);
    }
    private boolean starts(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length && Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }
    public static String url(StoredFile file) { return "/api/v1/files/" + file.getId(); }
    private void bad(String message) { throw new ApiException(HttpStatus.BAD_REQUEST, message); }
}
