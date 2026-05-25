package com.cmms.repository;

import com.cmms.model.CodeGroup;
import com.cmms.model.CodeGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeGroupRepository extends JpaRepository<CodeGroup, CodeGroupId> {
    List<CodeGroup> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
