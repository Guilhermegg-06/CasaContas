import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeftIcon,
  CheckCircleIcon,
  CopyIcon,
  PencilSimpleIcon,
  ReceiptIcon,
  TrashIcon,
} from '@phosphor-icons/react'
import { Link, useNavigate, useParams } from 'react-router'
import { StatusBadge } from '../components/StatusBadge'
import { apiRequest, idempotencyKey } from '../lib/api'
import { formatCurrency, formatDate, formatInstant, initials } from '../lib/format'
import { useAuth } from '../state/AuthContext'
import { useHousehold } from '../state/HouseholdContext'
import type { Expense, Member } from '../types'

interface ChargeMessage {
  message: string
}

export function ExpenseDetailPage() {
  const { expenseId } = useParams()
  const id = expenseId ?? ''
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { session } = useAuth()
  const { activeHousehold } = useHousehold()
  const householdId = activeHousehold?.id ?? ''
  const expense = useQuery({
    queryKey: ['expense', householdId, id],
    queryFn: () => apiRequest<Expense>(`/api/v1/households/${householdId}/expenses/${id}`),
    enabled: Boolean(householdId && id),
  })
  const members = useQuery({
    queryKey: ['members', householdId],
    queryFn: () => apiRequest<Member[]>(`/api/v1/households/${householdId}/members`),
    enabled: Boolean(householdId),
  })
  const currentMember = members.data?.find((member) => member.userId === session?.user.id)
  const canManage = activeHousehold?.role === 'OWNER' || activeHousehold?.role === 'ADMIN'

  async function refresh() {
    await queryClient.invalidateQueries({ queryKey: ['expense', householdId, id] })
    await queryClient.invalidateQueries({ queryKey: ['expenses', householdId] })
    await queryClient.invalidateQueries({ queryKey: ['dashboard', householdId] })
  }
  const primaryPayment = useMutation({
    mutationFn: (payerMemberId: string) =>
      apiRequest(`/api/v1/households/${householdId}/expenses/${id}/primary-payment`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({ payerMemberId }),
      }),
    onSuccess: refresh,
  })
  const settleShare = useMutation({
    mutationFn: (shareId: string) =>
      apiRequest(`/api/v1/households/${householdId}/expenses/${id}/shares/${shareId}/settlements`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey() },
      }),
    onSuccess: refresh,
  })
  const cancel = useMutation({
    mutationFn: () =>
      apiRequest<undefined>(`/api/v1/households/${householdId}/expenses/${id}`, {
        method: 'DELETE',
      }),
    onSuccess: async () => {
      await refresh()
      void navigate('/app/despesas')
    },
  })
  const charge = useMutation({
    mutationFn: (shareId: string) =>
      apiRequest<ChargeMessage>(
        `/api/v1/households/${householdId}/expenses/${id}/charge-message?shareId=${shareId}`,
      ),
    onSuccess: (result) => navigator.clipboard.writeText(result.message),
  })

  if (expense.isLoading || members.isLoading) return <div className="skeleton" />
  if (expense.isError || !expense.data)
    return <div className="inline-error">Despesa não encontrada ou sem acesso.</div>
  const data = expense.data
  const memberById = new Map(members.data?.map((member) => [member.id, member]) ?? [])
  const canEdit = data.status === 'PENDING' && data.settlements.length === 0

  return (
    <>
      <header className="page-header">
        <div>
          <Link to="/app/despesas" className="eyebrow">
            <ArrowLeftIcon /> Voltar para despesas
          </Link>
          <h1>Detalhe da despesa</h1>
        </div>
        <div className="page-actions">
          {canEdit && (
            <Link className="button" to={`/app/despesas/${data.id}/editar`}>
              <PencilSimpleIcon /> Editar
            </Link>
          )}
          {canManage && data.status !== 'CANCELLED' && (
            <button
              className="button button--danger"
              onClick={() => {
                if (window.confirm('Cancelar esta despesa? O histórico será preservado.'))
                  cancel.mutate()
              }}
            >
              <TrashIcon /> Cancelar
            </button>
          )}
        </div>
      </header>
      <section className="detail-layout">
        <div>
          <article className="detail-hero card">
            <div className="detail-hero__top">
              <div>
                <span className="eyebrow">{data.category}</span>
                <h1>{data.title}</h1>
              </div>
              <StatusBadge status={data.status} />
            </div>
            <strong className="detail-amount">{formatCurrency(data.total)}</strong>
            {data.notes && <p className="muted">{data.notes}</p>}
            <div className="detail-meta">
              <div>
                <span>Vencimento</span>
                <strong>{formatDate(data.dueDate)}</strong>
              </div>
              <div>
                <span>Divisão</span>
                <strong>
                  {data.splitType === 'EQUAL' ? 'Partes iguais' : 'Valores definidos'}
                </strong>
              </div>
              <div>
                <span>Pagamento principal</span>
                <strong>
                  {data.paidByMemberId
                    ? (memberById.get(data.paidByMemberId)?.name ?? 'Registrado')
                    : 'Ainda não pago'}
                </strong>
              </div>
            </div>
            {!data.paidByMemberId && data.status !== 'CANCELLED' && (
              <div className="form-section">
                <label className="field">
                  <span>Registrar quem pagou a conta</span>
                  <select
                    defaultValue=""
                    onChange={(event) => {
                      if (event.target.value) primaryPayment.mutate(event.target.value)
                    }}
                    disabled={primaryPayment.isPending}
                  >
                    <option value="">Escolha uma pessoa</option>
                    {members.data?.map((member) => (
                      <option key={member.id} value={member.id}>
                        {member.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            )}
          </article>
          <section className="panel card" style={{ marginTop: 16 }}>
            <div className="panel__header">
              <h2>Partes da despesa</h2>
              <ReceiptIcon />
            </div>
            <div className="share-list">
              {data.shares.map((share) => {
                const member = memberById.get(share.memberId)
                const canSettle =
                  share.status === 'PENDING' && (canManage || share.memberId === currentMember?.id)
                return (
                  <div className="share-row" key={share.id}>
                    <span className="avatar">{initials(member?.name)}</span>
                    <span className="share-row__person">
                      <strong>{member?.name ?? 'Morador'}</strong>
                      <small>{formatCurrency(share.amount)}</small>
                    </span>
                    <StatusBadge status={share.status} />
                    {canSettle && (
                      <button
                        className="button button--small"
                        onClick={() => settleShare.mutate(share.id)}
                        disabled={settleShare.isPending}
                      >
                        <CheckCircleIcon /> Confirmar
                      </button>
                    )}
                    {share.status === 'PENDING' && data.paidByMemberId && (
                      <button
                        className="icon-button"
                        aria-label={`Copiar cobrança para ${member?.name ?? 'morador'}`}
                        onClick={() => charge.mutate(share.id)}
                      >
                        <CopyIcon />
                      </button>
                    )}
                  </div>
                )
              })}
            </div>
          </section>
        </div>
        <aside className="panel card">
          <div className="panel__header">
            <h2>Histórico</h2>
          </div>
          <div className="timeline">
            {data.auditEvents.length === 0 && data.settlements.length === 0 ? (
              <div className="timeline__item">
                <span className="timeline__dot" />
                <span>
                  <strong>Despesa criada</strong>
                  <small>{formatInstant(data.createdAt)}</small>
                </span>
              </div>
            ) : (
              <>
                {data.auditEvents.map((event) => (
                  <div className="timeline__item" key={event.id}>
                    <span className="timeline__dot" />
                    <span>
                      <strong>{event.eventType.replaceAll('_', ' ')}</strong>
                      <small>{formatInstant(event.occurredAt)}</small>
                    </span>
                  </div>
                ))}
                {data.settlements.map((settlement) => (
                  <div className="timeline__item" key={settlement.id}>
                    <span className="timeline__dot" />
                    <span>
                      <strong>
                        {settlement.type === 'PRIMARY_PAYMENT'
                          ? 'Pagamento principal'
                          : 'Reembolso confirmado'}
                      </strong>
                      <small>
                        {formatCurrency(settlement.amount)} · {formatInstant(settlement.occurredAt)}
                      </small>
                    </span>
                  </div>
                ))}
              </>
            )}
          </div>
        </aside>
      </section>
    </>
  )
}
