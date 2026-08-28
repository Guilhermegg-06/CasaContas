package br.com.casacontas.shared.application;

import br.com.casacontas.shared.domain.AuditEvent;
import java.util.List;
import java.util.UUID;

public interface AuditPort {

  void append(AuditEvent event);

  List<AuditEvent> findByExpense(UUID expenseId);
}
