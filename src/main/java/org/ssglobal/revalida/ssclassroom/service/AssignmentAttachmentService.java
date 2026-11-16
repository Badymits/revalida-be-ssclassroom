package org.ssglobal.revalida.ssclassroom.service;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AttachmentRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AttachmentResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AttachmentUploadRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AttachmentUploadResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.AttachmentKind;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.PayloadTooLargeException;
import org.ssglobal.revalida.ssclassroom.model.AssignmentAttachmentModel;
import org.ssglobal.revalida.ssclassroom.repository.AssignmentAttachmentRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Links, images and files that a trainer attaches to an assignment's instructions.
 * Files and images go straight from the browser to S3 through a presigned URL;
 * this service only validates the request and keeps the metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentAttachmentService {

    /** 15 MB per file or image. Keep in sync with the V10 migration check and the frontend. */
    public static final long MAX_BYTES = 15L * 1024 * 1024;
    public static final int MAX_PER_ASSIGNMENT = 10;

    private static final Set<String> FILE_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv", "zip");
    // no svg on purpose: it can carry scripts
    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "gif", "webp");

    private static final String KEY_PREFIX = "att/";

    private final AssignmentAttachmentRepository attachmentRepo;
    private final S3Service s3Service;

    // ---------- upload URL ----------

    public AttachmentUploadResponseDto createUploadUrl(AttachmentUploadRequestDto req) {
        if (req.getKind() == null || req.getKind() == AttachmentKind.link) {
            throw new BadRequestException("kind", "Kind must be 'file' or 'image'");
        }
        if (req.getFilename() == null || req.getFilename().isBlank()) {
            throw new BadRequestException("filename", "Filename is required");
        }
        if (req.getSizeBytes() == null || req.getSizeBytes() < 1) {
            throw new BadRequestException("sizeBytes", "File is empty");
        }
        if (req.getSizeBytes() > MAX_BYTES) {
            throw new PayloadTooLargeException("File is too large. The limit is 15 MB.");
        }

        requireAllowedExtension(req.getKind(), req.getFilename(), "filename");

        String contentType = normalizeContentType(req.getContentType());
        if (req.getKind() == AttachmentKind.image && !contentType.startsWith("image/")) {
            throw new BadRequestException("contentType", "That file is not an image");
        }

        PresignedS3UrlDto signed = s3Service.createAttachmentPutUrl(
                req.getFilename(), contentType, req.getSizeBytes());

        return new AttachmentUploadResponseDto(signed.getS3Key(), contentType, MAX_BYTES, signed);
    }

    // ---------- read ----------

    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> listFor(String assignmentId) {
        return attachmentRepo.findByAssignmentIdOrderBySortOrderAsc(assignmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ---------- write ----------

    /** Makes the stored attachments match the list the trainer saved (add / remove / reorder). */
    @Transactional
    public void replaceAll(String assignmentId, List<AttachmentRequestDto> incoming) {
        if (incoming.size() > MAX_PER_ASSIGNMENT) {
            throw new BadRequestException("attachments",
                    "An assignment can have at most " + MAX_PER_ASSIGNMENT + " attachments");
        }
        incoming.forEach(this::validate);   // validate everything before touching the database

        List<AssignmentAttachmentModel> existing =
                attachmentRepo.findByAssignmentIdOrderBySortOrderAsc(assignmentId);

        Set<String> keptKeys = new HashSet<>();
        for (AttachmentRequestDto dto : incoming) {
            if (dto.getS3Key() != null) keptKeys.add(dto.getS3Key());
        }
        List<String> orphanKeys = existing.stream()
                .map(AssignmentAttachmentModel::getS3Key)
                .filter(key -> key != null && !keptKeys.contains(key))
                .toList();

        attachmentRepo.deleteAllByAssignment(assignmentId);

        int order = 0;
        for (AttachmentRequestDto dto : incoming) {
            AssignmentAttachmentModel m = new AssignmentAttachmentModel();
            m.setAssignmentId(assignmentId);
            m.setKind(dto.getKind());
            m.setSortOrder(order++);
            if (dto.getKind() == AttachmentKind.link) {
                m.setUrl(dto.getUrl().trim());
                m.setName(blankToNull(dto.getName()));
            } else {
                m.setName(dto.getName().trim());
                m.setContentType(normalizeContentType(dto.getContentType()));
                m.setSizeBytes(dto.getSizeBytes());
                m.setS3Key(dto.getS3Key());
            }
            attachmentRepo.save(m);
        }

        deleteFromS3AfterCommit(orphanKeys);
    }

    // ---------- helpers ----------

    private void validate(AttachmentRequestDto dto) {
        if (dto.getKind() == null) {
            throw new BadRequestException("attachments", "Every attachment needs a kind");
        }

        if (dto.getKind() == AttachmentKind.link) {
            String url = dto.getUrl() == null ? "" : dto.getUrl().trim();
            if (url.isEmpty() || url.length() > 2048) {
                throw new BadRequestException("attachments", "A link must be 1 to 2048 characters");
            }
            try {
                URI uri = URI.create(url);
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("attachments", "Links must start with http:// or https://");
            }
            if (dto.getName() != null && dto.getName().length() > 255) {
                throw new BadRequestException("attachments", "A link label must be 255 characters or fewer");
            }
            return;
        }

        // file / image
        if (dto.getS3Key() == null || !dto.getS3Key().startsWith(KEY_PREFIX) || dto.getS3Key().length() > 1024) {
            throw new BadRequestException("attachments", "Upload the file again, its storage key is invalid");
        }
        if (dto.getName() == null || dto.getName().isBlank() || dto.getName().length() > 255) {
            throw new BadRequestException("attachments", "A file name must be 1 to 255 characters");
        }
        if (dto.getSizeBytes() == null || dto.getSizeBytes() < 1) {
            throw new BadRequestException("attachments", "File size is missing");
        }
        if (dto.getSizeBytes() > MAX_BYTES) {
            throw new PayloadTooLargeException("File is too large. The limit is 15 MB.");
        }
        requireAllowedExtension(dto.getKind(), dto.getName(), "attachments");
    }

    private void requireAllowedExtension(AttachmentKind kind, String filename, String field) {
        String ext = extensionOf(filename);
        Set<String> allowed = kind == AttachmentKind.image ? IMAGE_EXTENSIONS : FILE_EXTENSIONS;
        if (!allowed.contains(ext)) {
            throw new BadRequestException(field,
                    "Unsupported " + kind + " type" + (ext.isEmpty() ? "" : ": ." + ext)
                            + ". Allowed: " + String.join(", ", new java.util.TreeSet<>(allowed)));
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot < 0 || dot == filename.length() - 1)
                ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return "application/octet-stream";
        String base = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        return base.isEmpty() || base.length() > 100 ? "application/octet-stream" : base;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Removes files the trainer took out, but only once the database change has really committed. */
    private void deleteFromS3AfterCommit(List<String> keys) {
        if (keys.isEmpty()) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String key : keys) {
                    try {
                        s3Service.deleteAttachmentObject(key);
                    } catch (Exception e) {
                        log.warn("Could not delete attachment {} from S3: {}", key, e.getMessage());
                    }
                }
            }
        });
    }

    private AttachmentResponseDto toResponse(AssignmentAttachmentModel m) {
        boolean isLink = m.getKind() == AttachmentKind.link;
        return AttachmentResponseDto.builder()
                .id(m.getAttachmentId())
                .kind(m.getKind())
                .name(m.getName())
                .contentType(m.getContentType())
                .sizeBytes(m.getSizeBytes())
                .s3Key(m.getS3Key())
                .url(m.getUrl())
                .downloadUrl(isLink ? null : s3Service.createAttachmentGetUrl(m.getS3Key()))
                .build();
    }
}
