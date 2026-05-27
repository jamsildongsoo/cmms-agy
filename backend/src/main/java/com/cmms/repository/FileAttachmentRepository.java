package com.cmms.repository;

import com.cmms.model.FileAttachment;
import com.cmms.model.FileAttachmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, FileAttachmentId> {
}
