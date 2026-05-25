package com.cmms.service;

import com.cmms.dto.BoardDto.BoardDetailResponse;
import com.cmms.model.Board;
import com.cmms.model.BoardComment;
import com.cmms.model.BoardCommentId;
import com.cmms.model.BoardId;
import com.cmms.repository.BoardCommentRepository;
import com.cmms.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardCommentRepository boardCommentRepository;

    @Transactional(readOnly = true)
    public List<Board> getBoards(String companyId) {
        return boardRepository.findByCompanyIdAndDeleteYnOrderByNoticeYnDescCreatedAtDesc(companyId, "N");
    }

    @Transactional(readOnly = true)
    public BoardDetailResponse getBoardDetails(String companyId, Long id) {
        Board board = boardRepository.findById(new BoardId(companyId, id))
                .filter(b -> "N".equals(b.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        List<BoardComment> comments = boardCommentRepository.findByCompanyIdAndBoardIdOrderByCommentNoAsc(companyId, id);

        BoardDetailResponse response = new BoardDetailResponse();
        response.setBoard(board);
        response.setComments(comments);
        return response;
    }

    @Transactional
    public Board saveBoard(String companyId, Board board, String operator) {
        board.setCompanyId(companyId);

        boolean isNew = board.getId() == null;
        if (isNew) {
            board.setCreatedBy(operator);
        }
        board.setUpdatedBy(operator);
        board.setDeleteYn("N");

        return boardRepository.save(board);
    }

    @Transactional
    public void deleteBoard(String companyId, Long id, String operator) {
        Board board = boardRepository.findById(new BoardId(companyId, id))
                .filter(b -> "N".equals(b.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        board.setDeleteYn("Y");
        board.setUpdatedBy(operator);
        boardRepository.save(board);
    }

    @Transactional
    public BoardComment saveComment(String companyId, BoardComment comment, String operatorName) {
        comment.setCompanyId(companyId);
        
        // commentNo 수동 채번 (최대값 + 1)
        List<BoardComment> existings = boardCommentRepository.findByCompanyIdAndBoardIdOrderByCommentNoAsc(companyId, comment.getBoardId());
        long nextNo = existings.stream()
                .mapToLong(BoardComment::getCommentNo)
                .max()
                .orElse(0L) + 1;

        comment.setCommentNo(nextNo);
        comment.setAuthorName(operatorName);

        return boardCommentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(String companyId, Long boardId, Long commentNo) {
        BoardComment comment = boardCommentRepository.findById(new BoardCommentId(companyId, boardId, commentNo))
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        boardCommentRepository.delete(comment);
    }
}
