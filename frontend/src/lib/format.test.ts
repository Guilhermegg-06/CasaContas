import { describe, expect, it, vi } from 'vitest'
import {
  formatCurrency,
  formatMonth,
  initials,
  shiftMonth,
  formatInstant,
  currentMonth,
} from './format'

describe('formatadores da interface', () => {
  it('usa o fuso da casa para o mês e o histórico', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-01T01:00:00Z'))
    try {
      expect(currentMonth('America/Fortaleza')).toBe('2026-09')
      expect(formatInstant('2026-10-01T01:00:00Z', 'Asia/Tokyo')).toContain('10:00')
    } finally {
      vi.useRealTimers()
    }
  })
  it('mostra dinheiro em reais e preserva os centavos', () => {
    expect(formatCurrency(33.34)).toBe('R$ 33,34')
    expect(formatCurrency('100.00')).toBe('R$ 100,00')
  })

  it('navega entre dezembro e janeiro sem perder o ano', () => {
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(formatMonth('2026-08')).toBe('agosto de 2026')
  })

  it('monta iniciais curtas para os avatares', () => {
    expect(initials('Ana Beatriz Lima')).toBe('AB')
    expect(initials(null)).toBe('CC')
  })
})
