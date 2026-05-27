package com.cmms.dto;

import java.util.List;

/**
 * 파일첨부 응답 DTO. 내부 경로(storage_path)·체크섬은 노출하지 않는다.
 */
public class FileDto {

    public record FileItemResponse(
            Integer itemNo,
            String originalFileName,
            String fileExtension,
            String mimeType,
            Long fileSize
    ) {}

    public record UploadResponse(
            Long groupNo,
            List<FileItemResponse> items
    ) {}
}
