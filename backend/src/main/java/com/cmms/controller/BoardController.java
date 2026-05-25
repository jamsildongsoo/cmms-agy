package com.cmms.controller;

import com.cmms.dto.BoardDto.BoardDetailResponse;
import com.cmms.model.Board;
import com.cmms.model.BoardComment;
import com.cmms.security.UserPrincipal;
import com.cmms.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @GetMapping
    public ResponseEntity<List<Board>> getBoards(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.getBoards(principal.getCompanyId()));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<BoardDetailResponse> getBoardDetails(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.getBoardDetails(principal.getCompanyId(), id));
    }

    @PostMapping
    public ResponseEntity<Board> saveBoard(
            @RequestBody Board board, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.saveBoard(principal.getCompanyId(), board, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        boardService.deleteBoard(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comment")
    public ResponseEntity<BoardComment> saveComment(
            @RequestBody BoardComment comment, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(boardService.saveComment(principal.getCompanyId(), comment, principal.getName()));
    }

    @DeleteMapping("/comment")
    public ResponseEntity<Void> deleteComment(
            @RequestParam Long boardId, @RequestParam Long commentNo, @AuthenticationPrincipal UserPrincipal principal) {
        boardService.deleteComment(principal.getCompanyId(), boardId, commentNo);
        return ResponseEntity.ok().build();
    }
}
