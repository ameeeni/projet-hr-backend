package tn.iteam.hrprojectbackend.leave.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iteam.hrprojectbackend.common.exception.BusinessException;
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.leave.dto.LeaveRequest;
import tn.iteam.hrprojectbackend.leave.dto.LeaveResponseDto;
import tn.iteam.hrprojectbackend.leave.dto.LeaveValidationDto;
import tn.iteam.hrprojectbackend.leave.entities.Leave;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.mapper.LeaveMapper;
import tn.iteam.hrprojectbackend.leave.repositories.LeaveRepository;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock private LeaveRepository leaveRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeaveMapper leaveMapper;
    @Mock private HrEventProducer hrEventProducer;

    @InjectMocks private LeaveServiceImpl leaveService;

    private User employee;
    private User manager;
    private Leave pendingLeave;
    private Leave savedLeave;
    private LeaveRequest leaveRequest;
    private LeaveResponseDto leaveResponseDto;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setNom("Doe");
        employee.setPrenom("John");
        employee.setEmail("john.doe@test.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setSoldeConge(20);

        manager = new User();
        manager.setId(2L);
        manager.setNom("Manager");
        manager.setEmail("manager@test.com");
        manager.setRole(Role.MANAGER);

        leaveRequest = LeaveRequest.builder()
                .type(LeaveType.MALADIE)
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusDays(3))
                .motif("Sick")
                .build();

        pendingLeave = Leave.builder()
                .id(1L)
                .employee(employee)
                .type(LeaveType.MALADIE)
                .status(LeaveStatus.PENDING)
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusDays(3))
                .nombreJours(3)
                .build();

        savedLeave = Leave.builder()
                .id(1L)
                .employee(employee)
                .type(LeaveType.MALADIE)
                .status(LeaveStatus.PENDING)
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusDays(3))
                .nombreJours(3)
                .dateSoumission(LocalDate.now())
                .build();

        leaveResponseDto = LeaveResponseDto.builder()
                .id(1L)
                .employeeId(1L)
                .employeeNom("Doe John")
                .type(LeaveType.MALADIE)
                .status(LeaveStatus.PENDING)
                .nombreJours(3)
                .build();
    }

    // ──────────── submitRequest ────────────

    @Test
    void submitRequest_Success_MALADIE() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any())).thenReturn(List.of());
        when(leaveMapper.toEntity(leaveRequest)).thenReturn(pendingLeave);
        when(leaveRequestRepository.save(any())).thenReturn(savedLeave);
        when(leaveMapper.toResponse(savedLeave)).thenReturn(leaveResponseDto);

        LeaveResponseDto result = leaveService.submitRequest(1L, leaveRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(hrEventProducer).sendLeaveSubmitted(any());
    }

    @Test
    void submitRequest_Success_CONGE_ANNUEL_SufficientBalance() {
        leaveRequest.setType(LeaveType.CONGE_ANNUEL);
        leaveRequest.setDateDebut(LocalDate.now().plusDays(1));
        leaveRequest.setDateFin(LocalDate.now().plusDays(4)); // 4 days
        employee.setSoldeConge(10); // 10 days available

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any())).thenReturn(List.of());

        Leave congeLeave = Leave.builder()
                .id(1L)
                .employee(employee)
                .type(LeaveType.CONGE_ANNUEL)
                .status(LeaveStatus.PENDING)
                .dateDebut(leaveRequest.getDateDebut())
                .dateFin(leaveRequest.getDateFin())
                .nombreJours(4)
                .build();

        when(leaveMapper.toEntity(leaveRequest)).thenReturn(congeLeave);
        when(leaveRequestRepository.save(any())).thenReturn(congeLeave);
        when(leaveMapper.toResponse(congeLeave)).thenReturn(leaveResponseDto);

        LeaveResponseDto result = leaveService.submitRequest(1L, leaveRequest);

        assertNotNull(result);
        verify(hrEventProducer).sendLeaveSubmitted(any());
    }

    @Test
    void submitRequest_EmployeeNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> leaveService.submitRequest(99L, leaveRequest));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void submitRequest_InvalidDateRange_ThrowsBusinessException() {
        leaveRequest.setDateDebut(LocalDate.now().plusDays(5));
        leaveRequest.setDateFin(LocalDate.now().plusDays(2)); // fin before début

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.submitRequest(1L, leaveRequest));
        assertEquals("INVALID_DATE_RANGE", ex.getCode());
    }

    @Test
    void submitRequest_OverlappingRequest_ThrowsBusinessException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any()))
                .thenReturn(List.of(pendingLeave));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.submitRequest(1L, leaveRequest));
        assertEquals("OVERLAPPING_REQUEST", ex.getCode());
    }

    @Test
    void submitRequest_CONGE_ANNUEL_InsufficientBalance_ThrowsBusinessException() {
        leaveRequest.setType(LeaveType.CONGE_ANNUEL);
        leaveRequest.setDateDebut(LocalDate.now().plusDays(1));
        leaveRequest.setDateFin(LocalDate.now().plusDays(10)); // 10 days
        employee.setSoldeConge(5); // only 5 days available

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.submitRequest(1L, leaveRequest));
        assertEquals("INSUFFICIENT_BALANCE", ex.getCode());
    }

    @Test
    void submitRequest_CONGE_ANNUEL_NullBalance_ThrowsBusinessException() {
        leaveRequest.setType(LeaveType.CONGE_ANNUEL);
        leaveRequest.setDateDebut(LocalDate.now().plusDays(1));
        leaveRequest.setDateFin(LocalDate.now().plusDays(3));
        employee.setSoldeConge(null); // null balance

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findOverlappingRequests(any(), any(), any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.submitRequest(1L, leaveRequest));
        assertEquals("INSUFFICIENT_BALANCE", ex.getCode());
    }

    // ──────────── cancelRequest ────────────

    @Test
    void cancelRequest_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(leaveRequestRepository.save(any())).thenReturn(pendingLeave);
        when(leaveMapper.toResponse(any())).thenReturn(leaveResponseDto);

        LeaveResponseDto result = leaveService.cancelRequest(1L, 1L);

        assertNotNull(result);
        assertEquals(LeaveStatus.CANCELLED, pendingLeave.getStatus());
    }

    @Test
    void cancelRequest_RequestNotFound_ThrowsBusinessException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.cancelRequest(99L, 1L));
        assertEquals("REQUEST_NOT_FOUND", ex.getCode());
    }

    @Test
    void cancelRequest_WrongEmployee_ThrowsBusinessException() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.cancelRequest(1L, 99L)); // different employee
        assertEquals("CANNOT_CANCEL_REQUEST", ex.getCode());
    }

    @Test
    void cancelRequest_NotPending_ThrowsBusinessException() {
        pendingLeave.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.cancelRequest(1L, 1L));
        assertEquals("REQUEST_ALREADY_PROCESSED", ex.getCode());
    }

    // ──────────── validateRequest ────────────

    @Test
    void validateRequest_Success_Rejected() {
        LeaveValidationDto dto = LeaveValidationDto.builder()
                .decision(LeaveStatus.REJECTED)
                .commentaireValidateur("Not enough justification")
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any())).thenReturn(pendingLeave);
        when(leaveMapper.toResponse(any())).thenReturn(leaveResponseDto);

        LeaveResponseDto result = leaveService.validateRequest(1L, 2L, dto);

        assertNotNull(result);
        assertEquals(LeaveStatus.REJECTED, pendingLeave.getStatus());
        assertEquals(manager, pendingLeave.getValidatedBy());
        verify(hrEventProducer).sendLeaveValidated(any());
    }

    @Test
    void validateRequest_Success_Approved_MALADIE_NoDeduction() {
        LeaveValidationDto dto = LeaveValidationDto.builder()
                .decision(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any())).thenReturn(pendingLeave);
        when(leaveMapper.toResponse(any())).thenReturn(leaveResponseDto);

        leaveService.validateRequest(1L, 2L, dto);

        // No balance deduction for MALADIE
        verify(userRepository, never()).save(employee);
    }

    @Test
    void validateRequest_Success_Approved_CONGE_ANNUEL_DeductsBalance() {
        pendingLeave.setType(LeaveType.CONGE_ANNUEL);
        pendingLeave.setNombreJours(5);
        employee.setSoldeConge(20);

        LeaveValidationDto dto = LeaveValidationDto.builder()
                .decision(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.save(any())).thenReturn(pendingLeave);
        when(leaveMapper.toResponse(any())).thenReturn(leaveResponseDto);

        leaveService.validateRequest(1L, 2L, dto);

        assertEquals(15, employee.getSoldeConge()); // 20 - 5 = 15
        verify(userRepository).save(employee);
    }

    @Test
    void validateRequest_RequestNotFound_ThrowsBusinessException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        LeaveValidationDto dto = new LeaveValidationDto(LeaveStatus.APPROVED, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.validateRequest(99L, 2L, dto));
        assertEquals("REQUEST_NOT_FOUND", ex.getCode());
    }

    @Test
    void validateRequest_ValidatorNotFound_ThrowsBusinessException() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        LeaveValidationDto dto = new LeaveValidationDto(LeaveStatus.APPROVED, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.validateRequest(1L, 99L, dto));
        assertEquals("VALIDATOR_NOT_FOUND", ex.getCode());
    }

    @Test
    void validateRequest_InvalidValidatorRole_ThrowsBusinessException() {
        User notValidator = new User();
        notValidator.setId(3L);
        notValidator.setRole(Role.EMPLOYEE);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(3L)).thenReturn(Optional.of(notValidator));

        LeaveValidationDto dto = new LeaveValidationDto(LeaveStatus.APPROVED, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.validateRequest(1L, 3L, dto));
        assertEquals("INVALID_VALIDATOR_ROLE", ex.getCode());
    }

    @Test
    void validateRequest_AlreadyProcessed_ThrowsBusinessException() {
        pendingLeave.setStatus(LeaveStatus.APPROVED); // already processed

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        LeaveValidationDto dto = new LeaveValidationDto(LeaveStatus.APPROVED, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> leaveService.validateRequest(1L, 2L, dto));
        assertEquals("TREATED_REQUEST", ex.getCode());
    }

    @Test
    void validateRequest_ByHR_Success() {
        User hrUser = new User();
        hrUser.setId(3L);
        hrUser.setRole(Role.HR);

        LeaveValidationDto dto = LeaveValidationDto.builder()
                .decision(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(userRepository.findById(3L)).thenReturn(Optional.of(hrUser));
        when(leaveRequestRepository.save(any())).thenReturn(pendingLeave);
        when(leaveMapper.toResponse(any())).thenReturn(leaveResponseDto);

        LeaveResponseDto result = leaveService.validateRequest(1L, 3L, dto);

        assertNotNull(result);
    }

    // ──────────── getById ────────────

    @Test
    void getById_Success() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(pendingLeave));
        when(leaveMapper.toResponse(pendingLeave)).thenReturn(leaveResponseDto);

        LeaveResponseDto result = leaveService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_NotFound_ThrowsResourceNotFoundException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.getById(99L));
    }

    // ──────────── getHistoryByEmployee ────────────

    @Test
    void getHistoryByEmployee_ReturnsList() {
        when(leaveRequestRepository.findHistoryByEmployeeId(1L)).thenReturn(List.of(pendingLeave));
        when(leaveMapper.toResponseList(any())).thenReturn(List.of(leaveResponseDto));

        List<LeaveResponseDto> result = leaveService.getHistoryByEmployee(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getHistoryByEmployee_EmptyList() {
        when(leaveRequestRepository.findHistoryByEmployeeId(1L)).thenReturn(List.of());
        when(leaveMapper.toResponseList(any())).thenReturn(List.of());

        List<LeaveResponseDto> result = leaveService.getHistoryByEmployee(1L);

        assertThat(result).isEmpty();
    }

    // ──────────── getPendingByTeam ────────────

    @Test
    void getPendingByTeam_ReturnsList() {
        when(leaveRequestRepository.findPendingByTeamId(1L)).thenReturn(List.of(pendingLeave));
        when(leaveMapper.toResponseList(any())).thenReturn(List.of(leaveResponseDto));

        List<LeaveResponseDto> result = leaveService.getPendingByTeam(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPendingByTeam_EmptyList() {
        when(leaveRequestRepository.findPendingByTeamId(1L)).thenReturn(List.of());
        when(leaveMapper.toResponseList(any())).thenReturn(List.of());

        List<LeaveResponseDto> result = leaveService.getPendingByTeam(1L);

        assertThat(result).isEmpty();
    }

    // ──────────── getAllRequests ────────────

    @Test
    void getAllRequests_ReturnsList() {
        when(leaveRequestRepository.findAll()).thenReturn(List.of(pendingLeave));
        when(leaveMapper.toResponseList(any())).thenReturn(List.of(leaveResponseDto));

        List<LeaveResponseDto> result = leaveService.getAllRequests();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllRequests_EmptyList() {
        when(leaveRequestRepository.findAll()).thenReturn(List.of());
        when(leaveMapper.toResponseList(any())).thenReturn(List.of());

        List<LeaveResponseDto> result = leaveService.getAllRequests();

        assertThat(result).isEmpty();
    }

    // ──────────── getByStatus ────────────

    @Test
    void getByStatus_ReturnsList() {
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING)).thenReturn(List.of(pendingLeave));
        when(leaveMapper.toResponseList(any())).thenReturn(List.of(leaveResponseDto));

        List<LeaveResponseDto> result = leaveService.getByStatus(LeaveStatus.PENDING);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByStatus_EmptyList() {
        when(leaveRequestRepository.findByStatus(LeaveStatus.APPROVED)).thenReturn(List.of());
        when(leaveMapper.toResponseList(any())).thenReturn(List.of());

        List<LeaveResponseDto> result = leaveService.getByStatus(LeaveStatus.APPROVED);

        assertThat(result).isEmpty();
    }

    // ──────────── getByEmployeeAndStatus ────────────

    @Test
    void getByEmployeeAndStatus_ReturnsList() {
        when(leaveRequestRepository.findByEmployeeIdAndStatus(1L, LeaveStatus.PENDING))
                .thenReturn(List.of(pendingLeave));
        when(leaveMapper.toResponseList(any())).thenReturn(List.of(leaveResponseDto));

        List<LeaveResponseDto> result = leaveService.getByEmployeeAndStatus(1L, LeaveStatus.PENDING);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByEmployeeAndStatus_EmptyList() {
        when(leaveRequestRepository.findByEmployeeIdAndStatus(1L, LeaveStatus.APPROVED))
                .thenReturn(List.of());
        when(leaveMapper.toResponseList(any())).thenReturn(List.of());

        List<LeaveResponseDto> result = leaveService.getByEmployeeAndStatus(1L, LeaveStatus.APPROVED);

        assertThat(result).isEmpty();
    }

    // ──────────── isOwner ────────────

    @Test
    void isOwner_True() {
        when(userRepository.findByEmail("john.doe@test.com")).thenReturn(Optional.of(employee));

        boolean result = leaveService.isOwner("john.doe@test.com", 1L);

        assertTrue(result);
    }

    @Test
    void isOwner_False_DifferentId() {
        when(userRepository.findByEmail("john.doe@test.com")).thenReturn(Optional.of(employee));

        boolean result = leaveService.isOwner("john.doe@test.com", 99L);

        assertFalse(result);
    }

    @Test
    void isOwner_False_UserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        boolean result = leaveService.isOwner("unknown@test.com", 1L);

        assertFalse(result);
    }
}
