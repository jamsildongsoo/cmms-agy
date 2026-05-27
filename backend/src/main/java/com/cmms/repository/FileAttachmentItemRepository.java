package com.cmms.repository;

import com.cmms.model.FileAttachmentItem;
import com.cmms.model.FileAttachmentItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentItemRepository extends JpaRepository<FileAttachmentItem, FileAttachmentItemId> {

    List<FileAttachmentItem> findByCompanyIdAndGroupNoOrderByItemNoAsc(String companyId, Long groupNo);

    @Query("select coalesce(max(i.itemNo), 0) from FileAttachmentItem i " +
           "where i.companyId = :companyId and i.groupNo = :groupNo")
    int findMaxItemNo(@Param("companyId") String companyId, @Param("groupNo") Long groupNo);

    /** reconciliation용: 모든 유효 메타의 storage_path 목록(고아 판별 기준). */
    @Query("select i.storagePath from FileAttachmentItem i")
    List<String> findAllStoragePaths();
}
