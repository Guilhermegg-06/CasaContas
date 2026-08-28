package br.com.casacontas.settlement.application;

import br.com.casacontas.settlement.domain.Settlement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository {

  Settlement save(Settlement settlement);

  Optional<Settlement> findById(UUID id);

  List<Settlement> findByExpense(UUID expenseId);
}
