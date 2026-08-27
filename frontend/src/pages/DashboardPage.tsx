import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  CalendarDotsIcon,
  CheckCircleIcon,
  CoinsIcon,
  HandCoinsIcon,
  ReceiptIcon,
  WarningCircleIcon,
} from '@phosphor-icons/react'
import { useState } from 'react'
import { Link } from 'react-router'
import { apiRequest } from '../lib/api'
import { currentMonth, formatCurrency, formatMonth, shiftMonth } from '../lib/format'
import { useHousehold } from '../state/HouseholdContext'
import type { MonthlyDashboard, PageResult, Expense } from '../types'

export function DashboardPage() {
  const { activeHousehold } = useHousehold()
  const [month, setMonth] = useState(currentMonth)
  const householdId = activeHousehold?.id ?? ''
  const dashboard = useQuery({
    queryKey: ['dashboard', householdId, month],
    queryFn: () =>
      apiRequest<MonthlyDashboard>(`/api/v1/households/${householdId}/dashboard?month=${month}`),
    enabled: Boolean(householdId),
  })
  const recentExpenses = useQuery({
    queryKey: ['expenses', householdId, month, 'preview'],
    queryFn: () =>
      apiRequest<PageResult<Expense>>(
        `/api/v1/households/${householdId}/expenses?month=${month}&size=4&sort=dueDate&direction=desc`,
      ),
    enabled: Boolean(householdId),
  })
  const data = dashboard.data

  return (
    <>
      <header className="page-header">
        <div>
          <span className="eyebrow">{activeHousehold?.name}</span>
          <h1>Visão geral</h1>
          <p>O retrato financeiro da casa neste mês.</p>
        </div>
        <div className="month-control" aria-label="Escolher mês">
          <button
            className="icon-button"
            onClick={() => setMonth((value) => shiftMonth(value, -1))}
            aria-label="Mês anterior"
          >
            <ArrowLeftIcon />
          </button>
          <strong>{formatMonth(month)}</strong>
          <button
            className="icon-button"
            onClick={() => setMonth((value) => shiftMonth(value, 1))}
            aria-label="Próximo mês"
          >
            <ArrowRightIcon />
          </button>
        </div>
      </header>

      {dashboard.isLoading ? (
        <div className="summary-grid">
          {Array.from({ length: 4 }, (_, index) => (
            <div className="skeleton" key={index} />
          ))}
        </div>
      ) : dashboard.isError || !data ? (
        <div className="inline-error" role="alert">
          Não foi possível carregar o resumo deste mês.
        </div>
      ) : (
        <>
          <section className="summary-grid" aria-label="Resumo mensal">
            <SummaryCard
              hero
              icon={ReceiptIcon}
              label="Total da casa"
              value={data.householdTotal}
              hint="todas as despesas do mês"
            />
            <SummaryCard
              icon={CheckCircleIcon}
              label="Já pago"
              value={data.paidTotal}
              hint="partes cobertas ou quitadas"
            />
            <SummaryCard
              icon={CalendarDotsIcon}
              label="Falta pagar"
              value={data.pendingTotal}
              hint="compromissos pendentes"
            />
            <SummaryCard
              icon={HandCoinsIcon}
              label="Você recebe"
              value={data.iReceive}
              hint="reembolsos em aberto"
            />
          </section>

          <section className="dashboard-grid">
            <div className="panel card">
              <div className="panel__header">
                <h2>Seu balanço no mês</h2>
                <CoinsIcon color="var(--coral)" size={22} />
              </div>
              <div className="financial-bars">
                <FinancialBar
                  label="Você já pagou"
                  value={data.iPaid}
                  total={data.householdTotal}
                />
                <FinancialBar
                  label="Você ainda deve"
                  value={data.iOwe}
                  total={data.householdTotal}
                  tone="coral"
                />
                <FinancialBar
                  label="Você tem a receber"
                  value={data.iReceive}
                  total={data.householdTotal}
                  tone="gold"
                />
              </div>
            </div>
            <div className="panel card alert-card">
              <div>
                <span className="alert-card__icon">
                  <WarningCircleIcon size={22} weight="fill" />
                </span>
                <strong>{data.overdueExpenses}</strong>
                <p>
                  {data.overdueExpenses === 1
                    ? 'despesa vencida precisa de atenção.'
                    : 'despesas vencidas precisam de atenção.'}
                </p>
              </div>
              <Link
                to={`/app/despesas?month=${month}&status=OVERDUE`}
                className="button button--small"
              >
                Ver pendências
              </Link>
            </div>
          </section>

          <section className="panel card" style={{ marginTop: 16 }}>
            <div className="panel__header">
              <h2>Últimas despesas</h2>
              <Link to="/app/despesas">Ver todas</Link>
            </div>
            {recentExpenses.data?.content.length ? (
              <div className="expense-list">
                {recentExpenses.data.content.map((expense) => (
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
                    <span className={`status status--${expense.status.toLowerCase()}`}>
                      {expense.status === 'SETTLED'
                        ? 'Quitada'
                        : expense.status === 'OVERDUE'
                          ? 'Vencida'
                          : 'Pendente'}
                    </span>
                    <ArrowRightIcon />
                  </Link>
                ))}
              </div>
            ) : (
              <p className="muted">Nenhuma despesa cadastrada neste mês.</p>
            )}
          </section>
        </>
      )}
    </>
  )
}

function SummaryCard({
  label,
  value,
  hint,
  icon: Icon,
  hero = false,
}: {
  label: string
  value: number
  hint: string
  icon: typeof ReceiptIcon
  hero?: boolean
}) {
  return (
    <article className={`summary-card card${hero ? ' summary-card--hero' : ''}`}>
      <span className="summary-card__label">
        <Icon size={17} />
        {label}
      </span>
      <strong className="summary-card__value">{formatCurrency(value)}</strong>
      <span className="summary-card__hint">{hint}</span>
    </article>
  )
}

function FinancialBar({
  label,
  value,
  total,
  tone,
}: {
  label: string
  value: number
  total: number
  tone?: 'coral' | 'gold'
}) {
  const percentage = total > 0 ? Math.min(100, Math.round((value / total) * 100)) : 0
  return (
    <div className="financial-bar">
      <div className="financial-bar__head">
        <span>{label}</span>
        <strong>{formatCurrency(value)}</strong>
      </div>
      <div className="financial-bar__track">
        <div
          className={`financial-bar__fill${tone ? ` financial-bar__fill--${tone}` : ''}`}
          style={{ width: `${String(percentage)}%` }}
        />
      </div>
    </div>
  )
}
