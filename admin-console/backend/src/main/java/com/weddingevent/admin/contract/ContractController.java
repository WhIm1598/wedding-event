package com.weddingevent.admin.contract;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.contract.ContractService.ContractDto;
import com.weddingevent.admin.contract.ContractService.CreateContractRequest;
import com.weddingevent.admin.contract.ContractService.Document;
import com.weddingevent.admin.contract.ContractService.ShareZaloRequest;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contracts (FN-ADM-CONTR-01). Staff can view/export/share; creating contracts is admin-only. */
@RestController
@RequestMapping("/api/v1/admin/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ContractDto>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<ContractDto> create(@Valid @RequestBody CreateContractRequest req) {
        return ApiResponse.ok(service.create(req), "Đã tạo hợp đồng");
    }

    @GetMapping(value = "/{id}/preview-html", produces = MediaType.TEXT_HTML_VALUE)
    public String previewHtml(@PathVariable UUID id) {
        return service.previewHtml(id);
    }

    /** Binary A4 PDF (210 x 297 mm) */
    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        Document doc = service.exportPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.fileName()).build().toString())
                .body(doc.content());
    }

    @PostMapping("/{id}/share-zalo")
    public ApiResponse<Void> shareZalo(@PathVariable UUID id, @Valid @RequestBody(required = false) ShareZaloRequest req) {
        service.shareZalo(id, req == null ? null : req.phoneNumber());
        return ApiResponse.ok(null, "Đã gửi hợp đồng qua Zalo");
    }
}
