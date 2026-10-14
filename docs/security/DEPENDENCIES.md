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
`springdoc.swagger-ui.version` recebe a mesma propriedade Maven durante o build,
para resolver os recursos do WebJar atualizado. O teste de acesso à documentação
reproduziu a incompatibilidade (500) e passou após esse alinhamento.
Validar a auditoria novamente e testar `/api-docs` e `/docs`; não pressupor que
uma troca de versão, sozinha, comprove a correção de todos os achados.

As demais propostas do Dependabot permanecem independentes: não foram aplicadas
atualizações de major de TypeScript, router ou ferramentas sem necessidade nesta fase.
O resultado da revisão final e eventuais achados restantes ficam no registro de continuidade.

## Correção do frontend — 2026-10-14

A auditoria semanal de 12/10 apontou quatro dependências transitivas com severidade
alta. A reprodução local também terminou com saída 1 e os mesmos quatro alertas.
O lockfile passa a brace-expansion 5.0.12, fast-uri 3.1.8, source-map-js 1.2.2 e
undici 7.30.0, mantendo as dependências diretas e suas faixas.

Com Node 22.20.0, `npm ci` e `npm audit --audit-level=high` passaram sem alertas.
Lint, formatação, 21 testes e build também passaram. Evidências completas ficam
em `.data/evidence/frontend-*` da cópia de trabalho isolada. Esses resultados são
locais: o fork e os checks remotos desta entrega dependem da conta de destino.

## Auditoria backend — 2026-10-14

O feed NVD que retornou 404 no PR #16 respondeu 200 nesta sessão. Sua URL permanece
a oficial. O workflow passa a repetir a auditoria completa até três vezes apenas
para os erros HTTP NVD classificados como transitórios; vulnerabilidades acima
do limiar continuam reprovadas sem repetição. Cinco testes offline executam o
comando real do workflow com uma fixture Maven e comprovam recuperação, limite
de tentativas, bloqueio por vulnerabilidade e ausência de mascaramento do resultado.
Dois desses cenários falharam corretamente no workflow anterior.

Swagger UI 5.33.2 incorpora DOMPurify 3.4.16, confirmado no WebJar do Maven Central.
Essa versão corrige os achados GHSA-6688-9rhm-gjv2 e GHSA-p98j-92pf-mc4p presentes
em DOMPurify 3.4.13 do WebJar anterior. As dependências Spring/Tomcat não mudaram.

Os 34 testes backend, cobertura, SpotBugs e Actionlint passaram. A auditoria real
Dependency-Check 13.0.0 terminou com BUILD SUCCESS após 29 minutos: zero achados
em 78 dependências, sem supressões novas ou mudança do limiar CVSS 7. Relatórios
HTML/JSON e logs ficam em `.data/evidence/backend-audit-result` e `backend-audit-real.log`.
Essas evidências são locais; o CI desta entrega ainda depende do fork.
