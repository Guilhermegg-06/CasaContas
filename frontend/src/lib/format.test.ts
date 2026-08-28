import { describe, expect, it } from 'vitest'
import { formatCurrency, formatMonth, initials, shiftMonth } from './format'

describe('formatadores da interface', () => {
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
