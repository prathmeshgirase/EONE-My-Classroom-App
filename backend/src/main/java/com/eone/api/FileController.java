package com.eone.api;

import com.eone.repository.StoredFileRepository;
import com.eone.security.CurrentUser;
import com.eone.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class FileController {
    private final UploadService uploads;
    private final StoredFileRepository files;
    private final FileStorage storage;
    private final AccessService access;

    @PostMapping(value = "/class-subjects/chapters/{id}/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.MaterialView material(@PathVariable UUID id, @RequestParam String title,
            @RequestParam(required = false) String description, @RequestParam MultipartFile file, Authentication auth) throws IOException {
        return uploads.material(id, CurrentUser.id(auth), title, description, file);
    }
    @PostMapping(value = "/assignments/{id}/submit-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dtos.SubmissionView submission(@PathVariable UUID id, @RequestParam(required = false) String answerText,
            @RequestParam MultipartFile file, Authentication auth) throws IOException {
        return uploads.submission(id, CurrentUser.id(auth), answerText, file);
    }
    @GetMapping("/files/{id}") @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(@PathVariable UUID id, Authentication auth) throws IOException {
        var file = files.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));
        UUID actor = CurrentUser.id(auth);
        if (file.getChapter() != null) access.requireViewClassSubject(actor, file.getChapter().getClassSubject().getId());
        else {
            UUID subject = file.getAssignment().getClassSubject().getId();
            access.requireViewClassSubject(actor, subject);
            if (!actor.equals(file.getOwnerId())) access.requireTeach(actor, subject);
        }
        Resource resource;
        try { resource = storage.read(file.getStorageKey()); }
        catch (java.nio.file.NoSuchFileException e) { throw new ApiException(HttpStatus.NOT_FOUND, "File is unavailable"); }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getByteSize()).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.getFilename()).build().toString())
                .body(resource);
    }
}
