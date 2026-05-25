package com.cmms.model;

import java.io.Serializable;
import java.util.Objects;

public class BoardCommentId implements Serializable {
    private String companyId;
    private Long boardId;
    private Long commentNo;

    public BoardCommentId() {}

    public BoardCommentId(String companyId, Long boardId, Long commentNo) {
        this.companyId = companyId;
        this.boardId = boardId;
        this.commentNo = commentNo;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }
    public Long getCommentNo() { return commentNo; }
    public void setCommentNo(Long commentNo) { this.commentNo = commentNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardCommentId that = (BoardCommentId) o;
        return Objects.equals(companyId, that.companyId) &&
               Objects.equals(boardId, that.boardId) &&
               Objects.equals(commentNo, that.commentNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, boardId, commentNo);
    }
}
