package com.cmms.repository;

import com.cmms.model.CodeItem;
import com.cmms.model.CodeItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeItemRepository extends JpaRepository<CodeItem, CodeItemId> {
    List<CodeItem> findByCompanyIdAndGroupIdOrderBySortOrderAsc(String companyId, String groupId);
}
