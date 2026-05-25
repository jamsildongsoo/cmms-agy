package com.cmms.repository;

import com.cmms.model.RoleDetail;
import com.cmms.model.RoleDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleDetailRepository extends JpaRepository<RoleDetail, RoleDetailId> {
    List<RoleDetail> findByCompanyIdAndRoleId(String companyId, String roleId);
}
