# Contribuindo com o CasaContas

## Fluxo de trabalho

O projeto usa branches curtas e pull requests para `main`. Na auditoria
de 14/10/2026, a branch ainda não tinha proteção técnica. O fluxo obrigatório é
trabalhar em uma branch, exigir os checks e obter revisão antes de qualquer merge.
A configuração de proteção depende de aprovação do mantenedor. Não crie `develop`.

1. Crie uma branch como `feat/nome-curto`, `fix/nome-curto` ou `docs/nome-curto`.
2. Use Conventional Commits e mantenha cada commit coeso.
3. Execute `make verify` antes de abrir a pull request.
4. Relacione a mudança a requisitos e issues aplicáveis.
5. Aguarde os checks obrigatórios e resolva as conversas antes do merge.

Colaboradores externos podem criar um fork, trabalhar em uma branch no fork e abrir uma pull request para `Guilhermegg-06/CasaContas:main`. Mantenedores trabalham diretamente em branches curtas deste repositório.

## Trabalhar em um fork

Substitua SEU_USUARIO pela conta ou organização do fork existente:

```powershell
git clone https://github.com/SEU_USUARIO/CasaContas.git
cd CasaContas
git remote add upstream https://github.com/Guilhermegg-06/CasaContas.git
git fetch upstream
git switch -c fix/nome-da-melhoria upstream/main
```

Confira o destino de push com `git remote -v`. Publique somente a branch
de trabalho no fork e abra o PR para o original; não faça push em `main`.
Uma conta que já possui o original precisa de uma organização existente ou do
fork de outro desenvolvedor autorizado. Uma cópia sem vínculo de fork não é
apresentada como fork. Não criar identidades artificiais para essa finalidade.

## Verificação no Windows

Java 21, Node 22.20.0 e Docker são os ambientes de referência do CI.
Os verificadores abaixo usam Java/Node em contêineres, evitando depender do JDK
selecionado no IntelliJ. Execute da raiz, com Docker Desktop ativo:

```powershell
./scripts/verify-backend.ps1
node scripts/verify-frontend.mjs
```

Os comandos Maven/Node diretos e as jornadas de navegador estão no [README](README.md).
Interface com API simulada, integração Spring/PostgreSQL e jornada completa são
evidências diferentes. Registre comando, revisão, resultado e bloqueios no PR.
Não reduzir metas, ignorar testes ou desativar auditorias para obter aprovação.

## Colaboração e coautoria

Uma tarefa real disponível é ampliar a integração de permissões dos convites
(RF-011/RF-013): administrador pode convidar MEMBER; morador comum não pode
gerar convite; usuário de outra casa não pode gerar convite nessa casa;
um convite com OWNER deve ser recusado sem persistência.

Dois desenvolvedores podem dividir implementação de fixtures/cenários da API e
as verificações de permissões/persistência. Primeiro reproduzam qualquer defeito;
só alterem produção se houver falha demonstrada. Usem PostgreSQL isolado e
integrem o trabalho por PR com checks aprovados e revisão do mantenedor.

Para um commit escrito conjuntamente, obtenha o nome e o email associado ao
GitHub que cada pessoa deseja usar. Após uma linha em branco na mensagem:

```text
test: valida permissões dos convites

Co-authored-by: Nome do colaborador <email-associado-ao-github>
```

O endereço noreply do GitHub pode preservar a privacidade. Atribua apenas trabalho
efetivamente realizado; revisão ou uso do agente não são registrados como código
escrito conjuntamente por outra pessoa. Não adicionar coautoria para obter badge.
[Documentação oficial](https://docs.github.com/en/pull-requests/how-tos/commit-changes/creating-a-commit-with-multiple-authors).

## Escopo e perguntas

Somente P0 está autorizado; P1/P2 continuam no backlog. Não mudar migrations já
aplicadas, credenciais, dados reais ou variáveis de produção. Mudanças com risco,
proteções da branch e escolha da licença exigem decisão explícita do proprietário.

Use Issues para problemas reproduzíveis e PRs para mudanças concretas. Discussions
ainda não estão habilitadas; sua ativação deve responder a uma demanda real.
Perguntas de instalação, TLS, centavos e testes podem usar as referências do README
e de `docs/operations/`. Não criar perguntas, PRs ou Issues descartáveis.
O [checklist de conquistas](docs/implementation/ACHIEVEMENTS.md) acompanha evidências.

## Segurança

Não abra issue pública para vulnerabilidades. Siga `SECURITY.md`.
