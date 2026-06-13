import { vi, describe, it, expect, beforeEach } from 'vitest'
import { inventoryApi } from './inventoryApi'
import { apiClient } from '@/shared/lib/apiClient'

vi.mock('@/shared/lib/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    patch: vi.fn(),
    post: vi.fn(),
  },
}))

describe('inventoryApi.addItem', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('POSTs to /api/v1/inventory with X-House-Id header and returns data', async () => {
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
    vi.mocked(apiClient.post).mockResolvedValue({ data: mockItem })

    const payload = {
      houseId: 'demo',
      name: 'Arroz',
      category: 'FOOD' as const,
      quantityLevel: 'ENOUGH' as const,
      expiryDate: null,
      notes: null,
    }

    const result = await inventoryApi.addItem('demo', payload)

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v1/inventory',
      payload,
      { headers: { 'X-House-Id': 'demo' } },
    )
    expect(result).toEqual(mockItem)
  })
})
