package com.cmms.dto;

import com.cmms.model.PmRecord;
import com.cmms.model.PmRecordItem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

public class PmDto {

    @Getter
    @Setter
    public static class PmSaveRequest {
        private PmRecord pmRecord;
        private List<PmRecordItem> checkItems;
    }

    @Getter
    @Setter
    public static class PmScheduleResponse {
        private String equipmentId;
        private String equipmentName;
        private String plantId;
        private String checkTypeCode;
        private String checkTypeName;
        private Integer cycleVal;
        private String cycleUnit;
        private LocalDate lastCheckDate;
        private LocalDate nextCheckDate;
    }
}
