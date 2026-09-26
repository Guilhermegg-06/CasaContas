import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { apiRequest } from '../lib/api'
import type { Household } from '../types'
import { useAuth } from './AuthContext'

const ACTIVE_HOUSE_KEY = 'casacontas.active-house'

interface HouseholdContextValue {
  households: Household[]
  activeHousehold: Household | null
  isLoading: boolean
  error: Error | null
  retry: () => void
  setActiveHouseholdId: (id: string) => void
  createHousehold: (input: { name: string; timezone: string }) => Promise<Household>
  acceptInvitation: (token: string) => Promise<Household>
}

const HouseholdContext = createContext<HouseholdContextValue | null>(null)

export function HouseholdProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const [activeId, setActiveId] = useState(() => localStorage.getItem(ACTIVE_HOUSE_KEY))
  const householdsQuery = useQuery({
    queryKey: ['households', session?.user.id],
    queryFn: () => apiRequest<Household[]>('/api/v1/households'),
  })
  const households = useMemo(() => householdsQuery.data ?? [], [householdsQuery.data])
  const activeHousehold =
    households.find((household) => household.id === activeId) ?? households.at(0) ?? null

  useEffect(() => {
    if (!activeHousehold) return
    localStorage.setItem(ACTIVE_HOUSE_KEY, activeHousehold.id)
  }, [activeHousehold])

  const createMutation = useMutation({
    mutationFn: (input: { name: string; timezone: string }) =>
      apiRequest<Household>('/api/v1/households', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: async (household) => {
      setActiveId(household.id)
      await queryClient.invalidateQueries({ queryKey: ['households'] })
    },
  })

  const acceptMutation = useMutation({
    mutationFn: (token: string) =>
      apiRequest<Household>('/api/v1/invitations/accept', {
        method: 'POST',
        body: JSON.stringify({ token }),
      }),
    onSuccess: async (household) => {
      setActiveId(household.id)
      await queryClient.invalidateQueries({ queryKey: ['households'] })
    },
  })

  const { isLoading, error, refetch } = householdsQuery
  const value = useMemo<HouseholdContextValue>(
    () => ({
      households,
      activeHousehold,
      isLoading,
      error,
      retry: () => {
        void refetch()
      },
      setActiveHouseholdId(id) {
        setActiveId(id)
        localStorage.setItem(ACTIVE_HOUSE_KEY, id)
      },
      createHousehold: (input) => createMutation.mutateAsync(input),
      acceptInvitation: (token) => acceptMutation.mutateAsync(token),
    }),
    [households, activeHousehold, isLoading, error, refetch, createMutation, acceptMutation],
  )

  return <HouseholdContext.Provider value={value}>{children}</HouseholdContext.Provider>
}

export function useHousehold() {
  const context = useContext(HouseholdContext)
  if (!context) throw new Error('useHousehold precisa estar dentro de HouseholdProvider')
  return context
}
