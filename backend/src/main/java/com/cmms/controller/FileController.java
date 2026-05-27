package com.cmms.controller;

import com.cmms.dto.FileDto.FileItemResponse;
import com.cmms.dto.FileDto.UploadResponse;
import com.cmms.security.UserPrincipal;
import com.cmms.service.FileStorageService;
import com.cmms.service.FileStorageService.DownloadResult;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 파일 첨부 API. 모든 경로 인증 필요(테넌트 격리는 서비스에서 companyId 기준).
 * 첨부 권한은 우선 인증만(후속: 대상 모듈 권한 연계).
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileService;

    public FileController(FileStorageService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "refModule", required = false) String refModule,
            @RequestParam(value = "refNo", required = false) String refNo,
            @RequestParam(value = "groupNo", required = false) Long groupNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.upload(
                principal.getCompanyId(), principal.getUsername(), refModule, refNo, groupNo, files));
    }

    @GetMapping("/{groupNo}")
    public ResponseEntity<List<FileItemResponse>> list(
            @PathVariable Long groupNo, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fileService.list(principal.getCompanyId(), groupNo));
    }

    @GetMapping("/{groupNo}/{itemNo}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long groupNo, @PathVariable Integer itemNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        DownloadResult dr = fileService.download(principal.getCompanyId(), groupNo, itemNo);
        String filename = URLEncoder.encode(dr.item().getOriginalFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        MediaType contentType = dr.item().getMimeType() != null
                ? safeMediaType(dr.item().getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(contentType)
                .contentLength(dr.item().getFileSize())
                .body(new InputStreamResource(dr.stream()));
    }

    @DeleteMapping("/{groupNo}/{itemNo}")
    public ResponseEntity<Void> delete(
            @PathVariable Long groupNo, @PathVariable Integer itemNo,
            @AuthenticationPrincipal UserPrincipal principal) {
        fileService.delete(principal.getCompanyId(), groupNo, itemNo);
        return ResponseEntity.ok().build();
    }

    private MediaType safeMediaType(String mime) {
        try {
            return MediaType.parseMediaType(mime);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
