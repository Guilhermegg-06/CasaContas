package br.com.casacontas.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public final class MalformedRequestExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> handle(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "MALFORMED_REQUEST",
            "O corpo da requisição está inválido",
            request.getRequestURI(),
            traceId,
            List.of());
    return ResponseEntity.badRequest().body(body);
  }
}
