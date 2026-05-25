package com.cmms.dto;

import com.cmms.model.Equipment;
import com.cmms.model.EquipmentCheckCycle;
import com.cmms.model.EquipmentCheckItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MasterDto {

    @Getter
    @Setter
    public static class EquipmentSaveRequest {
        private Equipment equipment;
        private List<EquipmentCheckItem> checkItems;
        private List<EquipmentCheckCycle> checkCycles;
    }
}
