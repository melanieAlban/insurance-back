package com.fram.insurance_manager.service.impl;

import com.fram.insurance_manager.dto.AttachmentDto;
import com.fram.insurance_manager.dto.ClientDto;
import com.fram.insurance_manager.dto.SaveUserDto;
import com.fram.insurance_manager.dto.UserDto;
import com.fram.insurance_manager.entity.Attachment;
import com.fram.insurance_manager.entity.Client;
import com.fram.insurance_manager.entity.User;
import com.fram.insurance_manager.enums.AttachmentType;
import com.fram.insurance_manager.enums.ContractStep;
import com.fram.insurance_manager.enums.UserRol;
import com.fram.insurance_manager.repository.ClientRepository;
import com.fram.insurance_manager.repository.UserRepository;
import com.fram.insurance_manager.service.AuthService;
import com.fram.insurance_manager.service.ClientService;
import com.fram.insurance_manager.util.AttachmentUtil;
import com.fram.insurance_manager.util.UserUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ModelMapper modelMapper;
    private final ClientRepository clientRepository;
    private final AuthService userService;
    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final AttachmentUtil attachmentUtil;

    private static final Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);

    @Override
    public List<ClientDto> getAll() {
        return List.copyOf(
                clientRepository.findAll().stream()
                        .map(this::clientToDto)
                        .toList()
        );
    }

    @Override
    public ClientDto getById(UUID id) {
        Client client = findClientById(id);
        return clientToDto(client);
    }

    @Override
    @Transactional
    public ClientDto save(ClientDto clientDto) {
        String password = userUtil.generateRandomPassword();

        SaveUserDto saveUserDto = clientDto.getUser();
        saveUserDto.setPassword(password);

        UserDto savedUser = userService.register(saveUserDto);

        Client client = dtoToClient(clientDto);
        client.setId(null);
        client.setUser(this.modelMapper.map(savedUser, User.class));

        Client savedClient = clientRepository.save(client);

        userUtil.sendCredentialsToUser(savedUser.getEmail(), savedUser.getEmail(), password);
        logger.info("Cliente registrado exitosamente con ID {}", savedClient.getId());

        return clientToDto(savedClient);
    }

    @Override
    @Transactional
    public ClientDto update(UUID id, ClientDto clientDto) {
        Client existingClient = findClientById(id);
        clientDto.getUser().setRol(UserRol.CLIENT);
        User existingUser = existingClient.getUser();

        if (!existingUser.getEmail().equals(clientDto.getUser().getEmail())) {
            logger.warn("Intento de modificación de email para cliente ID {}", id);
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "El email no puede ser modificado");
        }

        String password = existingUser.getPassword();
        modelMapper.map(clientDto, existingClient);
        existingClient.setUser(existingUser);
        existingClient.getUser().setPassword(password);
        Client updatedClient = clientRepository.save(existingClient);
        logger.info("Cliente actualizado correctamente con ID {}", updatedClient.getId());
        return clientToDto(updatedClient);
    }


    @Override
    @Transactional
    public void delete(UUID id) {
        Client client = findClientById(id);

        userRepository.deleteById(client.getUser().getId());
        clientRepository.deleteById(id);
        logger.info("Eliminación de cliente con ID {}", id);
    }

    @Override
    public ClientDto getByIdentification(String identificationNumber) {
        Client client = clientRepository.findByIdentificationNumber(identificationNumber);
        if (client != null) {
            logger.info("Cliente encontrado con cédula {}", identificationNumber);
            return clientToDto(client);
        }
        logger.warn("Cliente no encontrado con cédula {}", identificationNumber);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
    }

    @Transactional
    @Override
    public void uploadDocumentAttachments(UUID clientId, List<AttachmentDto> attachmentDtos) {
        Client client = findClientById(clientId);

        validateAttachmentTypes(attachmentDtos);

        attachmentDtos.forEach(attachmentDto -> {
            Attachment attachment = attachmentDtoToEntity(attachmentDto);

            String pathReference = client.getAttachments().stream()
                    .filter(attachment1 -> attachment.getAttachmentType().equals(attachment1.getAttachmentType()))
                    .map(Attachment::getPathReference).findFirst().orElse(null);

            attachment.setClient(client);
            Path path = attachmentUtil.saveAttachmentOnDiskBase64(attachmentDto.getContent(), attachmentDto.getFileName(), client.getLastName(), pathReference);
            attachment.setPathReference(path.toString());
            
            if (pathReference == null) {
                client.getAttachments().add(attachment);
            } else {
                client.getAttachments().stream()
                        .filter(existing -> attachment.getAttachmentType().equals(existing.getAttachmentType()))
                        .findFirst().ifPresent(existing -> {
                            existing.setFileName(attachment.getFileName());
                        });
            }
        });


        client.getContracts().stream().filter(Objects::nonNull).forEach(contract -> {
            contract.getStepStatuses().put(ContractStep.UPLOAD_DOCUMENTS, true);
        });

        logger.info("Documentos guardado correctamente para cliente con id {}", client.getId());
        clientRepository.save(client);
    }

    private ClientDto clientToDto(Client client) {
        return modelMapper.map(client, ClientDto.class);
    }

    private Client dtoToClient(ClientDto clientDto) {
        Client client = modelMapper.map(clientDto, Client.class);
        if (clientDto.getUser() != null) {
            client.setUser(modelMapper.map(clientDto.getUser(), User.class));
        }
        client.getUser().setRol(UserRol.CLIENT);
        return client;
    }

    private Client findClientById(UUID id) {
        return clientRepository.findById(id)
                .map(client -> {
                    logger.info("Cliente encontrado con ID {}", id);
                    return client;
                })
                .orElseThrow(() -> {
                    logger.warn("Cliente no encontrado con ID {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
                });
    }

    private Attachment attachmentDtoToEntity(AttachmentDto dto) {
        return modelMapper.map(dto, Attachment.class);
    }

    private void validateAttachmentTypes(List<AttachmentDto> attachments) {
        Set<AttachmentType> requiredTypes = Set.of(AttachmentType.IDENTIFICATION, AttachmentType.PORTRAIT_PHOTO);

        attachments.forEach(attachmentUtil::validateAttachment);

        if (attachments.size() != 2) {
            logger.warn("Validación fallida: cantidad incorrecta de archivos ({} recibidos)", attachments.size());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Son dos archivos: identificación y foto");
        }

        Set<AttachmentType> receivedTypes = attachments.stream()
                .map(AttachmentDto::getAttachmentType)
                .collect(Collectors.toSet());

        if (!receivedTypes.containsAll(requiredTypes)) {
            logger.warn("Validación fallida: tipos de archivo incompletos ({})", receivedTypes);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los archivos deben incluir uno de identificación y otro de tipo retrato");
        }
    }
}