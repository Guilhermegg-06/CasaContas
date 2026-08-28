package br.com.casacontas.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    String traceId,
    List<FieldError> errors) {

  public ApiErrorResponse {
    errors = List.copyOf(errors);
  }

  public record FieldError(String field, String message) {}
}
