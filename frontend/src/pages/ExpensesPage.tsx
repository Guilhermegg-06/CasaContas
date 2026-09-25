import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  CaretRightIcon,
  PlusIcon,
  ReceiptIcon,
} from '@phosphor-icons/react'
import { Link, useSearchParams } from 'react-router'
import { StatusBadge } from '../components/StatusBadge'
import { apiRequest } from '../lib/api'
import { currentMonth, formatCurrency, formatDate } from '../lib/format'
import { useHousehold } from '../state/HouseholdContext'
import type { Expense, Member, PageResult } from '../types'

const categories = [
  'Moradia',
  'Alimentação',
  'Energia',
  'Internet',
  'Limpeza',
  'Transporte',
  'Outros',
]

export function ExpensesPage() {
  const { activeHousehold } = useHousehold()
  const [params, setParams] = useSearchParams()
  const householdId = activeHousehold?.id ?? ''
  const month = params.get('month') ?? currentMonth(activeHousehold?.timezone)
  const status = params.get('status') ?? ''
  const category = params.get('category') ?? ''
  const memberId = params.get('memberId') ?? ''
  const page = Number(params.get('page') ?? 0)
  const query = new URLSearchParams({ month, page: String(page), size: '20' })
  if (status) query.set('status', status)
  if (category) query.set('category', category)
  if (memberId) query.set('memberId', memberId)

  const expenses = useQuery({
    queryKey: ['expenses', householdId, query.toString()],
    queryFn: () =>
      apiRequest<PageResult<Expense>>(`/api/v1/households/${householdId}/expenses?${query}`),
    enabled: Boolean(householdId),
  })
  const members = useQuery({
    queryKey: ['members', householdId],
    queryFn: () => apiRequest<Member[]>(`/api/v1/households/${householdId}/members`),
    enabled: Boolean(householdId),
  })

  function updateParam(key: string, value: string) {
    setParams((current) => {
      const next = new URLSearchParams(current)
      if (value) next.set(key, value)
      else next.delete(key)
      if (key !== 'page') next.delete('page')
      return next
    })
  }

  return (
    <>
      <header className="page-header">
        <div>
          <span className="eyebrow">Histórico da casa</span>
          <h1>Despesas</h1>
          <p>Encontre contas, acompanhe estados e confira cada divisão.</p>
        </div>
        <Link className="button button--primary" to="/app/despesas/nova">
          <PlusIcon /> Nova despesa
        </Link>
      </header>

      <section className="toolbar card" aria-label="Filtros das despesas">
        <label className="field">
          <span>Mês</span>
          <input
            type="month"
            value={month}
            onChange={(event) => updateParam('month', event.target.value)}
          />
        </label>
        <label className="field">
          <span>Categoria</span>
          <select
            value={category}
            onChange={(event) => updateParam('category', event.target.value)}
          >
            <option value="">Todas</option>
            {categories.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Situação</span>
          <select value={status} onChange={(event) => updateParam('status', event.target.value)}>
            <option value="">Todas</option>
            <option value="PENDING">Pendentes</option>
            <option value="OVERDUE">Vencidas</option>
            <option value="SETTLED">Quitadas</option>
            <option value="CANCELLED">Canceladas</option>
          </select>
        </label>
        <label className="field">
          <span>Morador</span>
          <select
            value={memberId}
            onChange={(event) => updateParam('memberId', event.target.value)}
          >
            <option value="">Todos</option>
            {members.data?.map((member) => (
              <option key={member.id} value={member.id}>
                {member.name || member.email}
              </option>
            ))}
          </select>
        </label>
      </section>

      {expenses.isLoading ? (
        <div className="skeleton" />
      ) : expenses.isError ? (
        <div className="inline-error" role="alert">
          Não foi possível carregar as despesas.
        </div>
      ) : !expenses.data?.content.length ? (
        <section className="empty-state card">
          <div>
            <span className="empty-state__icon">
              <ReceiptIcon size={28} />
            </span>
            <h2>Nenhuma despesa por aqui</h2>
            <p>Altere os filtros ou registre a primeira conta deste período.</p>
            <Link className="button button--primary" to="/app/despesas/nova">
              <PlusIcon /> Registrar despesa
            </Link>
          </div>
        </section>
      ) : (
        <>
          <section className="expense-list card">
            {expenses.data.content.map((expense) => (
              <Link className="expense-row" to={`/app/despesas/${expense.id}`} key={expense.id}>
                <span className="expense-row__title">
                  <span className="category-icon">
                    <ReceiptIcon />
                  </span>
                  <span>
                    <strong>{expense.title}</strong>
                    <small>{expense.category}</small>
                  </span>
                </span>
                <span className="expense-row__amount">{formatCurrency(expense.total)}</span>
                <span className="expense-row__date">vence {formatDate(expense.dueDate)}</span>
                <StatusBadge status={expense.status} />
                <CaretRightIcon color="var(--ink-soft)" />
              </Link>
            ))}
          </section>
          <nav className="pagination" aria-label="Paginação">
            <button
              className="icon-button"
              disabled={page === 0}
              onClick={() => updateParam('page', String(page - 1))}
              aria-label="Página anterior"
            >
              <ArrowLeftIcon />
            </button>
            <span>
              Página {page + 1} de {Math.max(1, expenses.data.totalPages)}
            </span>
            <button
              className="icon-button"
              disabled={page + 1 >= expenses.data.totalPages}
              onClick={() => updateParam('page', String(page + 1))}
              aria-label="Próxima página"
            >
              <ArrowRightIcon />
            </button>
          </nav>
        </>
      )}
    </>
  )
}
