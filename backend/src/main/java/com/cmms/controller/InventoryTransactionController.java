package com.cmms.controller;

import com.cmms.dto.InventoryTxDto.InventoryTxRequest;
import com.cmms.model.InventoryHistory;
import com.cmms.model.InventoryStatus;
import com.cmms.security.UserPrincipal;
import com.cmms.service.InventoryTransactionService;
import com.cmms.repository.InventoryStatusRepository;
import com.cmms.repository.InventoryHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-tx")
public class InventoryTransactionController {

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Autowired
    private InventoryStatusRepository inventoryStatusRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @GetMapping("/status")
    public ResponseEntity<List<InventoryStatus>> getInventoryStatus(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(inventoryStatusRepository.findByCompanyIdAndDeleteYn(principal.getCompanyId(), "N"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<InventoryHistory>> getInventoryHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(inventoryHistoryRepository.findByCompanyIdAndDeleteYn(principal.getCompanyId(), "N"));
    }

    @PostMapping
    public ResponseEntity<Void> processTransactions(
            @RequestBody InventoryTxRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        inventoryTransactionService.processTransactions(principal.getCompanyId(), request, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/close")
    public ResponseEntity<Void> closeMonth(
            @RequestParam String closingYm, @AuthenticationPrincipal UserPrincipal principal) {
        inventoryTransactionService.closeMonth(principal.getCompanyId(), closingYm, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
