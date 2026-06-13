import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AddItemModal } from './AddItemModal'
import { inventoryApi } from '../api/inventoryApi'

vi.mock('../api/inventoryApi', () => ({
  inventoryApi: {
    getItems: vi.fn(),
    updateQuantity: vi.fn(),
    addItem: vi.fn(),
  },
}))

function renderModal(props: Partial<{ isOpen: boolean; onClose: () => void; houseId: string }> = {}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  const onClose = props.onClose ?? vi.fn()
  return {
    onClose,
    ...render(
      <QueryClientProvider client={queryClient}>
        <AddItemModal isOpen={props.isOpen ?? true} onClose={onClose} houseId={props.houseId ?? 'demo'} />
      </QueryClientProvider>,
    ),
  }
}

describe('AddItemModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders all 5 fields when isOpen is true', () => {
    renderModal()
    expect(screen.getByLabelText(/nome/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/categoria/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/nível de estoque/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/validade/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/notas/i)).toBeInTheDocument()
  })

  it('renders nothing when isOpen is false', () => {
    renderModal({ isOpen: false })
    expect(screen.queryByLabelText(/nome/i)).not.toBeInTheDocument()
  })

  it('calls onClose when Cancelar is clicked', async () => {
    const { onClose } = renderModal()
    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('calls onClose when backdrop is clicked', async () => {
    const { onClose } = renderModal()
    fireEvent.click(screen.getByTestId('modal-backdrop'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('calls onClose when ✕ button is clicked', async () => {
    const { onClose } = renderModal()
    await userEvent.click(screen.getByRole('button', { name: /fechar/i }))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('shows inline error and does not call API when name is empty', async () => {
    vi.mocked(inventoryApi.addItem).mockResolvedValue({} as any)
    renderModal()
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))
    expect(screen.getByText(/nome é obrigatório/i)).toBeInTheDocument()
    expect(inventoryApi.addItem).not.toHaveBeenCalled()
  })

  it('calls inventoryApi.addItem and then onClose on successful submit', async () => {
    const mockItem = {
      id: 'aaa-111', houseId: 'demo', name: 'Arroz', category: 'FOOD',
      quantityLevel: 'ENOUGH', expiryDate: null, notes: null,
      createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    }
    vi.mocked(inventoryApi.addItem).mockResolvedValue(mockItem as any)
    const { onClose } = renderModal()

    await userEvent.type(screen.getByLabelText(/nome/i), 'Arroz')
    await userEvent.selectOptions(screen.getByLabelText(/categoria/i), 'FOOD')
    await userEvent.selectOptions(screen.getByLabelText(/nível de estoque/i), 'ENOUGH')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => expect(inventoryApi.addItem).toHaveBeenCalledWith(
      'demo',
      expect.objectContaining({ houseId: 'demo', name: 'Arroz', category: 'FOOD', quantityLevel: 'ENOUGH' }),
    ))
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
  })

  it('disables Salvar and shows Salvando... while mutation is pending', async () => {
    vi.mocked(inventoryApi.addItem).mockImplementation(() => new Promise(() => {}))
    renderModal()

    await userEvent.type(screen.getByLabelText(/nome/i), 'Arroz')
    await userEvent.selectOptions(screen.getByLabelText(/categoria/i), 'FOOD')
    await userEvent.selectOptions(screen.getByLabelText(/nível de estoque/i), 'ENOUGH')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    expect(screen.getByRole('button', { name: /salvando/i })).toBeDisabled()
  })

  it('shows error banner when API call fails', async () => {
    vi.mocked(inventoryApi.addItem).mockRejectedValue(new Error('Server error'))
    renderModal()

    await userEvent.type(screen.getByLabelText(/nome/i), 'Arroz')
    await userEvent.selectOptions(screen.getByLabelText(/categoria/i), 'FOOD')
    await userEvent.selectOptions(screen.getByLabelText(/nível de estoque/i), 'ENOUGH')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/não foi possível salvar/i),
    )
  })
})
