package com.fram.insurance_manager.controller;

import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.RefundRequestDto;
import com.fram.insurance_manager.dto.RejectRefundRequestDto;
import com.fram.insurance_manager.service.RefundRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("refund-request")
@RequiredArgsConstructor
public class RefundRequestController {
    private final RefundRequestService refundRequestService;

    @GetMapping
    public List<RefundRequestDto> getAll() {
        return refundRequestService.getRefundRequests();
    }

    @GetMapping("{refundRequestId}")
    public RefundRequestDto getById(@PathVariable UUID refundRequestId) {
        return refundRequestService.getRefundRequest(refundRequestId);
    }

    @PostMapping
    public RefundRequestDto save(@Valid @RequestBody RefundRequestDto refundRequestDto) {
        return refundRequestService.save(refundRequestDto);
    }

    @PostMapping("reject")
    public void reject(@Valid @RequestBody RejectRefundRequestDto refundRequestDto) {
        refundRequestService.reject(refundRequestDto.getId(), refundRequestDto.getReason());
    }

    @PostMapping("approve/{refundRequestId}")
    public void approve(@PathVariable UUID refundRequestId) {
        refundRequestService.approve(refundRequestId);
    }

    @PostMapping("attachments/{refundRequestId}")
    public void uploadDocumentAttachments(@PathVariable UUID refundRequestId,
                                          @Valid @RequestBody List<AttachmentDto> attachments) {
        refundRequestService.updateAttachments(refundRequestId, attachments);
    }
}
