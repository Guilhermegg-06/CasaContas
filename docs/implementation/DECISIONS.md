# Decisões de Implementação

Decisões estruturais vivem em ADRs; este arquivo registra escolhas operacionais menores.

| Data | Decisão | Motivo |
|---|---|---|
| 2026-08-26 | Fixar Spring Boot 4.1.1 e springdoc 3.1.0. | Versões estáveis atuais e compatíveis, confirmadas nas documentações oficiais. |
| 2026-08-26 | Fixar React 19.2.8 e Vite 8.2.2. | Versões estáveis atuais confirmadas nos canais oficiais. |
| 2026-08-26 | Executar build Java 21 via Maven Wrapper dentro de contêiner neste host. | O host possui Java 17; o contêiner preserva o requisito e a reprodutibilidade. |
| 2026-08-26 | Usar CSS nativo com tokens e Phosphor Icons. | Evita peso de um design system corporativo sem sacrificar consistência e acessibilidade. |

