package com.cmms.repository;

import com.cmms.model.PurchaseRequestItem;
import com.cmms.model.PurchaseRequestItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, PurchaseRequestItemId> {

    List<PurchaseRequestItem> findByCompanyIdAndRequestId(String companyId, String requestId);

    void deleteByCompanyIdAndRequestId(String companyId, String requestId);
}
