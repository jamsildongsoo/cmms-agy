package com.cmms.service;

import com.cmms.model.*;
import com.cmms.repository.*;
import com.cmms.util.CodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MdmService {

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleDetailRepository roleDetailRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CodeGroupRepository codeGroupRepository;

    @Autowired
    private CodeItemRepository codeItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 회사(Company) CRUD는 CompanyService로 분리.

    // ==========================================
    // 2. 플랜트 (Plant)
    // ==========================================
    @Transactional(readOnly = true)
    public List<Plant> getPlantsByCompany(String companyId) {
        return plantRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional
    public Plant savePlant(String companyId, Plant plant, String operator) {
        plant.setId(CodeUtil.normalize(plant.getId()));
        PlantId id = new PlantId(companyId, plant.getId());
        if (plantRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 플랜트 아이디입니다.");
        }
        plant.setCompanyId(companyId);
        plant.setCreatedBy(operator);
        plant.setUpdatedBy(operator);
        return plantRepository.save(plant);
    }

    @Transactional
    public Plant updatePlant(String companyId, String id, Plant req, String operator) {
        Plant plant = plantRepository.findById(new PlantId(companyId, id))
                .filter(p -> "N".equals(p.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("플랜트를 찾을 수 없습니다."));
        plant.setName(req.getName());
        plant.setUpdatedBy(operator);
        return plantRepository.save(plant);
    }

    @Transactional
    public void deletePlant(String companyId, String id, String operator) {
        Plant plant = plantRepository.findById(new PlantId(companyId, id))
                .filter(p -> "N".equals(p.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("플랜트를 찾을 수 없습니다."));
        plant.setDeleteYn("Y");
        plant.setUpdatedBy(operator);
        plantRepository.save(plant);
    }

    // ==========================================
    // 3. 부서 (Department)
    // ==========================================
    @Transactional(readOnly = true)
    public List<Department> getDepartmentsByCompany(String companyId) {
        return departmentRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional
    public Department saveDepartment(String companyId, Department dept, String operator) {
        dept.setId(CodeUtil.normalize(dept.getId()));
        if (dept.getParentId() != null && !dept.getParentId().trim().isEmpty()) {
            dept.setParentId(CodeUtil.normalize(dept.getParentId()));
        }
        DepartmentId id = new DepartmentId(companyId, dept.getId());
        if (departmentRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 부서 아이디입니다.");
        }
        dept.setCompanyId(companyId);
        dept.setCreatedBy(operator);
        dept.setUpdatedBy(operator);
        return departmentRepository.save(dept);
    }

    @Transactional
    public Department updateDepartment(String companyId, String id, Department req, String operator) {
        Department dept = departmentRepository.findById(new DepartmentId(companyId, id))
                .filter(d -> "N".equals(d.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다."));
        dept.setName(req.getName());
        dept.setParentId(req.getParentId());
        dept.setUpdatedBy(operator);
        return departmentRepository.save(dept);
    }

    @Transactional
    public void deleteDepartment(String companyId, String id, String operator) {
        Department dept = departmentRepository.findById(new DepartmentId(companyId, id))
                .filter(d -> "N".equals(d.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다."));
        dept.setDeleteYn("Y");
        dept.setUpdatedBy(operator);
        departmentRepository.save(dept);
    }

    // ==========================================
    // 4. 권한 (Role & RoleDetail)
    // ==========================================
    @Transactional(readOnly = true)
    public List<Role> getRolesByCompany(String companyId) {
        return roleRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional(readOnly = true)
    public List<RoleDetail> getRoleDetails(String companyId, String roleId) {
        return roleDetailRepository.findByCompanyIdAndRoleId(companyId, roleId);
    }

    @Transactional
    public Role saveRole(String companyId, Role role, String operator) {
        role.setId(CodeUtil.normalize(role.getId()));
        RoleId id = new RoleId(companyId, role.getId());
        if (roleRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 권한 그룹 아이디입니다.");
        }
        role.setCompanyId(companyId);
        role.setCreatedBy(operator);
        role.setUpdatedBy(operator);
        
        // 부모 레코드(Role)를 먼저 즉시 물리적으로 저장하여 외래키 제약조건 충돌 방지
        Role savedRole = roleRepository.saveAndFlush(role);

        // 기본 권한 상세 매트릭스 목록 자동 셋업 (모듈 정의는 AppModule 단일 소스)
        for (com.cmms.security.AppModule m : com.cmms.security.AppModule.values()) {
            RoleDetail detail = new RoleDetail();
            detail.setCompanyId(companyId);
            detail.setRoleId(savedRole.getId());
            detail.setModuleDetail(m.name());
            roleDetailRepository.save(detail);
        }
        roleDetailRepository.flush();

        return savedRole;
    }

    @Transactional
    public void saveRoleDetails(String companyId, String roleId, List<RoleDetail> details) {
        for (RoleDetail detail : details) {
            RoleDetailId id = new RoleDetailId(companyId, roleId, detail.getModuleDetail());
            RoleDetail existing = roleDetailRepository.findById(id).orElse(null);
            if (existing != null) {
                existing.setPermC(detail.getPermC());
                existing.setPermR(detail.getPermR());
                existing.setPermU(detail.getPermU());
                existing.setPermD(detail.getPermD());
                existing.setPermA(detail.getPermA());
                roleDetailRepository.save(existing);
            } else {
                detail.setCompanyId(companyId);
                detail.setRoleId(roleId);
                roleDetailRepository.save(detail);
            }
        }
    }

    @Transactional
    public void deleteRole(String companyId, String id, String operator) {
        Role role = roleRepository.findById(new RoleId(companyId, id))
                .filter(r -> "N".equals(r.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("권한 그룹을 찾을 수 없습니다."));
        role.setDeleteYn("Y");
        role.setUpdatedBy(operator);
        roleRepository.save(role);
    }

    // ==========================================
    // 5. 사용자 (User)
    // ==========================================
    @Transactional(readOnly = true)
    public List<User> getUsersByCompany(String companyId) {
        return userRepository.findAll().stream()
                .filter(u -> companyId.equals(u.getCompanyId()) && "N".equals(u.getDeleteYn()))
                .toList();
    }

    @Transactional
    public User saveUser(String companyId, User user, String operator) {
        UserId id = new UserId(companyId, user.getId());
        if (userRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 사용자 아이디입니다.");
        }
        user.setRoleId(CodeUtil.normalizeOptional(user.getRoleId()));
        user.setDepartmentId(CodeUtil.normalizeOptional(user.getDepartmentId()));
        user.setCompanyId(companyId);
        user.setPasswordHash(passwordEncoder.encode("1234")); // 신규 등록 사용자 임시 비밀번호 디폴트 1234 지정
        user.setCreatedBy(operator);
        user.setUpdatedBy(operator);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(String companyId, String id, User req, String operator) {
        User user = userRepository.findByCompanyIdAndId(companyId, id)
                .filter(u -> "N".equals(u.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setName(req.getName());
        user.setDepartmentId(CodeUtil.normalizeOptional(req.getDepartmentId()));
        user.setRoleId(CodeUtil.normalizeOptional(req.getRoleId()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPosition(req.getPosition());
        user.setTitle(req.getTitle());
        user.setUseYn(req.getUseYn());
        // 지정 플랜트 (관리자 지정 — 로그인 시 자동매핑보다 우선)
        user.setLastLoginPlantId(req.getLastLoginPlantId());
        user.setUpdatedBy(operator);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String companyId, String id, String operator) {
        User user = userRepository.findByCompanyIdAndId(companyId, id)
                .filter(u -> "N".equals(u.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setDeleteYn("Y");
        user.setUpdatedBy(operator);
        userRepository.save(user);
    }

    // ==========================================
    // 6. 저장소 (Warehouse)
    // ==========================================
    @Transactional(readOnly = true)
    public List<Warehouse> getWarehousesByCompany(String companyId) {
        return warehouseRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional
    public Warehouse saveWarehouse(String companyId, Warehouse warehouse, String operator) {
        warehouse.setId(CodeUtil.normalize(warehouse.getId()));
        WarehouseId id = new WarehouseId(companyId, warehouse.getId());
        if (warehouseRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 저장소 아이디입니다.");
        }
        warehouse.setCompanyId(companyId);
        warehouse.setCreatedBy(operator);
        warehouse.setUpdatedBy(operator);
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public Warehouse updateWarehouse(String companyId, String id, Warehouse req, String operator) {
        Warehouse warehouse = warehouseRepository.findById(new WarehouseId(companyId, id))
                .filter(w -> "N".equals(w.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));
        warehouse.setName(req.getName());
        warehouse.setPlantId(req.getPlantId());  // nullable = 공통부문
        warehouse.setUpdatedBy(operator);
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public void deleteWarehouse(String companyId, String id, String operator) {
        Warehouse warehouse = warehouseRepository.findById(new WarehouseId(companyId, id))
                .filter(w -> "N".equals(w.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));
        warehouse.setDeleteYn("Y");
        warehouse.setUpdatedBy(operator);
        warehouseRepository.save(warehouse);
    }

    // ==========================================
    // 7. 공통코드 (CodeGroup & CodeItem)
    // ==========================================
    @Transactional(readOnly = true)
    public List<CodeGroup> getCodeGroupsByCompany(String companyId) {
        return codeGroupRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional
    public CodeGroup saveCodeGroup(String companyId, CodeGroup group, String operator) {
        group.setId(CodeUtil.normalize(group.getId()));
        CodeGroupId id = new CodeGroupId(companyId, group.getId());
        if (codeGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 코드그룹 아이디입니다.");
        }
        group.setCompanyId(companyId);
        group.setCreatedBy(operator);
        group.setUpdatedBy(operator);
        return codeGroupRepository.save(group);
    }

    @Transactional
    public CodeGroup updateCodeGroup(String companyId, String id, CodeGroup req, String operator) {
        CodeGroup group = codeGroupRepository.findById(new CodeGroupId(companyId, id))
                .filter(g -> "N".equals(g.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("코드그룹을 찾을 수 없습니다."));
        group.setName(req.getName());
        group.setUpdatedBy(operator);
        return codeGroupRepository.save(group);
    }

    @Transactional
    public void deleteCodeGroup(String companyId, String id, String operator) {
        CodeGroup group = codeGroupRepository.findById(new CodeGroupId(companyId, id))
                .filter(g -> "N".equals(g.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("코드그룹을 찾을 수 없습니다."));
        if ("Y".equals(group.getSystemUseYn())) {
            throw new IllegalArgumentException("시스템 예약 공통코드는 삭제할 수 없습니다.");
        }
        group.setDeleteYn("Y");
        group.setUpdatedBy(operator);
        codeGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<CodeItem> getCodeItems(String companyId, String groupId) {
        return codeItemRepository.findByCompanyIdAndGroupIdOrderBySortOrderAsc(companyId, groupId);
    }

    @Transactional
    public CodeItem saveCodeItem(String companyId, String groupId, CodeItem item) {
        groupId = CodeUtil.normalize(groupId);
        item.setId(CodeUtil.normalize(item.getId()));
        CodeItemId id = new CodeItemId(companyId, groupId, item.getId());
        if (codeItemRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 존재하는 코드아이템 아이디입니다.");
        }
        item.setCompanyId(companyId);
        item.setGroupId(groupId);
        return codeItemRepository.save(item);
    }

    @Transactional
    public CodeItem updateCodeItem(String companyId, String groupId, String id, CodeItem req) {
        CodeItem item = codeItemRepository.findById(new CodeItemId(companyId, groupId, id))
                .orElseThrow(() -> new IllegalArgumentException("코드아이템을 찾을 수 없습니다."));
        item.setName(req.getName());
        item.setLegalInspectYn(req.getLegalInspectYn());
        item.setSortOrder(req.getSortOrder());
        return codeItemRepository.save(item);
    }

    @Transactional
    public void deleteCodeItem(String companyId, String groupId, String id) {
        CodeItem item = codeItemRepository.findById(new CodeItemId(companyId, groupId, id))
                .orElseThrow(() -> new IllegalArgumentException("코드아이템을 찾을 수 없습니다."));
        codeItemRepository.delete(item);
    }
}
