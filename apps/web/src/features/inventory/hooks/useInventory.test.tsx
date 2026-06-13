import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useAddItem } from './useInventory'
import { inventoryApi } from '../api/inventoryApi'

vi.mock('../api/inventoryApi', () => ({
  inventoryApi: {
    getItems: vi.fn(),
    updateQuantity: vi.fn(),
    addItem: vi.fn(),
  },
}))

function makeWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

describe('useAddItem', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('calls inventoryApi.addItem with the provided data', async () => {
    const mockItem = {
      id: 'aaa-111',
      houseId: 'demo',
      name: 'Arroz',
      category: 'FOOD',
      quantityLevel: 'ENOUGH',
      expiryDate: null,
      notes: null,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    }
    vi.mocked(inventoryApi.addItem).mockResolvedValue(mockItem as any)

    const { result } = renderHook(() => useAddItem(), { wrapper: makeWrapper() })

    const payload = {
      houseId: 'demo',
      name: 'Arroz',
      category: 'FOOD' as const,
      quantityLevel: 'ENOUGH' as const,
      expiryDate: null,
      notes: null,
    }

    result.current.mutate(payload)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(inventoryApi.addItem).toHaveBeenCalledWith('demo', payload)
  })

  it('exposes isError when inventoryApi.addItem rejects', async () => {
    vi.mocked(inventoryApi.addItem).mockRejectedValue(new Error('Network error'))

    const { result } = renderHook(() => useAddItem(), { wrapper: makeWrapper() })

    result.current.mutate({
      houseId: 'demo',
      name: 'Arroz',
      category: 'FOOD' as const,
      quantityLevel: 'ENOUGH' as const,
      expiryDate: null,
      notes: null,
    })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
