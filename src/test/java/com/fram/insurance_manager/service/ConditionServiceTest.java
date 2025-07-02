package com.fram.insurance_manager.service;

import com.fram.insurance_manager.dto.ConditionDto;
import com.fram.insurance_manager.entity.Client;
import com.fram.insurance_manager.entity.Condition;
import com.fram.insurance_manager.repository.ClientRepository;
import com.fram.insurance_manager.repository.ConditionRepository;
import com.fram.insurance_manager.service.impl.ConditionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionServiceTest {

    @Nested
    class ConditionServiceImplTest {

        @Mock(lenient = true)
        private ConditionRepository conditionRepository;

        @Mock(lenient = true)
        private ClientRepository clientRepository;

        @Mock(lenient = true)
        private ModelMapper modelMapper;

        @InjectMocks
        private ConditionServiceImpl conditionService;

        private Condition condition;
        private ConditionDto conditionDto;
        private Client client;
        private UUID conditionId;
        private UUID clientId;

        @BeforeEach
        void setUp() {
            conditionId = UUID.randomUUID();
            clientId = UUID.randomUUID();

            condition = new Condition();
            condition.setId(conditionId);
            condition.setName("Diabetes");
            condition.setDescription("Type 2 diabetes");
            condition.setAddedPercentage(15);
            condition.setClient(new ArrayList<>());

            conditionDto = new ConditionDto();
            conditionDto.setId(conditionId);
            conditionDto.setName("Diabetes");
            conditionDto.setDescription("Type 2 diabetes");
            conditionDto.setAddedPercentage(15);

            client = new Client();
            client.setId(clientId);
            client.setName("John");
            client.setLastName("Doe");
            client.setIdentificationNumber("123456789");
            client.setConditions(new ArrayList<>());
        }

        // Tests for getAll
        @Test
        void shouldGetAllConditions() {
            when(conditionRepository.findAll()).thenReturn(List.of(condition));
            when(modelMapper.map(condition, ConditionDto.class)).thenReturn(conditionDto);

            List<ConditionDto> result = conditionService.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(conditionDto);
            verify(conditionRepository).findAll();
            verify(modelMapper).map(condition, ConditionDto.class);
        }

        @Test
        void shouldReturnEmptyListWhenNoConditionsExist() {
            when(conditionRepository.findAll()).thenReturn(Collections.emptyList());

            List<ConditionDto> result = conditionService.getAll();

            assertThat(result).isEmpty();
            verify(conditionRepository).findAll();
            verifyNoInteractions(modelMapper);
        }

        // Tests for getById
        @Test
        void shouldGetConditionById() {
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.of(condition));
            when(modelMapper.map(condition, ConditionDto.class)).thenReturn(conditionDto);

            ConditionDto result = conditionService.getById(conditionId);

            assertThat(result).isEqualTo(conditionDto);
            verify(conditionRepository).findById(conditionId);
            verify(modelMapper).map(condition, ConditionDto.class);
        }

        @Test
        void shouldReturnNullWhenGettingNonExistingConditionById() {
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.empty());

            ConditionDto result = conditionService.getById(conditionId);

            assertThat(result).isNull();
            verify(conditionRepository).findById(conditionId);
            verifyNoInteractions(modelMapper);
        }

        // Tests for save
        @Test
        void shouldSaveConditionSuccessfully() {
            when(modelMapper.map(conditionDto, Condition.class)).thenReturn(condition);
            when(conditionRepository.findByName("Diabetes")).thenReturn(null);
            when(conditionRepository.save(condition)).thenReturn(condition);
            when(modelMapper.map(condition, ConditionDto.class)).thenReturn(conditionDto);

            ConditionDto result = conditionService.save(conditionDto);

            assertThat(result).isEqualTo(conditionDto);
            verify(modelMapper).map(conditionDto, Condition.class);
            verify(conditionRepository).findByName("Diabetes");
            verify(conditionRepository).save(condition);
            verify(modelMapper).map(condition, ConditionDto.class);
        }

        @Test
        void shouldThrowWhenSavingExistingCondition() {
            when(modelMapper.map(conditionDto, Condition.class)).thenReturn(condition);
            when(conditionRepository.findByName("Diabetes")).thenReturn(condition);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.save(conditionDto)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(exception.getReason()).isEqualTo("The condition already exists");
            verify(modelMapper).map(conditionDto, Condition.class);
            verify(conditionRepository).findByName("Diabetes");
            verifyNoMoreInteractions(conditionRepository);
        }

        // Tests for delete
        @Test
        void shouldDeleteConditionSuccessfully() {
            doNothing().when(conditionRepository).deleteById(conditionId);
            doNothing().when(conditionRepository).flush();

            conditionService.delete(conditionId);

            verify(conditionRepository).deleteById(conditionId);
            verify(conditionRepository).flush();
        }

        @Test
        void shouldThrowWhenDeletingConditionUsedByClient() {
            doThrow(new RuntimeException("Cannot delete")).when(conditionRepository).deleteById(conditionId);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.delete(conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isEqualTo("The condition is used in another client, you cannot delete it");
            verify(conditionRepository).deleteById(conditionId);
        }

        // Tests for getConditionsByClient
        @Test
        void shouldGetConditionsByClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findAllByClientId(clientId)).thenReturn(List.of(condition));
            when(modelMapper.map(condition, ConditionDto.class)).thenReturn(conditionDto);

            List<ConditionDto> result = conditionService.getConditionsByClient(clientId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(conditionDto);
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findAllByClientId(clientId);
            verify(modelMapper).map(condition, ConditionDto.class);
        }

        @Test
        void shouldReturnEmptyListWhenClientHasNoConditions() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findAllByClientId(clientId)).thenReturn(Collections.emptyList());

            List<ConditionDto> result = conditionService.getConditionsByClient(clientId);

            assertThat(result).isEmpty();
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findAllByClientId(clientId);
            verifyNoInteractions(modelMapper);
        }

        @Test
        void shouldReturnEmptyListWhenClientDoesNotExist() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

            List<ConditionDto> result = conditionService.getConditionsByClient(clientId);

            assertThat(result).isEmpty();
            verify(clientRepository).findById(clientId);
            verifyNoInteractions(conditionRepository, modelMapper);
        }

        // Tests for setConditionToClient
        @Test
        void shouldSetConditionToClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.of(condition));
            when(clientRepository.save(client)).thenReturn(client);
            when(modelMapper.map(condition, ConditionDto.class)).thenReturn(conditionDto);

            List<ConditionDto> result = conditionService.setConditionToClient(clientId, conditionId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(conditionDto);
            assertThat(client.getConditions()).contains(condition);
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
            verify(clientRepository).save(client);
            verify(modelMapper).map(condition, ConditionDto.class);
        }

        @Test
        void shouldThrowWhenSettingConditionToNonExistingClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.setConditionToClient(clientId, conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getReason()).isEqualTo("Client not found");
            verify(clientRepository).findById(clientId);
            verifyNoInteractions(conditionRepository);
        }

        @Test
        void shouldThrowWhenSettingNonExistingConditionToClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.setConditionToClient(clientId, conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getReason()).isEqualTo("Condition not found");
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
            verifyNoInteractions(modelMapper);
        }

        @Test
        void shouldThrowWhenSettingAlreadyAssignedConditionToClient() {
            client.getConditions().add(condition);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.of(condition));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.setConditionToClient(clientId, conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(exception.getReason()).isEqualTo("The condition is already assigned to the client");
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
            verifyNoInteractions(modelMapper);
        }

        // Tests for removeConditionFromClient
        @Test
        void shouldRemoveConditionFromClient() {
            client.getConditions().add(condition);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.of(condition));
            when(clientRepository.save(client)).thenReturn(client);

            List<ConditionDto> result = conditionService.removeConditionFromClient(clientId, conditionId);

            assertThat(result).isEmpty();
            assertThat(client.getConditions()).doesNotContain(condition);
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
            verify(clientRepository).save(client);
        }

        @Test
        void shouldThrowWhenRemovingConditionFromNonExistingClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.removeConditionFromClient(clientId, conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getReason()).isEqualTo("Client not found");
            verify(clientRepository).findById(clientId);
            verifyNoInteractions(conditionRepository);
        }

        @Test
        void shouldThrowWhenRemovingNonExistingConditionFromClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.removeConditionFromClient(clientId, conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getReason()).isEqualTo("Condition not found");
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
        }

        @Test
        void shouldThrowWhenRemovingNotAssignedConditionFromClient() {
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.of(condition));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> conditionService.removeConditionFromClient(clientId, conditionId)
            );

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(exception.getReason()).isEqualTo("The condition is not assigned to the client");
            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
        }

        // Edge case tests
        @Test
        void shouldHandleNullReturnFromModelMapper() {
            when(conditionRepository.findAll()).thenReturn(List.of(condition));
            when(modelMapper.map(condition, ConditionDto.class)).thenReturn(null);

            List<ConditionDto> result = conditionService.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isNull();
            verify(conditionRepository).findAll();
            verify(modelMapper).map(condition, ConditionDto.class);
        }

        @Test
        void shouldHandleExceptionInClientRepositorySave() {
            client.getConditions().add(condition);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(conditionRepository.findById(conditionId)).thenReturn(Optional.of(condition));
            when(clientRepository.save(client)).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class, 
                () -> conditionService.removeConditionFromClient(clientId, conditionId));

            verify(clientRepository).findById(clientId);
            verify(conditionRepository).findById(conditionId);
            verify(clientRepository).save(client);
        }

        @Test
        void shouldHandleNullReturnFromConditionRepository() {
            when(conditionRepository.findAll()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> conditionService.getAll());

            verify(conditionRepository).findAll();
            verifyNoInteractions(modelMapper);
        }
    }
}
