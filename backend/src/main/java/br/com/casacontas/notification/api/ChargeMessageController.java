package br.com.casacontas.notification.api;

import br.com.casacontas.notification.application.ChargeMessageService;
import br.com.casacontas.notification.application.ChargeMessageService.ChargeMessage;
import br.com.casacontas.shared.application.CurrentUser;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/expenses/{expenseId}/charge-message")
public class ChargeMessageController {

  private final ChargeMessageService service;
  private final CurrentUser currentUser;

  public ChargeMessageController(ChargeMessageService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping
  ChargeMessage generate(
      @PathVariable UUID householdId, @PathVariable UUID expenseId, @RequestParam UUID shareId) {
    return service.generate(currentUser.id(), householdId, expenseId, shareId);
  }
}
