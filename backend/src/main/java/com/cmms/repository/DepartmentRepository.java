package com.cmms.repository;

import com.cmms.model.Department;
import com.cmms.model.DepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, DepartmentId> {
    List<Department> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
