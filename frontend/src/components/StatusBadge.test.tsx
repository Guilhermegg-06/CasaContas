import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it('traduz o estado financeiro para linguagem cotidiana', () => {
    render(<StatusBadge status="OVERDUE" />)
    expect(screen.getByText('Vencida')).toHaveClass('status--overdue')
  })
})
