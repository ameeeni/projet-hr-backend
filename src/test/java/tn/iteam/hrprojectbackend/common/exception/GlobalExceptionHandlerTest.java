package tn.iteam.hrprojectbackend.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    void handleResourceNotFound_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Utilisateur", 1L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Utilisateur"));
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleResourceNotFound_ByField_Returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Équipe", "nom", "Dev Team");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Équipe"));
    }

    @Test
    void handleBusinessException_Returns400() {
        BusinessException ex = new BusinessException("DUPLICATE_EMAIL", "Un utilisateur avec cet email existe déjà");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("DUPLICATE_EMAIL", response.getBody().getError());
        assertEquals("Un utilisateur avec cet email existe déjà", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorized_Returns403() {
        UnauthorizedException ex = new UnauthorizedException("Accès refusé");

        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Accès refusé", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_Returns400WithFieldErrors() throws Exception {
        // Create a mock validation error
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userRequest");
        bindingResult.addError(new FieldError("userRequest", "email", "Email invalide"));
        bindingResult.addError(new FieldError("userRequest", "nom", "Le nom est obligatoire"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation Failed", response.getBody().getError());
        assertNotNull(response.getBody().getValidationErrors());
        assertTrue(response.getBody().getValidationErrors().containsKey("email"));
        assertTrue(response.getBody().getValidationErrors().containsKey("nom"));
        assertEquals("Email invalide", response.getBody().getValidationErrors().get("email"));
    }

    @Test
    void handleTypeMismatch_Returns400() {
        // Create a type mismatch exception mock
        MethodArgumentTypeMismatchException ex = org.mockito.Mockito.mock(
                MethodArgumentTypeMismatchException.class);
        org.mockito.Mockito.when(ex.getName()).thenReturn("id");
        org.mockito.Mockito.when(ex.getValue()).thenReturn("abc");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid Parameter", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("id"));
    }

    @Test
    void handleUnreadableMessage_Returns400() {
        HttpMessageNotReadableException ex = org.mockito.Mockito.mock(
                HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Malformed JSON", response.getBody().getError());
    }

    @Test
    void handleGenericException_Returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
    }

    @Test
    void handleGenericException_SwaggerPath_Returns500WithoutBody() {
        request.setRequestURI("/swagger-ui/index.html");
        Exception ex = new RuntimeException("Swagger error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void handleGenericException_ApiDocsPath_Returns500WithoutBody() {
        request.setRequestURI("/v3/api-docs");
        Exception ex = new RuntimeException("API docs error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void handleGenericException_WebjarsPath_Returns500WithoutBody() {
        request.setRequestURI("/webjars/swagger-ui/bundle.js");
        Exception ex = new RuntimeException("Webjars error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void errorResponse_TimestampNotNull() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 1L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleBusinessException_DifferentCodes() {
        String[] codes = {"DUPLICATE_EMAIL", "DUPLICATE_MATRICULE", "HR_CANNOT_HAVE_TEAM",
                "USER_ALREADY_IN_TEAM", "OVERLAPPING_REQUEST", "INSUFFICIENT_BALANCE"};

        for (String code : codes) {
            BusinessException ex = new BusinessException(code, "Error message");
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);
            assertEquals(code, response.getBody().getError());
        }
    }
}
