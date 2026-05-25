package com.cmms.repository;

import com.cmms.model.User;
import com.cmms.model.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UserId> {
    Optional<User> findByCompanyIdAndId(String companyId, String id);
    Optional<User> findByCompanyIdAndIdAndDeleteYnAndUseYn(String companyId, String id, String deleteYn, String useYn);
}
