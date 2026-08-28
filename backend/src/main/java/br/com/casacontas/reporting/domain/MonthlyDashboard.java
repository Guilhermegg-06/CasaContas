package br.com.casacontas.reporting.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyDashboard(
    YearMonth month,
    BigDecimal householdTotal,
    BigDecimal paidTotal,
    BigDecimal pendingTotal,
    long overdueExpenses,
    BigDecimal iOwe,
    BigDecimal iPaid,
    BigDecimal iReceive) {}
