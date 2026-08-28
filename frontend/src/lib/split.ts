export function toCents(value: number | string) {
  return Math.round(Number(value) * 100)
}

export function splitEqually(total: number | string, participantIds: string[]) {
  if (participantIds.length === 0) return new Map<string, number>()
  const totalCents = toCents(total)
  const base = Math.floor(totalCents / participantIds.length)
  let remainder = totalCents % participantIds.length
  const result = new Map<string, number>()
  participantIds.forEach((id) => {
    const cents = base + (remainder > 0 ? 1 : 0)
    remainder = Math.max(0, remainder - 1)
    result.set(id, cents / 100)
  })
  return result
}

export function customSplitMatches(total: number | string, amounts: (number | string)[]) {
  return amounts.reduce<number>((sum, amount) => sum + toCents(amount), 0) === toCents(total)
}
