# Interface web do CasaContas

Aplicação React 19, TypeScript estrito e Vite. A interface cobre autenticação, entrada/criação de casa, painel, despesas, rateios, confirmações, cobrança e moradores.

## Desenvolvimento

```bash
npm ci
npm run dev
```

Por padrão, o cliente usa a própria origem. Para chamar uma API separada:

```bash
VITE_API_URL=http://localhost:8080 npm run dev
```

No Windows, defina `VITE_API_URL` no terminal ou em um arquivo `.env.local` não versionado.

## Qualidade

```bash
npm run verify
npm run test:e2e
```

`verify` executa ESLint, Prettier, Vitest e build. O Vitest descobre somente `src/**/*.test.{ts,tsx}`; os testes em `e2e/` pertencem exclusivamente ao Playwright.

## Organização

- `src/pages`: jornadas e telas;
- `src/components`: componentes reutilizáveis;
- `src/state`: sessão e casa ativa;
- `src/lib`: cliente HTTP, formatação e divisão em centavos;
- `src/test`: preparação do MSW e Testing Library;
- `e2e`: jornada responsiva desktop/celular.
