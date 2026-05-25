package com.cmms.service;

import com.cmms.dto.ApprovalDto.*;
import com.cmms.model.*;
import com.cmms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ApprovalService {

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private ApprovalStepRepository approvalStepRepository;

    @Autowired
    private PmRecordRepository pmRecordRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkPermitRepository workPermitRepository;

    @Autowired
    private EquipmentCheckCycleRepository equipmentCheckCycleRepository;

    @Autowired
    private SequenceService sequenceService;

    @Transactional
    public Approval submitApproval(String companyId, ApprovalSubmitRequest request, String operator) {
        Approval approval = request.getApproval();
        approval.setCompanyId(companyId);

        String appNo = sequenceService.generateNextNo(companyId, "APR", "DEPT_ROOT");
        approval.setId(appNo);
        approval.setDrafterId(operator);
        approval.setStatus("P"); // 진행
        approval.setCreatedBy(operator);
        approval.setUpdatedBy(operator);
        approval.setDeleteYn("N");

        Approval savedApproval = approvalRepository.save(approval);

        // 단계 리스트 저장
        List<ApprovalStep> steps = request.getSteps();
        
        // 0. 기안자 자동 추가 (0순번)
        ApprovalStep draftStep = new ApprovalStep();
        draftStep.setCompanyId(companyId);
        draftStep.setApprovalId(appNo);
        draftStep.setStepNo(0);
        draftStep.setApproverId(operator);
        draftStep.setApprovalType("D");
        draftStep.setApprovalResult("A"); // 기안 시점 완료
        draftStep.setActionAt(LocalDateTime.now());
        draftStep.setComments("상신함");
        approvalStepRepository.save(draftStep);

        // 1번부터 결재선 순차 저장
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                ApprovalStep step = steps.get(i);
                step.setCompanyId(companyId);
                step.setApprovalId(appNo);
                step.setStepNo(i + 1);
                
                // 첫 결재자(1번)는 'P'(결재진행), 나머지는 'T'(대기)
                if (i == 0) {
                    step.setApprovalResult("P");
                } else {
                    step.setApprovalResult("T");
                }
                
                approvalStepRepository.save(step);
            }
        }

        // 연계 모듈 상태값 업데이트
        String refNo = request.getRefNo();
        String refModule = request.getRefModule();
        if (refNo != null && refModule != null) {
            updateLinkedModuleStatus(companyId, refModule, refNo, appNo, "P", operator);
        }

        return savedApproval;
    }

    @Transactional(readOnly = true)
    public List<Approval> getSentApprovals(String companyId, String userId) {
        return approvalRepository.findByCompanyIdAndDeleteYn(companyId, "N").stream()
                .filter(a -> userId.equals(a.getDrafterId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Approval> getPendingApprovals(String companyId, String userId) {
        // 본인 차례에 와 있는 대기결재('P')인 단계를 수집
        List<String> appIds = approvalStepRepository.findByCompanyIdAndApproverId(companyId, userId).stream()
                .filter(step -> "P".equals(step.getApprovalResult()))
                .filter(step -> "A".equals(step.getApprovalType()) || "G".equals(step.getApprovalType())) // 결재, 합의
                .map(ApprovalStep::getApprovalId)
                .toList();

        return approvalRepository.findAll().stream()
                .filter(a -> companyId.equals(a.getCompanyId()) && "N".equals(a.getDeleteYn()))
                .filter(a -> appIds.contains(a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Approval> getReferencedApprovals(String companyId, String userId) {
        // 본인이 참조자('R')인 결재 문서들을 조회
        List<String> appIds = approvalStepRepository.findByCompanyIdAndApproverId(companyId, userId).stream()
                .filter(step -> "R".equals(step.getApprovalType()))
                .map(ApprovalStep::getApprovalId)
                .toList();

        return approvalRepository.findAll().stream()
                .filter(a -> companyId.equals(a.getCompanyId()) && "N".equals(a.getDeleteYn()))
                .filter(a -> appIds.contains(a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalDetailResponse getApprovalDetails(String companyId, String id) {
        Approval approval = approvalRepository.findById(new ApprovalId(companyId, id))
                .filter(a -> "N".equals(a.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("결재 문서를 찾을 수 없습니다."));

        List<ApprovalStep> steps = approvalStepRepository.findByCompanyIdAndApprovalIdOrderByStepNoAsc(companyId, id);

        ApprovalDetailResponse response = new ApprovalDetailResponse();
        response.setApproval(approval);
        response.setSteps(steps);
        return response;
    }

    @Transactional
    public void processApprovalAction(String companyId, String id, ApprovalActionRequest request, String approverId) {
        Approval approval = approvalRepository.findById(new ApprovalId(companyId, id))
                .filter(a -> "N".equals(a.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("결재 문서를 찾을 수 없습니다."));

        List<ApprovalStep> steps = approvalStepRepository.findByCompanyIdAndApprovalIdOrderByStepNoAsc(companyId, id);

        // 현재 처리 주체 단계 찾기
        ApprovalStep currentStep = steps.stream()
                .filter(step -> approverId.equals(step.getApproverId()) && "P".equals(step.getApprovalResult()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("결재할 수 있는 권한이 없거나 대기 중이 아닙니다."));

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            currentStep.setApprovalResult("A"); // 승인 완료
            currentStep.setActionAt(LocalDateTime.now());
            currentStep.setComments(request.getComments());
            approvalStepRepository.save(currentStep);

            // 다음 결재 단계 찾기
            Optional<ApprovalStep> nextStepOpt = steps.stream()
                    .filter(step -> step.getStepNo() > currentStep.getStepNo())
                    .filter(step -> "A".equals(step.getApprovalType()) || "G".equals(step.getApprovalType()))
                    .findFirst();

            if (nextStepOpt.isPresent()) {
                // 다음 결재자가 있으면 락 해제
                ApprovalStep nextStep = nextStepOpt.get();
                nextStep.setApprovalResult("P");
                approvalStepRepository.save(nextStep);
            } else {
                // 더이상 결재자가 없으면 결재 완결 승인('C') 처리
                approval.setStatus("C");
                approval.setUpdatedBy(approverId);
                approvalRepository.save(approval);

                // 연계 모듈 완결 승인 전파
                propagateFinalConfirmation(companyId, id, approverId);
            }
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            // 반려 처리
            currentStep.setApprovalResult("R"); // 반려
            currentStep.setActionAt(LocalDateTime.now());
            currentStep.setComments(request.getComments());
            approvalStepRepository.save(currentStep);

            // 이후 단계들 전부 종료 대기 처리
            steps.stream()
                    .filter(step -> step.getStepNo() > currentStep.getStepNo())
                    .forEach(step -> {
                        step.setApprovalResult("T");
                        approvalStepRepository.save(step);
                    });

            approval.setStatus("R"); // 반려
            approval.setUpdatedBy(approverId);
            approvalRepository.save(approval);

            // 연계 모듈 반려 전파
            propagateRejection(companyId, id, approverId);
        }
    }

    private void updateLinkedModuleStatus(String companyId, String refModule, String refNo, String approvalId, String status, String operator) {
        if ("PM".equalsIgnoreCase(refModule)) {
            pmRecordRepository.findAll().stream()
                    .filter(pm -> companyId.equals(pm.getCompanyId()) && refNo.equals(pm.getId()))
                    .findFirst().ifPresent(pm -> {
                        pm.setApprovalId(approvalId);
                        pm.setStatus(status);
                        pm.setUpdatedBy(operator);
                        pmRecordRepository.save(pm);
                    });
        } else if ("WO".equalsIgnoreCase(refModule)) {
            workOrderRepository.findAll().stream()
                    .filter(wo -> companyId.equals(wo.getCompanyId()) && refNo.equals(wo.getId()))
                    .findFirst().ifPresent(wo -> {
                        wo.setApprovalId(approvalId);
                        wo.setStatus(status);
                        wo.setUpdatedBy(operator);
                        workOrderRepository.save(wo);
                    });
        } else if ("WP".equalsIgnoreCase(refModule)) {
            workPermitRepository.findAll().stream()
                    .filter(wp -> companyId.equals(wp.getCompanyId()) && refNo.equals(wp.getId()))
                    .findFirst().ifPresent(wp -> {
                        wp.setApprovalId(approvalId);
                        wp.setStatus(status);
                        wp.setUpdatedBy(operator);
                        workPermitRepository.save(wp);
                    });
        }
    }

    private void propagateFinalConfirmation(String companyId, String approvalId, String operator) {
        // PM
        pmRecordRepository.findAll().stream()
                .filter(pm -> companyId.equals(pm.getCompanyId()) && approvalId.equals(pm.getApprovalId()))
                .findFirst().ifPresent(pm -> {
                    pm.setStatus("C");
                    pm.setUpdatedBy(operator);
                    pmRecordRepository.save(pm);

                    // PM 완료에 따른 차기 점검 스케줄 갱신 연동
                    updateCheckCycleSchedule(companyId, pm, operator);
                });

        // WO
        workOrderRepository.findAll().stream()
                .filter(wo -> companyId.equals(wo.getCompanyId()) && approvalId.equals(wo.getApprovalId()))
                .findFirst().ifPresent(wo -> {
                    wo.setStatus("C");
                    wo.setUpdatedBy(operator);
                    workOrderRepository.save(wo);
                });

        // WP
        workPermitRepository.findAll().stream()
                .filter(wp -> companyId.equals(wp.getCompanyId()) && approvalId.equals(wp.getApprovalId()))
                .findFirst().ifPresent(wp -> {
                    wp.setStatus("C");
                    wp.setUpdatedBy(operator);
                    workPermitRepository.save(wp);
                });
    }

    private void propagateRejection(String companyId, String approvalId, String operator) {
        // PM
        pmRecordRepository.findAll().stream()
                .filter(pm -> companyId.equals(pm.getCompanyId()) && approvalId.equals(pm.getApprovalId()))
                .findFirst().ifPresent(pm -> {
                    pm.setStatus("R");
                    pm.setUpdatedBy(operator);
                    pmRecordRepository.save(pm);
                });

        // WO
        workOrderRepository.findAll().stream()
                .filter(wo -> companyId.equals(wo.getCompanyId()) && approvalId.equals(wo.getApprovalId()))
                .findFirst().ifPresent(wo -> {
                    wo.setStatus("R");
                    wo.setUpdatedBy(operator);
                    workOrderRepository.save(wo);
                });

        // WP
        workPermitRepository.findAll().stream()
                .filter(wp -> companyId.equals(wp.getCompanyId()) && approvalId.equals(wp.getApprovalId()))
                .findFirst().ifPresent(wp -> {
                    wp.setStatus("R");
                    wp.setUpdatedBy(operator);
                    workPermitRepository.save(wp);
                });
    }

    private void updateCheckCycleSchedule(String companyId, PmRecord pm, String operator) {
        EquipmentCheckCycleId cycleId = new EquipmentCheckCycleId(companyId, pm.getPlantId(), pm.getEquipmentId(), pm.getCheckTypeCode());
        equipmentCheckCycleRepository.findById(cycleId).ifPresent(cycle -> {
            cycle.setLastCheckDate(pm.getWorkDate());
            cycle.setNextCheckDate(calculateNextDate(pm.getWorkDate(), cycle.getCycleVal(), cycle.getCycleUnit()));
            cycle.setUpdatedBy(operator);
            equipmentCheckCycleRepository.save(cycle);
        });
    }

    private LocalDate calculateNextDate(LocalDate lastDate, int val, String unit) {
        if (lastDate == null) return null;
        return switch (unit.toUpperCase()) {
            case "D" -> lastDate.plusDays(val);
            case "W" -> lastDate.plusWeeks(val);
            case "M" -> lastDate.plusMonths(val);
            case "Y" -> lastDate.plusYears(val);
            default -> lastDate.plusMonths(val);
        };
    }
}
