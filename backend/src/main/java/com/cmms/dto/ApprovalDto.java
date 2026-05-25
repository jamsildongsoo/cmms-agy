package com.cmms.dto;

import com.cmms.model.Approval;
import com.cmms.model.ApprovalStep;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ApprovalDto {

    @Getter
    @Setter
    public static class ApprovalSubmitRequest {
        private Approval approval;
        private List<ApprovalStep> steps;
        private String refNo;
        private String refModule;
    }

    @Getter
    @Setter
    public static class ApprovalActionRequest {
        private String comments;
        private String action; // APPROVE or REJECT
    }

    @Getter
    @Setter
    public static class ApprovalDetailResponse {
        private Approval approval;
        private List<ApprovalStep> steps;
    }
}
