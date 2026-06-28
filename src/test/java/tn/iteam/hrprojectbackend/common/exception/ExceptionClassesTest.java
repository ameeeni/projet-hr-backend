package tn.iteam.hrprojectbackend.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionClassesTest {

    // ──────────── BusinessException ────────────

    @Test
    void businessException_StoresCodeAndMessage() {
        BusinessException ex = new BusinessException("DUPLICATE_EMAIL", "Email already exists");

        assertEquals("DUPLICATE_EMAIL", ex.getCode());
        assertEquals("Email already exists", ex.getMessage());
    }

    @Test
    void businessException_IsRuntimeException() {
        BusinessException ex = new BusinessException("ERROR", "msg");

        assertInstanceOf(RuntimeException.class, ex);
    }

    // ──────────── ResourceNotFoundException ────────────

    @Test
    void resourceNotFoundException_ById_SetsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Utilisateur", 42L);

        assertTrue(ex.getMessage().contains("Utilisateur"));
        assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    void resourceNotFoundException_ByField_SetsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Équipe", "nom", "Dev Team");

        assertTrue(ex.getMessage().contains("Équipe"));
        assertTrue(ex.getMessage().contains("nom"));
        assertTrue(ex.getMessage().contains("Dev Team"));
    }

    @Test
    void resourceNotFoundException_IsRuntimeException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Test", 1L);

        assertInstanceOf(RuntimeException.class, ex);
    }

    // ──────────── UnauthorizedException ────────────

    @Test
    void unauthorizedException_StoresMessage() {
        UnauthorizedException ex = new UnauthorizedException("Access denied");

        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void unauthorizedException_IsRuntimeException() {
        UnauthorizedException ex = new UnauthorizedException("Forbidden");

        assertInstanceOf(RuntimeException.class, ex);
    }

    // ──────────── ErrorResponse ────────────

    @Test
    void errorResponse_Builder_SetsAllFields() {
        ErrorResponse response = ErrorResponse.builder()
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/test")
                .build();

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Resource not found", response.getMessage());
        assertEquals("/api/test", response.getPath());
    }

    @Test
    void errorResponse_NoArgs_DefaultValues() {
        ErrorResponse response = new ErrorResponse();

        assertEquals(0, response.getStatus());
        assertNull(response.getError());
        assertNull(response.getMessage());
    }

    @Test
    void errorResponse_WithValidationErrors() {
        java.util.Map<String, String> errors = new java.util.HashMap<>();
        errors.put("email", "Email invalide");

        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .validationErrors(errors)
                .build();

        assertNotNull(response.getValidationErrors());
        assertEquals("Email invalide", response.getValidationErrors().get("email"));
    }
}
