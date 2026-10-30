package com.arjun.crm.service.impl;

import com.arjun.crm.dto.response.TaskAttachmentResponse;
import com.arjun.crm.entity.Task;
import com.arjun.crm.entity.TaskAttachment;
import com.arjun.crm.entity.User;
import com.arjun.crm.exception.AccessDeniedException;
import com.arjun.crm.exception.ResourceNotFoundException;
import com.arjun.crm.repository.TaskAttachmentRepository;
import com.arjun.crm.repository.TaskRepository;
import com.arjun.crm.repository.UserRepository;
import com.arjun.crm.service.FileStorageService;
import com.arjun.crm.service.TaskAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskAttachmentServiceImpl implements TaskAttachmentService {

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    @Override
    @Transactional
    public TaskAttachmentResponse uploadAttachment(Long taskId, MultipartFile file) {
        User currentUser = getAuthenticatedUser();
        log.info("Uploading attachment to task ID: {} by user: {}", taskId, currentUser.getEmail());

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String fileUrl = fileStorageService.store(file, currentUser.getId());

        // Create attachment entity
        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .uploadedBy(currentUser)
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        TaskAttachment savedAttachment = taskAttachmentRepository.save(attachment);
        log.info("Attachment uploaded successfully: {}", savedAttachment.getId());

        return TaskAttachmentResponse.fromEntity(savedAttachment);
    }

    @Override
    @Transactional
    public void deleteAttachment(Long taskId, Long attachmentId) {
        User currentUser = getAuthenticatedUser();
        log.info("Deleting attachment ID: {} from task ID: {}", attachmentId, taskId);

        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with ID: " + attachmentId));

        // Verify attachment belongs to task
        if (!attachment.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Attachment does not belong to this task");
        }

        // Only uploader can delete
        if (!attachment.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only delete your own attachments");
        }

        taskAttachmentRepository.delete(attachment);
        fileStorageService.deleteByUrl(attachment.getFileUrl());
        log.info("Attachment deleted successfully: {}", attachmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> listAttachments(Long taskId) {
        log.info("Listing attachments for task ID: {}", taskId);

        // Verify task exists
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        List<TaskAttachment> attachments = taskAttachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId);
        return attachments.stream()
                .map(TaskAttachmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAttachmentResponse getAttachment(Long attachmentId) {
        log.info("Fetching attachment ID: {}", attachmentId);

        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with ID: " + attachmentId));

        return TaskAttachmentResponse.fromEntity(attachment);
    }

    /**
     * Get authenticated user from security context
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
