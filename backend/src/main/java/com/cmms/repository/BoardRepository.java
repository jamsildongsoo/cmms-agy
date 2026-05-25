package com.cmms.repository;

import com.cmms.model.Board;
import com.cmms.model.BoardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, BoardId> {
    List<Board> findByCompanyIdAndDeleteYnOrderByNoticeYnDescCreatedAtDesc(String companyId, String deleteYn);
}
