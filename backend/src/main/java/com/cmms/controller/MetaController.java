package com.cmms.controller;

import com.cmms.security.AppModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 모듈 메타 정보 노출(코드+한글 라벨) — FE 라벨맵 단일 소스.
 * 인증 필요(다른 /api/** 와 동일), 권한 매트릭스는 적용하지 않는다(메타라 모든 로그인 사용자 접근 가능).
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/modules")
    public List<Map<String, String>> getModules() {
        return Arrays.stream(AppModule.values())
                .map(m -> Map.of("code", m.name(), "label", m.label()))
                .toList();
    }
}
