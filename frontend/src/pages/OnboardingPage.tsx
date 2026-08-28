import { ArrowRightIcon, HouseLineIcon, TicketIcon } from '@phosphor-icons/react'
import { useState, type SyntheticEvent } from 'react'
import { ApiError } from '../lib/api'
import { useHousehold } from '../state/HouseholdContext'
import { Brand } from '../components/Brand'

export function OnboardingPage() {
  const { createHousehold, acceptInvitation } = useHousehold()
  const [mode, setMode] = useState<'choose' | 'create' | 'accept'>('choose')
  const [name, setName] = useState('')
  const [token, setToken] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (mode === 'create') {
        await createHousehold({ name: name.trim(), timezone: 'America/Fortaleza' })
      } else {
        await acceptInvitation(token.trim())
      }
    } catch (submitError) {
      setError(
        submitError instanceof ApiError ? submitError.message : 'Não foi possível continuar.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="placeholder-page">
      <nav>
        <Brand compact />
      </nav>
      <section className="onboarding">
        <div className="onboarding-card card">
          <span className="eyebrow">Primeiro passo</span>
          <h1>Qual casa vamos organizar?</h1>
          <p className="muted">Crie um novo espaço ou use o convite que alguém enviou para você.</p>

          {mode === 'choose' ? (
            <div className="onboarding-options">
              <button className="onboarding-option" onClick={() => setMode('create')}>
                <HouseLineIcon size={32} weight="duotone" />
                <h2>Criar uma casa</h2>
                <p>Você vira proprietário e pode convidar os demais moradores.</p>
              </button>
              <button className="onboarding-option" onClick={() => setMode('accept')}>
                <TicketIcon size={32} weight="duotone" />
                <h2>Usar um convite</h2>
                <p>Cole o código recebido para entrar em uma casa existente.</p>
              </button>
            </div>
          ) : (
            <form className="form-stack" onSubmit={(event) => void submit(event)}>
              {mode === 'create' ? (
                <label className="field">
                  <span>Nome da casa</span>
                  <input
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    minLength={2}
                    maxLength={100}
                    placeholder="Ex.: Apê da Praia"
                    required
                    autoFocus
                  />
                </label>
              ) : (
                <label className="field">
                  <span>Código do convite</span>
                  <textarea
                    value={token}
                    onChange={(event) => setToken(event.target.value)}
                    placeholder="Cole aqui o código completo"
                    required
                    autoFocus
                  />
                </label>
              )}
              {error && (
                <div className="inline-error" role="alert">
                  {error}
                </div>
              )}
              <div className="form-footer">
                <button
                  type="button"
                  className="button button--ghost"
                  onClick={() => setMode('choose')}
                >
                  Voltar
                </button>
                <button className="button button--primary" disabled={submitting}>
                  {submitting
                    ? 'Só um instante…'
                    : mode === 'create'
                      ? 'Criar casa'
                      : 'Entrar na casa'}
                  {!submitting && <ArrowRightIcon />}
                </button>
              </div>
            </form>
          )}
        </div>
      </section>
    </main>
  )
}
