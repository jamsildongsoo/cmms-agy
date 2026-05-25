package com.cmms.controller;

import com.cmms.dto.MasterDto.EquipmentSaveRequest;
import com.cmms.model.Equipment;
import com.cmms.model.Inventory;
import com.cmms.security.UserPrincipal;
import com.cmms.service.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master")
public class MasterController {

    @Autowired
    private MasterService masterService;

    // ==========================================
    // 1. 설비 마스터 (Equipment)
    // ==========================================
    @GetMapping("/equipments")
    public ResponseEntity<List<Equipment>> getEquipments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.getEquipmentsByCompany(principal.getCompanyId()));
    }

    @GetMapping("/equipments/plant/{plantId}")
    public ResponseEntity<List<Equipment>> getEquipmentsByPlant(
            @PathVariable String plantId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.getEquipmentsByPlant(principal.getCompanyId(), plantId));
    }

    @GetMapping("/equipments/details")
    public ResponseEntity<EquipmentSaveRequest> getEquipmentDetails(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.getEquipmentWithDetails(principal.getCompanyId(), plantId, id));
    }

    @PostMapping("/equipments")
    public ResponseEntity<Equipment> saveEquipment(
            @RequestBody EquipmentSaveRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.saveEquipment(principal.getCompanyId(), request, principal.getUsername()));
    }

    @DeleteMapping("/equipments")
    public ResponseEntity<Void> deleteEquipment(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        masterService.deleteEquipment(principal.getCompanyId(), plantId, id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/equipments/csv")
    public ResponseEntity<String> downloadEquipmentsCsv(@AuthenticationPrincipal UserPrincipal principal) {
        String csv = masterService.exportEquipmentsToCsv(principal.getCompanyId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "equipments.csv");
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    // ==========================================
    // 2. 재고 마스터 (Inventory)
    // ==========================================
    @GetMapping("/inventories")
    public ResponseEntity<List<Inventory>> getInventories(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.getInventoriesByCompany(principal.getCompanyId()));
    }

    @GetMapping("/inventories/{id}")
    public ResponseEntity<Inventory> getInventory(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.getInventoryById(principal.getCompanyId(), id));
    }

    @PostMapping("/inventories")
    public ResponseEntity<Inventory> saveInventory(
            @RequestBody Inventory inventory, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(masterService.saveInventory(principal.getCompanyId(), inventory, principal.getUsername()));
    }

    @DeleteMapping("/inventories/{id}")
    public ResponseEntity<Void> deleteInventory(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        masterService.deleteInventory(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/inventories/csv")
    public ResponseEntity<String> downloadInventoriesCsv(@AuthenticationPrincipal UserPrincipal principal) {
        String csv = masterService.exportInventoriesToCsv(principal.getCompanyId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "inventories.csv");
        headers.setContentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }
}
