package com.cmms.repository;

import com.cmms.model.BoardComment;
import com.cmms.model.BoardCommentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardCommentRepository extends JpaRepository<BoardComment, BoardCommentId> {
    List<BoardComment> findByCompanyIdAndBoardIdOrderByCommentNoAsc(String companyId, Long boardId);
    void deleteByCompanyIdAndBoardId(String companyId, Long boardId);
}
