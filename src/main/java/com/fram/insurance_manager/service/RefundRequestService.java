package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.RefundRequestDto;

import java.util.List;
import java.util.UUID;

public interface RefundRequestService {
    List<RefundRequestDto> getRefundRequests();

    RefundRequestDto getRefundRequest(UUID refundRequestId);

    RefundRequestDto save(RefundRequestDto refundRequestDto);

    void reject(UUID refundRequestId, String reason);

    void approve(UUID refundRequestId);

    void updateAttachments(UUID refundRequestId, List<AttachmentDto> attachmentDtos);
}

