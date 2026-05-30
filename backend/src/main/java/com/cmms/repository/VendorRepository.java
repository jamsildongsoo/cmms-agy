package com.cmms.repository;

import com.cmms.model.Vendor;
import com.cmms.model.VendorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, VendorId> {
    List<Vendor> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);

    Optional<Vendor> findByCompanyIdAndId(String companyId, String id);
}
