package com.cmms.repository;

import com.cmms.model.PurchaseRequest;
import com.cmms.model.PurchaseRequestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, PurchaseRequestId> {

    Optional<PurchaseRequest> findByCompanyIdAndId(String companyId, String id);

    List<PurchaseRequest> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);

    List<PurchaseRequest> findByCompanyIdAndPlantIdAndDeleteYn(String companyId, String plantId, String deleteYn);
}
