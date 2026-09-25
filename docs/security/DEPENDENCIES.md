# Auditoria de dependências — 2026-09-25

A execução [36090196781](https://github.com/Guilhermegg-06/CasaContas/actions/runs/36090196781)
comprovou que o feed oficial da NVD permite executar o Dependency-Check 13.0.0
sem uma chave de API. O limiar permanece CVSS 7; nenhuma supressão foi adicionada.

A falha seguinte foi de vulnerabilidade, não de infraestrutura: Tomcat 11.0.24
teve nove achados acima do limiar, incluindo CVE-2026-65182 e CVE-2026-65637.
O catálogo da [Apache](https://tomcat.apache.org/security-11.html) lista as
correções nas versões 11.0.25 e 11.0.26. A propriedade `tomcat.version` passa a
11.0.26, mantendo a família 11.0 e o Spring Boot existente.

Swagger UI 5.32.11 incorporava DOMPurify 3.4.12, apontado por CVE-2026-75838.
O gerenciamento da dependência WebJar passa a Swagger UI 5.32.15, versão publicada
no [Maven Central](https://repo.maven.apache.org/maven2/org/webjars/swagger-ui/5.32.15/).
Validar a auditoria novamente e testar `/api-docs` e `/docs`; não pressupor que
uma troca de versão, sozinha, comprove a correção de todos os achados.

As demais propostas do Dependabot permanecem independentes: não foram aplicadas
atualizações de major de TypeScript, router ou ferramentas sem necessidade nesta fase.
O resultado da revisão final e eventuais achados restantes ficam no registro de continuidade.
