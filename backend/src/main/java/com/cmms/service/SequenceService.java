package com.cmms.service;

import com.cmms.model.SequenceGenerator;
import com.cmms.model.SequenceGeneratorId;
import com.cmms.repository.SequenceGeneratorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SequenceService {

    @Autowired
    private SequenceGeneratorRepository sequenceGeneratorRepository;

    @Transactional
    public synchronized String generateNextNo(String companyId, String refModule, String departmentId) {
        String deptId = departmentId == null || departmentId.trim().isEmpty() ? "DEPT_ROOT" : departmentId;
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        SequenceGeneratorId id = new SequenceGeneratorId(companyId, refModule, deptId, yearMonth);
        SequenceGenerator generator = sequenceGeneratorRepository.findById(id).orElseGet(() -> {
            SequenceGenerator newGen = new SequenceGenerator();
            newGen.setCompanyId(companyId);
            newGen.setRefModule(refModule);
            newGen.setDepartmentId(deptId);
            newGen.setYearMonth(yearMonth);
            newGen.setLastSeq(0);
            newGen.setCreatedBy("SYSTEM");
            newGen.setUpdatedBy("SYSTEM");
            return newGen;
        });

        int nextSeq = generator.getLastSeq() + 1;
        generator.setLastSeq(nextSeq);
        generator.setUpdatedBy("SYSTEM");
        sequenceGeneratorRepository.save(generator);

        return String.format("%s-%s-%s-%04d", refModule, deptId, yearMonth, nextSeq);
    }
}
