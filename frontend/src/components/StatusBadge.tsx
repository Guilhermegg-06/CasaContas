import { statusLabel } from '../lib/format'
import type { ExpenseStatus, ShareStatus } from '../types'

export function StatusBadge({ status }: { status: ExpenseStatus | ShareStatus }) {
  return <span className={`status status--${status.toLowerCase()}`}>{statusLabel[status]}</span>
}
