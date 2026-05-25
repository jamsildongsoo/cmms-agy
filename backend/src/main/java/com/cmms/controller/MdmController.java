package com.cmms.controller;

import com.cmms.model.*;
import com.cmms.security.UserPrincipal;
import com.cmms.service.MdmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mdm")
public class MdmController {

    @Autowired
    private MdmService mdmService;

    // ==========================================
    // 1. 회사 (Company)
    // ==========================================
    @GetMapping("/companies")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(mdmService.getAllCompanies());
    }

    @GetMapping("/companies/{id}")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResponseEntity<Company> getCompanyById(@PathVariable String id) {
        return ResponseEntity.ok(mdmService.getCompanyById(id));
    }

    @PostMapping("/companies")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResponseEntity<Company> createCompany(@RequestBody Company company, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveCompany(company, principal.getUsername()));
    }

    @PutMapping("/companies/{id}")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResponseEntity<Company> updateCompany(@PathVariable String id, @RequestBody Company company, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updateCompany(id, company, principal.getUsername()));
    }

    @DeleteMapping("/companies/{id}")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResponseEntity<Void> deleteCompany(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteCompany(id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 2. 플랜트 (Plant)
    // ==========================================
    @GetMapping("/plants")
    public ResponseEntity<List<Plant>> getPlants(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getPlantsByCompany(principal.getCompanyId()));
    }

    @PostMapping("/plants")
    public ResponseEntity<Plant> createPlant(@RequestBody Plant plant, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.savePlant(principal.getCompanyId(), plant, principal.getUsername()));
    }

    @PutMapping("/plants/{id}")
    public ResponseEntity<Plant> updatePlant(@PathVariable String id, @RequestBody Plant plant, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updatePlant(principal.getCompanyId(), id, plant, principal.getUsername()));
    }

    @DeleteMapping("/plants/{id}")
    public ResponseEntity<Void> deletePlant(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deletePlant(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 3. 부서 (Department)
    // ==========================================
    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getDepartmentsByCompany(principal.getCompanyId()));
    }

    @PostMapping("/departments")
    public ResponseEntity<Department> createDepartment(@RequestBody Department dept, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveDepartment(principal.getCompanyId(), dept, principal.getUsername()));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable String id, @RequestBody Department dept, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updateDepartment(principal.getCompanyId(), id, dept, principal.getUsername()));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteDepartment(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 4. 권한 그룹 (Role)
    // ==========================================
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoles(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getRolesByCompany(principal.getCompanyId()));
    }

    @GetMapping("/roles/{roleId}/details")
    public ResponseEntity<List<RoleDetail>> getRoleDetails(@PathVariable String roleId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getRoleDetails(principal.getCompanyId(), roleId));
    }

    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody Role role, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveRole(principal.getCompanyId(), role, principal.getUsername()));
    }

    @PostMapping("/roles/{roleId}/details")
    public ResponseEntity<Void> saveRoleDetails(@PathVariable String roleId, @RequestBody List<RoleDetail> details, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.saveRoleDetails(principal.getCompanyId(), roleId, details);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteRole(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 5. 사용자 (User)
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getUsersByCompany(principal.getCompanyId()));
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveUser(principal.getCompanyId(), user, principal.getUsername()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User user, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updateUser(principal.getCompanyId(), id, user, principal.getUsername()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteUser(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 6. 저장소 (Warehouse)
    // ==========================================
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getWarehousesByCompany(principal.getCompanyId()));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody Warehouse warehouse, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveWarehouse(principal.getCompanyId(), warehouse, principal.getUsername()));
    }

    @PutMapping("/warehouses/{id}")
    public ResponseEntity<Warehouse> updateWarehouse(@PathVariable String id, @RequestBody Warehouse warehouse, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updateWarehouse(principal.getCompanyId(), id, warehouse, principal.getUsername()));
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteWarehouse(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    // ==========================================
    // 7. 공통코드 그룹 & 아이템
    // ==========================================
    @GetMapping("/code-groups")
    public ResponseEntity<List<CodeGroup>> getCodeGroups(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getCodeGroupsByCompany(principal.getCompanyId()));
    }

    @PostMapping("/code-groups")
    public ResponseEntity<CodeGroup> createCodeGroup(@RequestBody CodeGroup group, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveCodeGroup(principal.getCompanyId(), group, principal.getUsername()));
    }

    @PutMapping("/code-groups/{id}")
    public ResponseEntity<CodeGroup> updateCodeGroup(@PathVariable String id, @RequestBody CodeGroup group, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updateCodeGroup(principal.getCompanyId(), id, group, principal.getUsername()));
    }

    @DeleteMapping("/code-groups/{id}")
    public ResponseEntity<Void> deleteCodeGroup(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteCodeGroup(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/code-groups/{groupId}/items")
    public ResponseEntity<List<CodeItem>> getCodeItems(@PathVariable String groupId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.getCodeItems(principal.getCompanyId(), groupId));
    }

    @PostMapping("/code-groups/{groupId}/items")
    public ResponseEntity<CodeItem> createCodeItem(@PathVariable String groupId, @RequestBody CodeItem item, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.saveCodeItem(principal.getCompanyId(), groupId, item));
    }

    @PutMapping("/code-groups/{groupId}/items/{id}")
    public ResponseEntity<CodeItem> updateCodeItem(@PathVariable String groupId, @PathVariable String id, @RequestBody CodeItem item, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(mdmService.updateCodeItem(principal.getCompanyId(), groupId, id, item));
    }

    @DeleteMapping("/code-groups/{groupId}/items/{id}")
    public ResponseEntity<Void> deleteCodeItem(@PathVariable String groupId, @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        mdmService.deleteCodeItem(principal.getCompanyId(), groupId, id);
        return ResponseEntity.ok().build();
    }
}
