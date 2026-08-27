import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CopyIcon, PlusIcon, TrashIcon, UserPlusIcon, XIcon } from '@phosphor-icons/react'
import { useState, type SyntheticEvent } from 'react'
import { apiRequest } from '../lib/api'
import { initials, roleLabel } from '../lib/format'
import { useAuth } from '../state/AuthContext'
import { useHousehold } from '../state/HouseholdContext'
import type { Member, Role } from '../types'

interface Invitation {
  id: string
  token: string
  role: Role
  expiresAt: string
}

export function MembersPage() {
  const { session } = useAuth()
  const { activeHousehold } = useHousehold()
  const queryClient = useQueryClient()
  const [inviteOpen, setInviteOpen] = useState(false)
  const householdId = activeHousehold?.id ?? ''
  const canManage = activeHousehold?.role === 'OWNER' || activeHousehold?.role === 'ADMIN'
  const members = useQuery({
    queryKey: ['members', householdId],
    queryFn: () => apiRequest<Member[]>(`/api/v1/households/${householdId}/members`),
    enabled: Boolean(householdId),
  })
  const removeMember = useMutation({
    mutationFn: (memberId: string) =>
      apiRequest<undefined>(`/api/v1/households/${householdId}/members/${memberId}`, {
        method: 'DELETE',
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['members', householdId] }),
  })

  return (
    <>
      <header className="page-header">
        <div>
          <span className="eyebrow">{activeHousehold?.name}</span>
          <h1>Moradores</h1>
          <p>Quem participa da casa e qual é o papel de cada pessoa.</p>
        </div>
        {canManage && (
          <button className="button button--primary" onClick={() => setInviteOpen(true)}>
            <PlusIcon /> Convidar pessoa
          </button>
        )}
      </header>

      {members.isLoading ? (
        <div className="member-grid">
          {Array.from({ length: 3 }, (_, index) => (
            <div className="skeleton" key={index} />
          ))}
        </div>
      ) : (
        <section className="member-grid">
          {members.data?.map((member) => (
            <article className="member-card card" key={member.id}>
              <span className="avatar">{initials(member.name)}</span>
              <h2>{member.name || 'Morador'}</h2>
              <p>{member.email || 'Conta ativa na casa'}</p>
              <footer className="member-card__footer">
                <span className="role-label">{roleLabel[member.role]}</span>
                {canManage && member.userId !== session?.user.id && member.role !== 'OWNER' && (
                  <button
                    className="icon-button"
                    aria-label={`Remover ${member.name}`}
                    onClick={() => removeMember.mutate(member.id)}
                  >
                    <TrashIcon />
                  </button>
                )}
              </footer>
            </article>
          ))}
        </section>
      )}
      {inviteOpen && <InviteModal householdId={householdId} onClose={() => setInviteOpen(false)} />}
    </>
  )
}

function InviteModal({ householdId, onClose }: { householdId: string; onClose: () => void }) {
  const [role, setRole] = useState<Role>('RESIDENT')
  const [invitation, setInvitation] = useState<Invitation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const invite = useMutation({
    mutationFn: () =>
      apiRequest<Invitation>(`/api/v1/households/${householdId}/invitations`, {
        method: 'POST',
        body: JSON.stringify({ role, validityHours: 72 }),
      }),
    onSuccess: setInvitation,
    onError: () => setError('Não foi possível gerar o convite.'),
  })

  function submit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault()
    invite.mutate()
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="invite-title">
        <div className="panel__header">
          <div>
            <span className="eyebrow">Novo morador</span>
            <h2 id="invite-title">Gerar convite</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Fechar">
            <XIcon />
          </button>
        </div>
        {invitation ? (
          <div className="form-stack">
            <p className="muted">
              Envie este código à pessoa. Ele vale por 72 horas e só pode ser usado uma vez.
            </p>
            <div className="invite-token">{invitation.token}</div>
            <button
              className="button button--primary button--wide"
              onClick={() => void navigator.clipboard.writeText(invitation.token)}
            >
              <CopyIcon /> Copiar convite
            </button>
          </div>
        ) : (
          <form className="form-stack" onSubmit={submit}>
            <label className="field">
              <span>Papel na casa</span>
              <select value={role} onChange={(event) => setRole(event.target.value as Role)}>
                <option value="RESIDENT">Morador</option>
                <option value="ADMIN">Administrador</option>
              </select>
            </label>
            {error && (
              <div className="inline-error" role="alert">
                {error}
              </div>
            )}
            <button className="button button--primary button--wide" disabled={invite.isPending}>
              <UserPlusIcon /> {invite.isPending ? 'Gerando…' : 'Gerar convite'}
            </button>
          </form>
        )}
      </section>
    </div>
  )
}
