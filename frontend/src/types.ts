export type Role = 'OWNER' | 'ADMIN' | 'MEMBER'
export type SplitType = 'EQUAL' | 'CUSTOM'
export type ExpenseStatus = 'PENDING' | 'OVERDUE' | 'SETTLED' | 'CANCELLED'
export type ShareStatus = 'PENDING' | 'COVERED' | 'SETTLED'

export interface User {
  id: string
  name: string
  email: string
}

export interface AuthSession {
  tokenType: 'Bearer'
  accessToken: string
  expiresIn: number
  refreshToken: string
  user: User
}

export interface Household {
  id: string
  name: string
  timezone: string
  currency: 'BRL'
  role: Role
}

export interface Member {
  id: string
  userId: string
  name: string
  email: string
  role: Role
  joinedAt: string
}

export interface MonthlyDashboard {
  month: string
  householdTotal: number
  paidTotal: number
  pendingTotal: number
  overdueExpenses: number
  iOwe: number
  iPaid: number
  iReceive: number
}

export interface ExpenseShare {
  id: string
  memberId: string
  amount: number
  status: ShareStatus
  settledAt: string | null
}

export interface Settlement {
  id: string
  shareId: string | null
  payerMemberId: string
  recipientMemberId: string | null
  amount: number
  type: 'PRIMARY_PAYMENT' | 'REIMBURSEMENT'
  occurredAt: string
}

export interface AuditEvent {
  id: string
  eventType: string
  details: string
  occurredAt: string
}

export interface Expense {
  id: string
  householdId: string
  createdByMemberId: string
  paidByMemberId: string | null
  title: string
  total: number
  category: string
  dueDate: string
  notes: string | null
  splitType: SplitType
  currency: 'BRL'
  status: ExpenseStatus
  createdAt: string
  updatedAt: string
  shares: ExpenseShare[]
  settlements: Settlement[]
  auditEvents: AuditEvent[]
}

export interface PageResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  traceId: string
  errors: FieldError[]
}
