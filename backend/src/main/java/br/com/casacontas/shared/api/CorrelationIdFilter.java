package br.com.casacontas.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-ID";
  public static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String supplied = request.getHeader(HEADER);
    String traceId =
        supplied != null && supplied.matches("[A-Za-z0-9._-]{1,64}")
            ? supplied
            : UUID.randomUUID().toString();
    MDC.put(MDC_KEY, traceId);
    response.setHeader(HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
