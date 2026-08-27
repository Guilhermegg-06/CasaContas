package br.com.casacontas.expense.application;

import java.time.YearMonth;
import java.util.UUID;

public record ExpenseFilter(
    UUID householdId,
    YearMonth month,
    String category,
    String status,
    UUID memberId,
    int page,
    int size,
    String sort,
    boolean ascending) {}
