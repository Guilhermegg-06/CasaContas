import type { ExpenseStatus, Role, ShareStatus } from '../types'

const currencyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const monthFormatter = new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
})

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  timeZone: 'UTC',
})

export function formatCurrency(value: number | string) {
  return currencyFormatter.format(Number(value))
}

export function formatDate(value: string) {
  return dateFormatter.format(new Date(`${value}T12:00:00Z`)).replace('.', '')
}

export function formatInstant(value: string, timeZone = 'UTC') {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone,
  }).format(new Date(value))
}

export function formatMonth(value: string) {
  const [year, month] = value.split('-').map(Number)
  return monthFormatter.format(new Date(Date.UTC(year, month - 1, 1)))
}

export function currentMonth(timeZone = 'UTC') {
  const parts = new Intl.DateTimeFormat('en', {
    year: 'numeric',
    month: '2-digit',
    timeZone,
  }).formatToParts(new Date())
  const year = parts.find((part) => part.type === 'year')?.value
  const month = parts.find((part) => part.type === 'month')?.value
  if (!year || !month) throw new Error('Não foi possível calcular o mês da casa')
  return `${year}-${month}`
}

export function shiftMonth(value: string, amount: number) {
  const [year, month] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1 + amount, 1))
  return date.toISOString().slice(0, 7)
}

export function initials(name: string | null | undefined) {
  if (!name) return 'CC'
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}

export const roleLabel: Record<Role, string> = {
  OWNER: 'Proprietário',
  ADMIN: 'Administrador',
  MEMBER: 'Morador',
}

export const statusLabel: Record<ExpenseStatus | ShareStatus, string> = {
  PENDING: 'Pendente',
  OVERDUE: 'Vencida',
  SETTLED: 'Quitada',
  CANCELLED: 'Cancelada',
  COVERED: 'Coberta',
}
