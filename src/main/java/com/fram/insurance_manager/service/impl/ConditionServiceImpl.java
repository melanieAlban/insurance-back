package com.fram.insurance_manager.service.impl;

import com.fram.insurance_manager.dto.ConditionDto;
import com.fram.insurance_manager.entity.Client;
import com.fram.insurance_manager.entity.Condition;
import com.fram.insurance_manager.repository.ClientRepository;
import com.fram.insurance_manager.repository.ConditionRepository;
import com.fram.insurance_manager.service.ConditionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ConditionServiceImpl implements ConditionService {

    private final ModelMapper modelMapper;
    private final ConditionRepository conditionRepository;
    private final ClientRepository clientRepository;

    @Override
    public List<ConditionDto> getAll() {
        List<Condition> conditions = conditionRepository.findAll();
        return conditions.stream()
                .map(this::conditionToConditionDto)
                .collect(Collectors.toList());
    }

    @Override
    public ConditionDto getById(UUID id) {
        Optional<Condition> conditionOptional = conditionRepository.findById(id);
        return conditionOptional.map(this::conditionToConditionDto).orElse(null);
    }

    @Override
    public ConditionDto save(ConditionDto conditionDto) {
        Condition condition = conditionDtoToCondition(conditionDto);
        Condition condition1 = this.conditionRepository.findByName(condition.getName());
        if (condition1 != null) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "The condition already exists");
        }
        Condition savedCondition = conditionRepository.save(condition);
        return conditionToConditionDto(savedCondition);
    }

    @Override
    public void delete(UUID id) {
        try{
            conditionRepository.deleteById(id);
            conditionRepository.flush();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The condition is used in another client, you cannot delete it");
        }
    }

    @Override
    public List<ConditionDto> getConditionsByClient(UUID clientId) {
        Optional<Client> clientOptional = clientRepository.findById(clientId);
        if (clientOptional.isPresent()) {
            Client client = clientOptional.get();
            List<Condition> conditions = conditionRepository.findAllByClientId(clientId);
            return conditions.stream()
                    .map(this::conditionToConditionDto)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public List<ConditionDto> setConditionToClient(UUID clientId, UUID conditionId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condition not found"));

        if (client.getConditions().contains(condition)) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "The condition is already assigned to the client");
        }

        client.getConditions().add(condition);
        clientRepository.save(client);

        return client.getConditions().stream()
                .map(this::conditionToConditionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ConditionDto> removeConditionFromClient(UUID clientId, UUID conditionId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condition not found"));

        if (!client.getConditions().contains(condition)) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "The condition is not assigned to the client");
        }

        client.getConditions().remove(condition);
        clientRepository.save(client);

        return client.getConditions().stream()
                .map(this::conditionToConditionDto)
                .collect(Collectors.toList());
    }



    private ConditionDto conditionToConditionDto(Condition condition) {
        return modelMapper.map(condition, ConditionDto.class);
    }

    private Condition conditionDtoToCondition(ConditionDto conditionDto) {
        return modelMapper.map(conditionDto, Condition.class);
    }
}
