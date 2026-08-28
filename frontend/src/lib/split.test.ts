import { describe, expect, it } from 'vitest'
import { customSplitMatches, splitEqually } from './split'

describe('rateio em centavos', () => {
  it('distribui o centavo restante de forma determinística', () => {
    const result = splitEqually('100.00', ['ana', 'bia', 'caio'])

    expect([...result.values()]).toEqual([33.34, 33.33, 33.33])
  })

  it('compara a soma personalizada sem erro de ponto flutuante', () => {
    expect(customSplitMatches('0.30', ['0.10', '0.20'])).toBe(true)
    expect(customSplitMatches('100.00', ['60.00', '39.99'])).toBe(false)
  })
})
