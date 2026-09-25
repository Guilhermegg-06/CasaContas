package br.com.casacontas.shared.api;

import br.com.casacontas.expense.domain.FinancialRuleException;
import br.com.casacontas.shared.application.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiErrorResponse> handleNotFound(
      NoResourceFoundException exception, HttpServletRequest request) {
    return response(
        HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso não encontrado", request, List.of());
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiErrorResponse> handleBusiness(
      BusinessException exception, HttpServletRequest request) {
    return response(
        exception.status(), exception.code(), exception.getMessage(), request, List.of());
  }

  @ExceptionHandler(FinancialRuleException.class)
  ResponseEntity<ApiErrorResponse> handleFinancial(
      FinancialRuleException exception, HttpServletRequest request) {
    return response(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "FINANCIAL_RULE_VIOLATION",
        exception.getMessage(),
        request,
        List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiErrorResponse.FieldError> fields =
        exception.getBindingResult().getFieldErrors().stream().map(this::mapFieldError).toList();
    return response(
        HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revise os campos informados", request, fields);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiErrorResponse> handleForbidden(
      AccessDeniedException exception, HttpServletRequest request) {
    return response(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "Você não tem permissão para esta ação",
        request,
        List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiErrorResponse> handleConflict(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        "DATA_CONFLICT",
        "A operação conflita com dados existentes",
        request,
        List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    LOGGER.error("Unexpected request failure", exception);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Não foi possível concluir a operação",
        request,
        List.of());
  }

  private ApiErrorResponse.FieldError mapFieldError(FieldError fieldError) {
    return new ApiErrorResponse.FieldError(
        fieldError.getField(),
        fieldError.getDefaultMessage() == null ? "valor inválido" : fieldError.getDefaultMessage());
  }

  private ResponseEntity<ApiErrorResponse> response(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<ApiErrorResponse.FieldError> errors) {
    String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(), status.value(), code, message, request.getRequestURI(), traceId, errors);
    return ResponseEntity.status(status).body(body);
  }
}
