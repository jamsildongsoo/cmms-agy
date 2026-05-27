package com.cmms.service;

import com.cmms.model.*;
import com.cmms.repository.*;
import com.cmms.security.AppModule;
import com.cmms.util.CodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 회사(Company) 전담 서비스 — MDM(플랜트/부서/사용자/창고/코드)과 분리.
 * 회사 생성은 sysadmin(SYSTEM) 전용이며, 생성 시 표준 롤·권한매트릭스·공통코드를 시드한다.
 * (부서/플랜트/창고는 생성하지 않음 — 이후 관리자가 MDM에서 등록)
 */
@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RoleDetailRepository roleDetailRepository;
    @Autowired
    private CodeGroupRepository codeGroupRepository;
    @Autowired
    private CodeItemRepository codeItemRepository;

    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll().stream()
                .filter(c -> "N".equals(c.getDeleteYn()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Company getCompanyById(String id) {
        return companyRepository.findById(id)
                .filter(c -> "N".equals(c.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));
    }

    /** 회사 생성 + 초기 시드(롤/권한매트릭스/공통코드 복사). */
    @Transactional
    public Company createCompany(Company company, String operator) {
        String companyId = CodeUtil.normalize(company.getId());
        if (companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("이미 존재하는 회사 코드입니다.");
        }
        company.setId(companyId);
        company.setCreatedBy(operator);
        company.setUpdatedBy(operator);
        Company saved = companyRepository.save(company);

        seedRoles(companyId);
        copySystemCommonCodes(companyId);
        return saved;
    }

    @Transactional
    public Company updateCompany(String id, Company req, String operator) {
        Company company = getCompanyById(id);
        company.setName(req.getName());
        company.setBusinessNumber(req.getBusinessNumber());
        company.setEmail(req.getEmail());
        company.setUpdatedBy(operator);
        return companyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(String id, String operator) {
        Company company = getCompanyById(id);
        company.setDeleteYn("Y");
        company.setUpdatedBy(operator);
        companyRepository.save(company);
    }

    // ===== 신규 회사 시드 =====

    private void seedRoles(String companyId) {
        // SYSTEM 역할은 플랫폼 전역 슈퍼관리자 전용 — 회사별로 만들지 않는다.
        String[] roles = {"ADMIN", "MANAGER", "USER"};
        String[] roleNames = {"회사관리자", "중간관리자", "일반사용자"};
        for (int i = 0; i < roles.length; i++) {
            Role role = new Role();
            role.setCompanyId(companyId);
            role.setId(roles[i]);
            role.setRoleName(roleNames[i]);
            role.setMultiPlant("N");
            role.setCreatedBy("SYSTEM");
            role.setUpdatedBy("SYSTEM");
            roleRepository.save(role);
            createDefaultRoleDetails(companyId, roles[i]);
        }
    }

    private void createDefaultRoleDetails(String companyId, String roleId) {
        List<RoleDetail> details = new ArrayList<>();
        for (AppModule m : AppModule.values()) {
            String module = m.name();
            RoleDetail detail = new RoleDetail();
            detail.setCompanyId(companyId);
            detail.setRoleId(roleId);
            detail.setModuleDetail(module);
            switch (roleId) {
                case "ADMIN":
                    // 회사관리자: 전 모듈 전 권한
                    detail.setPermC("Y");
                    detail.setPermR("Y");
                    detail.setPermU("Y");
                    detail.setPermD("Y");
                    detail.setPermA("Y");
                    break;
                case "MANAGER":
                    // 중간관리자: MDM은 조회만, 나머지는 CRUD + 자체확정(A)
                    if (module.equals("MDM")) {
                        detail.setPermC("N");
                        detail.setPermR("Y");
                        detail.setPermU("N");
                        detail.setPermD("N");
                        detail.setPermA("N");
                    } else {
                        detail.setPermC("Y");
                        detail.setPermR("Y");
                        detail.setPermU("Y");
                        detail.setPermD("Y");
                        detail.setPermA("Y");
                    }
                    break;
                case "USER":
                    // 일반사용자: MDM은 조회만, 나머지는 CRUD (자체확정 A 없음 → 결재 상신 필요)
                    if (module.equals("MDM")) {
                        detail.setPermC("N");
                        detail.setPermR("Y");
                        detail.setPermU("N");
                        detail.setPermD("N");
                        detail.setPermA("N");
                    } else {
                        detail.setPermC("Y");
                        detail.setPermR("Y");
                        detail.setPermU("Y");
                        detail.setPermD("Y");
                        detail.setPermA("N");
                    }
                    break;
            }
            details.add(detail);
        }
        roleDetailRepository.saveAll(details);
    }

    /** SYSTEM 테넌트의 공통코드(그룹+아이템)를 신규 회사로 복사. */
    private void copySystemCommonCodes(String companyId) {
        for (CodeGroup sg : codeGroupRepository.findByCompanyIdAndDeleteYn("SYSTEM", "N")) {
            CodeGroup group = new CodeGroup();
            group.setCompanyId(companyId);
            group.setId(sg.getId());
            group.setName(sg.getName());
            group.setSystemUseYn(sg.getSystemUseYn());
            group.setCreatedBy("SYSTEM");
            group.setUpdatedBy("SYSTEM");
            codeGroupRepository.save(group);

            for (CodeItem si : codeItemRepository.findByCompanyIdAndGroupIdOrderBySortOrderAsc("SYSTEM", sg.getId())) {
                CodeItem item = new CodeItem();
                item.setCompanyId(companyId);
                item.setGroupId(si.getGroupId());
                item.setId(si.getId());
                item.setName(si.getName());
                item.setLegalInspectYn(si.getLegalInspectYn());
                item.setSortOrder(si.getSortOrder());
                codeItemRepository.save(item);
            }
        }
    }
}
