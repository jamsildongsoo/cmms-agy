package com.cmms.repository;

import com.cmms.model.Role;
import com.cmms.model.RoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, RoleId> {
    List<Role> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
