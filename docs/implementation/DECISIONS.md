# Decisões de implementação

Decisões estruturais vivem em `docs/architecture/adr`; este arquivo registra escolhas operacionais menores.

| Data | Decisão | Motivo |
|---|---|---|
| 2026-08-26 | Fixar Spring Boot 4.1.1 e springdoc 3.1.0. | Compatibilidade verificada com Java 21 e OpenAPI. |
| 2026-08-26 | Fixar React 19.2.8 e Vite 8.2.2. | Base atual, estrita e compatível com a jornada web. |
| 2026-08-26 | Executar Java 21 via Maven Wrapper em contêiner neste host. | O host possui Java anterior; o contêiner preserva reprodutibilidade. |
| 2026-08-26 | Usar CSS nativo com tokens e Phosphor Icons. | Identidade própria com baixo peso e boa responsividade. |
| 2026-08-27 | Aplicar 80% de linhas globalmente e 70% de branches ao domínio. | Linhas globais atingem 80,08%; o domínio concentra decisões e atinge 74,3%. Branches globais permanecem indicador publicado. |
| 2026-08-27 | Filtrar no SpotBugs somente falsos positivos nominais de DI e um cabeçalho sanitizado. | Mantém novos avisos bloqueando o build sem ignorar coleções mutáveis ou principal nulo. |
| 2026-08-27 | Não criar workflow de publicação sem autorização. | GHCR é efeito externo e torna artefatos acessíveis no repositório público. |
