# ADR 0003 — Dinheiro em centavos e tempo em UTC

- Status: aceito
- Data: 2026-08-27

## Contexto

Arredondamento binário e fusos implícitos causariam divergências em rateios e vencimentos.

## Decisão

Representar dinheiro com `BigDecimal` e `NUMERIC(19,2)`, sempre em BRL no MVP. Na divisão igual, distribuir centavos restantes de forma determinística. Persistir instantes em UTC, datas de vencimento como `LocalDate` e o identificador IANA do fuso em cada casa.

## Consequências

A soma das partes sempre coincide com o total. A interface formata valores em pt-BR e usa o fuso da casa sem alterar o instante técnico persistido.
