package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.ClientDto;

import java.util.List;
import java.util.UUID;

public interface ClientService {
    List<ClientDto> getAll();

    ClientDto getById(UUID id);

    ClientDto save(ClientDto clientDto);

    ClientDto update(UUID id, ClientDto clientDto);

    void delete(UUID id);

    ClientDto getByIdentification(String identificationNumber);

    void uploadDocumentAttachments(UUID clientId, List<AttachmentDto> attachments);
}
