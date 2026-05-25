package com.cmms.repository;

import com.cmms.model.LoginHistory;
import com.cmms.model.LoginHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, LoginHistoryId> {
}
