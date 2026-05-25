package com.cmms.controller;

import com.cmms.dto.PmDto.PmSaveRequest;
import com.cmms.dto.PmDto.PmScheduleResponse;
import com.cmms.model.PmRecord;
import com.cmms.model.PmRecordItem;
import com.cmms.security.UserPrincipal;
import com.cmms.service.PmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pm")
public class PmController {

    @Autowired
    private PmService pmService;

    @GetMapping("/schedules")
    public ResponseEntity<List<PmScheduleResponse>> getPmSchedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now();
        return ResponseEntity.ok(pmService.getPmSchedules(principal.getCompanyId(), date));
    }

    @GetMapping("/records")
    public ResponseEntity<List<PmRecord>> getPmRecords(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pmService.getPmRecordsByCompany(principal.getCompanyId()));
    }

    @GetMapping("/records/details")
    public ResponseEntity<PmSaveRequest> getPmRecordDetails(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pmService.getPmRecordDetails(principal.getCompanyId(), plantId, id));
    }

    @GetMapping("/records/initial-items")
    public ResponseEntity<List<PmRecordItem>> getInitialCheckItems(
            @RequestParam String plantId, @RequestParam String equipmentId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pmService.getInitialCheckItems(principal.getCompanyId(), plantId, equipmentId));
    }

    @PostMapping("/records")
    public ResponseEntity<PmRecord> savePmRecord(
            @RequestBody PmSaveRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pmService.savePmRecord(principal.getCompanyId(), request, principal.getUsername()));
    }

    @DeleteMapping("/records")
    public ResponseEntity<Void> deletePmRecord(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        pmService.deletePmRecord(principal.getCompanyId(), plantId, id, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
