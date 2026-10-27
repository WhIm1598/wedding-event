package com.weddingevent.admin.contract;

import com.weddingevent.admin.contract.ContractService.Document;
import com.weddingevent.admin.contract.ContractShareService.PublicContractDto;
import com.weddingevent.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FN-PUB-CONTR-01: the page the Zalo link opens on the customer's phone. No login; the token is the credential. */
@RestController
@RequestMapping("/api/v1/public/contracts/{token}")
public class PublicContractController {

    private final ContractShareService sharing;

    public PublicContractController(ContractShareService sharing) {
        this.sharing = sharing;
    }

    @GetMapping
    public ApiResponse<PublicContractDto> view(@PathVariable String token) {
        return ApiResponse.ok(sharing.publicView(token));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String token) {
        Document doc = sharing.publicPdf(token);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.fileName()).build().toString())
                .body(doc.content());
    }

    /** The client IP comes through the gateway's X-Forwarded-For (server.forward-headers-strategy) */
    @PostMapping("/accept")
    public ApiResponse<PublicContractDto> accept(@PathVariable String token, HttpServletRequest request) {
        return ApiResponse.ok(sharing.accept(token, request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT)),
                "Cảm ơn bạn đã xác nhận hợp đồng");
    }
}
