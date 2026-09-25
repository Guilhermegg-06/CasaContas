import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeftIcon, CheckIcon } from '@phosphor-icons/react'
import { useMemo, useState, type SyntheticEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError, apiRequest, idempotencyKey } from '../lib/api'
import { formatCurrency } from '../lib/format'
import { customSplitMatches, splitEqually } from '../lib/split'
import { useHousehold } from '../state/HouseholdContext'
import type { Expense, Member, SplitType } from '../types'

const categories = [
  'Moradia',
  'Alimentação',
  'Energia',
  'Internet',
  'Limpeza',
  'Transporte',
  'Outros',
]

export function ExpenseFormPage() {
  const { expenseId } = useParams()
  const id = expenseId ?? ''
  const { activeHousehold } = useHousehold()
  const householdId = activeHousehold?.id ?? ''
  const members = useQuery({
    queryKey: ['members', householdId],
    queryFn: () => apiRequest<Member[]>(`/api/v1/households/${householdId}/members`),
    enabled: Boolean(householdId),
  })
  const expense = useQuery({
    queryKey: ['expense', householdId, id],
    queryFn: () => apiRequest<Expense>(`/api/v1/households/${householdId}/expenses/${id}`),
    enabled: Boolean(id && householdId),
  })

  if (members.isLoading || (expenseId && expense.isLoading)) return <div className="skeleton" />
  if (members.isError || (expenseId && !expense.data))
    return <div className="inline-error">Não foi possível preparar o formulário.</div>
  return (
    <ExpenseForm
      key={expense.data?.updatedAt ?? 'new'}
      members={members.data ?? []}
      initial={expense.data}
    />
  )
}

function ExpenseForm({ members, initial }: { members: Member[]; initial?: Expense }) {
  const { activeHousehold } = useHousehold()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const householdId = activeHousehold?.id ?? ''
  const [title, setTitle] = useState(initial?.title ?? '')
  const [total, setTotal] = useState(initial ? String(initial.total) : '')
  const [category, setCategory] = useState(initial?.category ?? 'Moradia')
  const [dueDate, setDueDate] = useState(initial?.dueDate ?? new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [splitType, setSplitType] = useState<SplitType>(initial?.splitType ?? 'EQUAL')
  const [selected, setSelected] = useState<string[]>(
    initial?.shares.map((share) => share.memberId) ?? members.map((member) => member.id),
  )
  const [amounts, setAmounts] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      initial?.shares.map((share) => [share.memberId, String(share.amount)]) ?? [],
    ),
  )
  const [paidByMemberId, setPaidByMemberId] = useState(initial?.paidByMemberId ?? '')
  const [error, setError] = useState<string | null>(null)
  const equalPreview = useMemo(() => splitEqually(total || 0, selected), [total, selected])

  const save = useMutation({
    mutationFn: async () => {
      if (selected.length === 0) throw new Error('Escolha pelo menos um morador.')
      if (
        splitType === 'CUSTOM' &&
        !customSplitMatches(
          total,
          selected.map((id) => amounts[id] ?? 0),
        )
      )
        throw new Error('A soma das partes precisa ser igual ao total da despesa.')
      const body = {
        title: title.trim(),
        total,
        category,
        dueDate,
        notes: notes.trim() || null,
        splitType,
        participants: selected.map((memberId) => ({
          memberId,
          amount: splitType === 'CUSTOM' ? amounts[memberId] : undefined,
        })),
        paidByMemberId: paidByMemberId || null,
      }
      return initial
        ? apiRequest<Expense>(`/api/v1/households/${householdId}/expenses/${initial.id}`, {
            method: 'PUT',
            body: JSON.stringify(body),
          })
        : apiRequest<Expense>(`/api/v1/households/${householdId}/expenses`, {
            method: 'POST',
            headers: { 'Idempotency-Key': idempotencyKey() },
            body: JSON.stringify(body),
          })
    },
    onSuccess: async (saved) => {
      queryClient.setQueryData(['expense', householdId, saved.id], saved)
      await queryClient.invalidateQueries({ queryKey: ['expenses', householdId] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard', householdId] })
      void navigate(`/app/despesas/${saved.id}`)
    },
    onError: (saveError) =>
      setError(
        saveError instanceof ApiError || saveError instanceof Error
          ? saveError.message
          : 'Não foi possível salvar a despesa.',
      ),
  })

  function toggle(memberId: string) {
    setSelected((current) =>
      current.includes(memberId) ? current.filter((id) => id !== memberId) : [...current, memberId],
    )
  }

  function submit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    save.mutate()
  }

  return (
    <>
      <header className="page-header">
        <div>
          <Link to={initial ? `/app/despesas/${initial.id}` : '/app/despesas'} className="eyebrow">
            <ArrowLeftIcon /> Voltar para despesas
          </Link>
          <h1>{initial ? 'Editar despesa' : 'Nova despesa'}</h1>
          <p>Preencha os dados e confira a divisão antes de salvar.</p>
        </div>
      </header>
      <form className="form-card card" onSubmit={submit}>
        <section>
          <div className="form-section__title">
            <span className="form-section__number">1</span>
            <div>
              <h2>Dados da conta</h2>
              <p>O que foi comprado, quanto custou e quando vence.</p>
            </div>
          </div>
          <div className="form-grid">
            <label className="field form-grid__wide">
              <span>Título</span>
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                maxLength={120}
                placeholder="Ex.: Conta de energia"
                required
              />
            </label>
            <label className="field">
              <span>Valor total</span>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={total}
                onChange={(event) => setTotal(event.target.value)}
                placeholder="0,00"
                required
              />
            </label>
            <label className="field">
              <span>Vencimento</span>
              <input
                type="date"
                value={dueDate}
                onChange={(event) => setDueDate(event.target.value)}
                required
              />
            </label>
            <label className="field">
              <span>Categoria</span>
              <select value={category} onChange={(event) => setCategory(event.target.value)}>
                {categories.map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Quem pagou a conta?</span>
              <select
                value={paidByMemberId}
                onChange={(event) => setPaidByMemberId(event.target.value)}
                disabled={Boolean(initial)}
              >
                <option value="">Ainda não foi paga</option>
                {members.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.name || member.email}
                  </option>
                ))}
              </select>
            </label>
            <label className="field form-grid__wide">
              <span>Observação (opcional)</span>
              <textarea
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                maxLength={500}
                placeholder="Algum detalhe útil para a casa"
              />
            </label>
          </div>
        </section>

        <section className="form-section">
          <div className="form-section__title">
            <span className="form-section__number">2</span>
            <div>
              <h2>Como dividir</h2>
              <p>Escolha as pessoas e confira o valor de cada parte.</p>
            </div>
          </div>
          <div className="segmented">
            <label>
              <input
                type="radio"
                checked={splitType === 'EQUAL'}
                onChange={() => setSplitType('EQUAL')}
              />
              <span>Dividir igualmente</span>
            </label>
            <label>
              <input
                type="radio"
                checked={splitType === 'CUSTOM'}
                onChange={() => setSplitType('CUSTOM')}
              />
              <span>Definir cada valor</span>
            </label>
          </div>
          <div className="participant-list" style={{ marginTop: 16 }}>
            {members.map((member) => {
              const checked = selected.includes(member.id)
              const preview = splitType === 'EQUAL' ? equalPreview.get(member.id) : undefined
              return (
                <label className="participant-row" key={member.id}>
                  <input type="checkbox" checked={checked} onChange={() => toggle(member.id)} />
                  <span>
                    <strong>{member.name || 'Morador'}</strong>
                    <small>{member.email}</small>
                  </span>
                  {checked &&
                    (splitType === 'CUSTOM' ? (
                      <input
                        aria-label={`Parte de ${member.name}`}
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={amounts[member.id] ?? ''}
                        onChange={(event) =>
                          setAmounts((current) => ({ ...current, [member.id]: event.target.value }))
                        }
                        required
                      />
                    ) : (
                      <strong>{formatCurrency(preview ?? 0)}</strong>
                    ))}
                </label>
              )
            })}
          </div>
        </section>
        {error && (
          <div className="inline-error" role="alert">
            {error}
          </div>
        )}
        <footer className="form-footer">
          <Link className="button button--ghost" to="/app/despesas">
            Cancelar
          </Link>
          <button className="button button--primary" disabled={save.isPending}>
            <CheckIcon /> {save.isPending ? 'Salvando…' : 'Salvar despesa'}
          </button>
        </footer>
      </form>
    </>
  )
}
