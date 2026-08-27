import { HouseLineIcon as HouseLine } from '@phosphor-icons/react'
import { Link } from 'react-router'

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className="brand" to="/" aria-label="CasaContas — início">
      <span className="brand__mark" aria-hidden="true">
        <HouseLine weight="fill" size={compact ? 18 : 22} />
      </span>
      <span>CasaContas</span>
    </Link>
  )
}
