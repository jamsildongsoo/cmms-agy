package com.cmms.dto;

import com.cmms.model.Board;
import com.cmms.model.BoardComment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class BoardDto {

    @Getter
    @Setter
    public static class BoardDetailResponse {
        private Board board;
        private List<BoardComment> comments;
    }
}
