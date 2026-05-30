package com.cmms.service;

import com.cmms.model.Vendor;
import com.cmms.model.VendorId;
import com.cmms.repository.VendorRepository;
import com.cmms.util.CodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<Vendor> getVendors(String companyId) {
        return vendorRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional
    public Vendor saveVendor(String companyId, Vendor vendor, String operator) {
        vendor.setId(CodeUtil.normalize(vendor.getId()));
        VendorId id = new VendorId(companyId, vendor.getId());
        if (vendorRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 벤더 아이디입니다.");
        }
        vendor.setCompanyId(companyId);
        vendor.setCreatedBy(operator);
        vendor.setUpdatedBy(operator);
        return vendorRepository.save(vendor);
    }

    @Transactional
    public Vendor updateVendor(String companyId, String id, Vendor req, String operator) {
        Vendor vendor = vendorRepository.findById(new VendorId(companyId, id))
                .filter(v -> "N".equals(v.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("벤더를 찾을 수 없습니다."));
        vendor.setName(req.getName());
        vendor.setBizNo(req.getBizNo());
        vendor.setContact(req.getContact());
        vendor.setManager(req.getManager());
        vendor.setRemarks(req.getRemarks());
        vendor.setUpdatedBy(operator);
        return vendorRepository.save(vendor);
    }

    @Transactional
    public void deleteVendor(String companyId, String id, String operator) {
        Vendor vendor = vendorRepository.findById(new VendorId(companyId, id))
                .filter(v -> "N".equals(v.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("벤더를 찾을 수 없습니다."));
        vendor.setDeleteYn("Y");
        vendor.setUpdatedBy(operator);
        vendorRepository.save(vendor);
    }
}
