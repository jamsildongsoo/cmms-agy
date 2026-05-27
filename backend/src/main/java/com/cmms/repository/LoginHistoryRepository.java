package com.cmms.repository;

import com.cmms.model.LoginHistory;
import com.cmms.model.LoginHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, LoginHistoryId> {
    List<LoginHistory> findTop200ByOrderByLoginAtDesc();
    List<LoginHistory> findByCompanyIdOrderByLoginAtDesc(String companyId);
    List<LoginHistory> findByCompanyIdAndUserIdOrderByLoginAtDesc(String companyId, String userId);
}
