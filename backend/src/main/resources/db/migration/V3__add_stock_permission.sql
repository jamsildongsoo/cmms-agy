-- 재고처리(STOCK) 권한 모듈 신설에 따른 기존 role_detail 백필.
-- 기존 롤의 STOCK 권한은 INVENTORY 정책을 그대로 따른다(없으면 미생성, SYSTEM은 매트릭스 미사용).
INSERT INTO role_detail (company_id, role_id, module_detail, perm_c, perm_r, perm_u, perm_d, perm_a)
SELECT rd.company_id, rd.role_id, 'STOCK', rd.perm_c, rd.perm_r, rd.perm_u, rd.perm_d, rd.perm_a
FROM role_detail rd
WHERE rd.module_detail = 'INVENTORY'
  AND NOT EXISTS (
      SELECT 1 FROM role_detail x
      WHERE x.company_id = rd.company_id
        AND x.role_id = rd.role_id
        AND x.module_detail = 'STOCK'
  );
