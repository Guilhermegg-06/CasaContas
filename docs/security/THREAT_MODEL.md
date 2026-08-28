# Modelo de ameaças do MVP

## Ativos

Credenciais, tokens de sessão, vínculos entre usuários e casas, valores financeiros, histórico de pagamentos, convites e trilha de auditoria.

## Fronteiras de confiança

Navegador e rede são não confiáveis. Nginx encaminha somente HTTP; Spring Security autentica e cada caso de uso revalida participação ativa na casa. PostgreSQL é a fonte de verdade para autorização, idempotência e auditoria.

| Ameaça | Controle atual | Evidência |
|---|---|---|
| Acesso a outra casa | filtro por casa e membro ativo; recurso alheio responde como não encontrado | `CriticalFlowsIntegrationTest` |
| Roubo de senha | BCrypt custo 12; senha nunca retorna pela API | configuração de segurança |
| Roubo/reuso de token | access token de 10 min; refresh opaco, com hash e rotação | `RefreshSessionTest` e fluxo integrado |
| Convite reutilizado ou vencido | token aleatório, hash, validade e consumo único | `InvitationTest` |
| Pagamento duplicado | chave de idempotência persistida e transação | fluxo crítico integrado |
| Manipulação de centavos | `BigDecimal`, escala 2 e distribuição determinística | `SplitCalculatorTest` |
| Exclusão de histórico | cancelamento e remoção lógicos; auditoria append-only | migration e serviços |
| Injeção e entrada malformada | Bean Validation, parâmetros SQL e erro 400 padronizado | `MalformedRequestIntegrationTest` |
| Vazamento em logs | logs estruturados usam correlation ID e não registram tokens/senhas | configuração e revisão |
| Dependência vulnerável | Dependabot, `npm audit`, OWASP Dependency-Check e CodeQL | workflows |

## Riscos residuais antes de produção

- Aplicar rate limiting externo a login, registro, convite e refresh.
- Guardar segredos em cofre do ambiente e rotacionar o `JWT_SECRET` com estratégia de chaves.
- Ativar TLS, WAF, backups cifrados e retenção de logs.
- Executar DAST e teste de concorrência sob carga.
- Configurar alertas de disponibilidade, latência e falha de autenticação.
