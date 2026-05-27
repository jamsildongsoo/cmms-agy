package com.cmms.service;

import com.cmms.constant.ApprovalStepType;
import com.cmms.constant.DocStatus;
import com.cmms.constant.SeqModule;
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

        List<ApprovalStep> steps = request.getSteps();
        // 결재/합의 대상이 한 명이라도 있으면 진행(라우팅), 없으면 임시저장
        boolean hasApprover = steps != null && steps.stream().anyMatch(this::isApprovalOrAgreement);

        boolean isNew = approval.getId() == null || approval.getId().trim().isEmpty();
        String appNo;
        if (isNew) {
            appNo = sequenceService.generateNextNo(companyId, SeqModule.APR.code(), "DEPT_ROOT");
            approval.setId(appNo);
            approval.setDrafterId(operator);
            approval.setCreatedBy(operator);
        } else {
            // 재상신: 임시저장 상태에서만 허용. 기존 단계는 제거 후 재생성한다.
            appNo = approval.getId();
            Approval existing = approvalRepository.findById(new ApprovalId(companyId, appNo))
                    .filter(a -> "N".equals(a.getDeleteYn()))
                    .orElseThrow(() -> new IllegalArgumentException("결재 문서를 찾을 수 없습니다."));
            if (!DocStatus.TEMP.code().equals(existing.getStatus())) {
                throw new IllegalArgumentException("임시저장 상태에서만 재상신할 수 있습니다.");
            }
            approval.setDrafterId(existing.getDrafterId());
            approval.setCreatedBy(existing.getCreatedBy());
            approvalStepRepository.deleteByCompanyIdAndApprovalId(companyId, appNo);
            approvalStepRepository.flush();
        }

        approval.setStatus(hasApprover ? DocStatus.IN_PROGRESS.code() : DocStatus.TEMP.code());
        approval.setUpdatedBy(operator);
        approval.setDeleteYn("N");

        Approval savedApproval = approvalRepository.save(approval);

        // 단계 저장 (steps/hasApprover는 위에서 계산됨)
        // 0. 기안자 자동 추가 (0순번)
        ApprovalStep draftStep = new ApprovalStep();
        draftStep.setCompanyId(companyId);
        draftStep.setApprovalId(appNo);
        draftStep.setStepNo(0);
        draftStep.setApproverId(operator);
        draftStep.setApprovalType(ApprovalStepType.DRAFT.code());
        draftStep.setApprovalResult("Y"); // 기안 = 처리완료
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

                // 결재선 단계는 모두 대기(빈칸/NULL). '현재 차례'는 저장하지 않고 순서로 계산한다.
                step.setApprovalResult(null);

                approvalStepRepository.save(step);
            }
        }

        // 연계 모듈 상태값 업데이트 — 실제 라우팅(결재자 있음)일 때만 '결재중' 처리
        String refNo = request.getRefNo();
        String refModule = request.getRefModule();
        if (hasApprover && refNo != null && refModule != null) {
            updateLinkedModuleStatus(companyId, refModule, refNo, appNo, DocStatus.IN_PROGRESS.code(), operator);
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
        // 본인이 결재/합의 대상이고 미처리(NULL)이며, 그 문서의 '현재 차례'가 본인인 건을 수집
        List<String> appIds = approvalStepRepository.findByCompanyIdAndApproverId(companyId, userId).stream()
                .filter(step -> step.getApprovalResult() == null)
                .filter(this::isApprovalOrAgreement) // 결재, 합의
                .map(ApprovalStep::getApprovalId)
                .distinct()
                .filter(appId -> isCurrentTurn(companyId, appId, userId))
                .toList();

        return approvalRepository.findAll().stream()
                .filter(a -> companyId.equals(a.getCompanyId()) && "N".equals(a.getDeleteYn()))
                .filter(a -> DocStatus.IN_PROGRESS.code().equals(a.getStatus())) // 진행중 문서만(반려/완결 제외)
                .filter(a -> appIds.contains(a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Approval> getReferencedApprovals(String companyId, String userId) {
        // 본인이 참조자('R')인 결재 문서들을 조회
        List<String> appIds = approvalStepRepository.findByCompanyIdAndApproverId(companyId, userId).stream()
                .filter(step -> ApprovalStepType.REFERENCE.code().equals(step.getApprovalType()))
                .map(ApprovalStep::getApprovalId)
                .toList();

        return approvalRepository.findAll().stream()
                .filter(a -> companyId.equals(a.getCompanyId()) && "N".equals(a.getDeleteYn()))
                .filter(a -> appIds.contains(a.getId()))
                .toList();
    }

    /** 결재/반려함: 본인이 결재/합의자로서 이미 처리(승인 Y 또는 반려 N)한 문서. 기안(D)·참조(R) 제외. */
    public List<Approval> getProcessedApprovals(String companyId, String userId) {
        List<String> appIds = approvalStepRepository.findByCompanyIdAndApproverId(companyId, userId).stream()
                .filter(step -> step.getApprovalResult() != null) // 처리됨(Y 승인 / N 반려)
                .filter(this::isApprovalOrAgreement)               // 결재/합의자로서만
                .map(ApprovalStep::getApprovalId)
                .distinct()
                .toList();

        return approvalRepository.findByCompanyIdAndDeleteYn(companyId, "N").stream()
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

        if (!DocStatus.IN_PROGRESS.code().equals(approval.getStatus())) {
            throw new IllegalArgumentException("이미 종료된 결재 문서입니다.");
        }

        List<ApprovalStep> steps = approvalStepRepository.findByCompanyIdAndApprovalIdOrderByStepNoAsc(companyId, id);

        // 현재 차례 = 가장 앞선(stepNo 최소) 미처리(NULL) 결재/합의 단계. 그 단계 결재자가 본인이어야 한다.
        ApprovalStep currentStep = steps.stream()
                .filter(this::isApprovalOrAgreement)
                .filter(step -> step.getApprovalResult() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("결재 대기 중인 단계가 없습니다."));
        if (!approverId.equals(currentStep.getApproverId())) {
            throw new IllegalArgumentException("결재할 수 있는 권한이 없거나 대기 중이 아닙니다.");
        }

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            currentStep.setApprovalResult("Y"); // 승인
            currentStep.setActionAt(LocalDateTime.now());
            currentStep.setComments(request.getComments());
            approvalStepRepository.save(currentStep);

            // 남은 미처리(NULL) 결재/합의 단계가 없으면 완결 확정
            boolean hasNextPending = steps.stream()
                    .filter(this::isApprovalOrAgreement)
                    .anyMatch(step -> step.getApprovalResult() == null);

            if (!hasNextPending) {
                approval.setStatus(DocStatus.CONFIRMED.code());
                approval.setUpdatedBy(approverId);
                approvalRepository.save(approval);

                // 연계 모듈 완결 승인 전파
                propagateFinalConfirmation(companyId, id, approverId);
            }
            // 다음 단계 별도 활성화 불필요 — 다음 미처리(NULL) 단계가 자동으로 '현재 차례'가 됨
        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            currentStep.setApprovalResult("N"); // 반려
            currentStep.setActionAt(LocalDateTime.now());
            currentStep.setComments(request.getComments());
            approvalStepRepository.save(currentStep);

            // 이후 단계는 대기(NULL)로 남기고, 문서를 반려로 종료(문서 상태가 추가 진행을 차단)
            approval.setStatus(DocStatus.REJECTED.code());
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
                    pm.setStatus(DocStatus.CONFIRMED.code());
                    pm.setUpdatedBy(operator);
                    pmRecordRepository.save(pm);

                    // PM 완료에 따른 차기 점검 스케줄 갱신 연동
                    updateCheckCycleSchedule(companyId, pm, operator);
                });

        // WO
        workOrderRepository.findAll().stream()
                .filter(wo -> companyId.equals(wo.getCompanyId()) && approvalId.equals(wo.getApprovalId()))
                .findFirst().ifPresent(wo -> {
                    wo.setStatus(DocStatus.CONFIRMED.code());
                    wo.setUpdatedBy(operator);
                    workOrderRepository.save(wo);
                });

        // WP
        workPermitRepository.findAll().stream()
                .filter(wp -> companyId.equals(wp.getCompanyId()) && approvalId.equals(wp.getApprovalId()))
                .findFirst().ifPresent(wp -> {
                    wp.setStatus(DocStatus.CONFIRMED.code());
                    wp.setUpdatedBy(operator);
                    workPermitRepository.save(wp);
                });
    }

    private void propagateRejection(String companyId, String approvalId, String operator) {
        // PM
        pmRecordRepository.findAll().stream()
                .filter(pm -> companyId.equals(pm.getCompanyId()) && approvalId.equals(pm.getApprovalId()))
                .findFirst().ifPresent(pm -> {
                    pm.setStatus(DocStatus.REJECTED.code());
                    pm.setUpdatedBy(operator);
                    pmRecordRepository.save(pm);
                });

        // WO
        workOrderRepository.findAll().stream()
                .filter(wo -> companyId.equals(wo.getCompanyId()) && approvalId.equals(wo.getApprovalId()))
                .findFirst().ifPresent(wo -> {
                    wo.setStatus(DocStatus.REJECTED.code());
                    wo.setUpdatedBy(operator);
                    workOrderRepository.save(wo);
                });

        // WP
        workPermitRepository.findAll().stream()
                .filter(wp -> companyId.equals(wp.getCompanyId()) && approvalId.equals(wp.getApprovalId()))
                .findFirst().ifPresent(wp -> {
                    wp.setStatus(DocStatus.REJECTED.code());
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

    // 결재/합의 단계인지
    private boolean isApprovalOrAgreement(ApprovalStep s) {
        return ApprovalStepType.APPROVAL.code().equals(s.getApprovalType())
                || ApprovalStepType.AGREEMENT.code().equals(s.getApprovalType());
    }

    // 해당 문서의 '현재 차례'(가장 앞선 미처리 결재/합의 단계)의 결재자가 userId인가
    private boolean isCurrentTurn(String companyId, String approvalId, String userId) {
        return approvalStepRepository.findByCompanyIdAndApprovalIdOrderByStepNoAsc(companyId, approvalId).stream()
                .filter(this::isApprovalOrAgreement)
                .filter(step -> step.getApprovalResult() == null)
                .findFirst()
                .map(step -> userId.equals(step.getApproverId()))
                .orElse(false);
    }
}
