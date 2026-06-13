import { useState } from 'react'
import { z } from 'zod'
import type { Category, QuantityLevel } from '../types'
import { useAddItem } from '../hooks/useInventory'

interface AddItemModalProps {
  isOpen: boolean
  onClose: () => void
  houseId: string
}

const CATEGORIES: Category[] = ['FOOD', 'CLEANING', 'HYGIENE', 'OTHER']
const QUANTITY_LEVELS: QuantityLevel[] = ['RUNNING_OUT', 'ENOUGH', 'PLENTY']

const FormSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório').max(255),
  category: z.enum(['FOOD', 'CLEANING', 'HYGIENE', 'OTHER'] as const),
  quantityLevel: z.enum(['RUNNING_OUT', 'ENOUGH', 'PLENTY'] as const),
  expiryDate: z.string().optional(),
  notes: z.string().max(1000).optional(),
})

export function AddItemModal({ isOpen, onClose, houseId }: AddItemModalProps) {
  const [name, setName] = useState('')
  const [category, setCategory] = useState<Category>('FOOD')
  const [quantityLevel, setQuantityLevel] = useState<QuantityLevel>('ENOUGH')
  const [expiryDate, setExpiryDate] = useState('')
  const [notes, setNotes] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const { mutate, isPending, isError } = useAddItem()

  if (!isOpen) return null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const result = FormSchema.safeParse({
      name,
      category,
      quantityLevel,
      expiryDate: expiryDate || undefined,
      notes: notes || undefined,
    })
    if (!result.success) {
      const errors: Record<string, string> = {}
      result.error.issues.forEach((issue) => {
        const key = String(issue.path[0])
        if (!errors[key]) errors[key] = issue.message
      })
      setFieldErrors(errors)
      return
    }
    setFieldErrors({})
    mutate(
      {
        houseId,
        name: result.data.name,
        category: result.data.category,
        quantityLevel: result.data.quantityLevel,
        expiryDate: result.data.expiryDate ?? null,
        notes: result.data.notes ?? null,
      },
      { onSuccess: onClose },
    )
  }

  return (
    <div
      data-testid="modal-backdrop"
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 50,
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-item-title"
        onClick={(e) => e.stopPropagation()}
        style={{
          background: '#1e1e1e',
          border: '1px solid #444',
          borderRadius: 8,
          padding: 24,
          width: '100%',
          maxWidth: 440,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 id="add-item-title" style={{ margin: 0, fontSize: 18 }}>Adicionar Item</h2>
          <button type="button" aria-label="Fechar" onClick={onClose}>✕</button>
        </div>

        {isError && (
          <div role="alert" style={{ marginBottom: 12, color: '#f87171' }}>
            Não foi possível salvar o item. Tente novamente.
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 12 }}>
            <label htmlFor="name" style={{ display: 'block', marginBottom: 4 }}>Nome *</label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={255}
              style={{ width: '100%', boxSizing: 'border-box' }}
            />
            {fieldErrors.name && (
              <span style={{ color: '#f87171', fontSize: 12 }}>{fieldErrors.name}</span>
            )}
          </div>

          <div style={{ display: 'flex', gap: 12, marginBottom: 12 }}>
            <div style={{ flex: 1 }}>
              <label htmlFor="category" style={{ display: 'block', marginBottom: 4 }}>Categoria *</label>
              <select
                id="category"
                value={category}
                onChange={(e) => setCategory(e.target.value as Category)}
                style={{ width: '100%' }}
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div style={{ flex: 1 }}>
              <label htmlFor="quantityLevel" style={{ display: 'block', marginBottom: 4 }}>Nível de estoque *</label>
              <select
                id="quantityLevel"
                value={quantityLevel}
                onChange={(e) => setQuantityLevel(e.target.value as QuantityLevel)}
                style={{ width: '100%' }}
              >
                {QUANTITY_LEVELS.map((l) => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ marginBottom: 12 }}>
            <label htmlFor="expiryDate" style={{ display: 'block', marginBottom: 4 }}>Validade</label>
            <input
              id="expiryDate"
              type="date"
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
              style={{ width: '100%', boxSizing: 'border-box' }}
            />
          </div>

          <div style={{ marginBottom: 16 }}>
            <label htmlFor="notes" style={{ display: 'block', marginBottom: 4 }}>Notas</label>
            <textarea
              id="notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              maxLength={1000}
              rows={3}
              style={{ width: '100%', boxSizing: 'border-box' }}
            />
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" disabled={isPending}>
              {isPending ? 'Salvando...' : 'Salvar'}
            </button>
            <button type="button" onClick={onClose}>Cancelar</button>
          </div>
        </form>
      </div>
    </div>
  )
}
