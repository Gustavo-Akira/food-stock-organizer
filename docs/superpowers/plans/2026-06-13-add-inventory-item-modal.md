# Add Inventory Item Modal — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a modal dialog to `InventoryPage` so users can add new inventory items via `POST /api/v1/inventory`.

**Architecture:** `InventoryPage` owns `isOpen` state and renders `<AddItemModal>`. The modal contains a controlled form validated client-side with Zod, calls a new `useAddItem` mutation hook, and closes on success while invalidating the inventory React Query cache. No new routes are added.

**Tech Stack:** React 18, TypeScript, TanStack Query v5, Zod, axios, Vitest, @testing-library/react

---

## File Map

| File | Action |
|---|---|
| `apps/web/vitest.config.ts` | Create — vitest config with jsdom + React plugin |
| `apps/web/src/test/setup.ts` | Create — jest-dom matchers setup |
| `apps/web/package.json` | Modify — add vitest + testing-library devDeps + test script |
| `apps/web/src/features/inventory/api/inventoryApi.ts` | Modify — add `addItem` function |
| `apps/web/src/features/inventory/api/inventoryApi.test.ts` | Create — unit test for `addItem` |
| `apps/web/src/features/inventory/hooks/useInventory.ts` | Modify — add `useAddItem` hook |
| `apps/web/src/features/inventory/hooks/useInventory.test.tsx` | Create — unit test for `useAddItem` |
| `apps/web/src/features/inventory/components/AddItemModal.tsx` | Create — modal component |
| `apps/web/src/features/inventory/components/AddItemModal.test.tsx` | Create — component tests |
| `apps/web/src/features/inventory/index.ts` | Modify — export `AddItemModal` |
| `apps/web/src/pages/InventoryPage.tsx` | Modify — add button + `isOpen` state + render modal |

---

## Task 1: Set Up Test Infrastructure

**Files:**
- Create: `apps/web/vitest.config.ts`
- Create: `apps/web/src/test/setup.ts`
- Modify: `apps/web/package.json`

- [ ] **Step 1: Install test dependencies**

Run from repo root:
```bash
npm install --save-dev --workspace @food-stock/web vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom
```

Expected: packages installed, no errors.

- [ ] **Step 2: Create vitest config**

Create `apps/web/vitest.config.ts`:
```ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
```

- [ ] **Step 3: Create test setup file**

Create `apps/web/src/test/setup.ts`:
```ts
import '@testing-library/jest-dom'
```

- [ ] **Step 4: Add test script to package.json**

In `apps/web/package.json`, add to `"scripts"`:
```json
"test": "vitest run",
"test:watch": "vitest"
```

Result:
```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "lint": "eslint . --ext ts,tsx",
    "preview": "vite preview",
    "test": "vitest run",
    "test:watch": "vitest"
  }
}
```

- [ ] **Step 4b: Add vitest global types to tsconfig**

Open `apps/web/tsconfig.json`. Add `"types"` to `"compilerOptions"`:
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    },
    "types": ["vitest/globals", "@testing-library/jest-dom"]
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

This lets TypeScript recognize `describe`, `it`, `expect`, `vi` as globals in all files under `src/`, and extends `expect` with the jest-dom matchers.

- [ ] **Step 5: Verify setup with a smoke test**

Create `apps/web/src/test/smoke.test.ts`:
```ts
import { describe, it, expect } from 'vitest'

describe('test setup', () => {
  it('works', () => {
    expect(1 + 1).toBe(2)
  })
})
```

Run:
```bash
npm --workspace @food-stock/web run test
```

Expected output includes: `✓ src/test/smoke.test.ts > test setup > works`

- [ ] **Step 6: Delete smoke test file**

Delete `apps/web/src/test/smoke.test.ts`.

- [ ] **Step 7: Commit**

```bash
git add apps/web/vitest.config.ts apps/web/src/test/setup.ts apps/web/package.json apps/web/tsconfig.json package-lock.json
git commit -m "chore(web): set up vitest with jsdom and testing-library"
```

---

## Task 2: Add `addItem` to `inventoryApi`

**Files:**
- Modify: `apps/web/src/features/inventory/api/inventoryApi.ts`
- Create: `apps/web/src/features/inventory/api/inventoryApi.test.ts`

- [ ] **Step 1: Write the failing test**

Create `apps/web/src/features/inventory/api/inventoryApi.test.ts`:
```ts
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
```

- [ ] **Step 2: Run test — verify it fails**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: FAIL — `inventoryApi.addItem is not a function` (or similar).

- [ ] **Step 3: Implement `addItem`**

Open `apps/web/src/features/inventory/api/inventoryApi.ts`. Current content:
```ts
import { apiClient } from '@/shared/lib/apiClient'
import type { InventoryItem, QuantityLevel } from '../types'

export const inventoryApi = {
  getItems: (houseId: string): Promise<InventoryItem[]> =>
    apiClient.get(`/api/v1/inventory?houseId=${houseId}`).then((r) => r.data),

  updateQuantity: (itemId: string, quantityLevel: QuantityLevel): Promise<InventoryItem> =>
    apiClient
      .patch(`/api/v1/inventory/${itemId}/quantity`, { quantityLevel })
      .then((r) => r.data),
}
```

Add the import and the new method:
```ts
import { apiClient } from '@/shared/lib/apiClient'
import type { InventoryItem, InventoryItemInput, QuantityLevel } from '../types'

export const inventoryApi = {
  getItems: (houseId: string): Promise<InventoryItem[]> =>
    apiClient.get(`/api/v1/inventory?houseId=${houseId}`).then((r) => r.data),

  updateQuantity: (itemId: string, quantityLevel: QuantityLevel): Promise<InventoryItem> =>
    apiClient
      .patch(`/api/v1/inventory/${itemId}/quantity`, { quantityLevel })
      .then((r) => r.data),

  addItem: (houseId: string, data: InventoryItemInput): Promise<InventoryItem> =>
    apiClient
      .post('/api/v1/inventory', data, { headers: { 'X-House-Id': houseId } })
      .then((r) => r.data),
}
```

- [ ] **Step 4: Re-export `InventoryItemInput` from types**

Open `apps/web/src/features/inventory/types.ts`. Current content:
```ts
export type { InventoryItem, QuantityLevel, Category } from '@food-stock/shared'
```

Add `InventoryItemInput`:
```ts
export type { InventoryItem, InventoryItemInput, QuantityLevel, Category } from '@food-stock/shared'
```

- [ ] **Step 5: Run test — verify it passes**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: `✓ src/features/inventory/api/inventoryApi.test.ts > inventoryApi.addItem > POSTs to /api/v1/inventory...`

- [ ] **Step 6: Commit**

```bash
git add apps/web/src/features/inventory/api/inventoryApi.ts apps/web/src/features/inventory/api/inventoryApi.test.ts apps/web/src/features/inventory/types.ts
git commit -m "feat(web): add inventoryApi.addItem with X-House-Id header"
```

---

## Task 3: Add `useAddItem` Hook

**Files:**
- Modify: `apps/web/src/features/inventory/hooks/useInventory.ts`
- Create: `apps/web/src/features/inventory/hooks/useInventory.test.tsx`

- [ ] **Step 1: Write the failing tests**

Create `apps/web/src/features/inventory/hooks/useInventory.test.tsx`:
```tsx
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
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: FAIL — `useAddItem is not a function` (or export not found).

- [ ] **Step 3: Implement `useAddItem`**

Open `apps/web/src/features/inventory/hooks/useInventory.ts`. Current content:
```ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { inventoryApi } from '../api/inventoryApi'
import type { QuantityLevel } from '../types'

export function useInventory(houseId: string) {
  return useQuery({
    queryKey: ['inventory', houseId],
    queryFn: () => inventoryApi.getItems(houseId),
    enabled: !!houseId,
  })
}

export function useUpdateQuantity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ itemId, quantityLevel }: { itemId: string; quantityLevel: QuantityLevel }) =>
      inventoryApi.updateQuantity(itemId, quantityLevel),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}
```

Add the import and new hook:
```ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { inventoryApi } from '../api/inventoryApi'
import type { InventoryItemInput, QuantityLevel } from '../types'

export function useInventory(houseId: string) {
  return useQuery({
    queryKey: ['inventory', houseId],
    queryFn: () => inventoryApi.getItems(houseId),
    enabled: !!houseId,
  })
}

export function useUpdateQuantity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ itemId, quantityLevel }: { itemId: string; quantityLevel: QuantityLevel }) =>
      inventoryApi.updateQuantity(itemId, quantityLevel),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}

export function useAddItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: InventoryItemInput) =>
      inventoryApi.addItem(data.houseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: all tests in `useInventory.test.tsx` pass.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/features/inventory/hooks/useInventory.ts apps/web/src/features/inventory/hooks/useInventory.test.tsx
git commit -m "feat(web): add useAddItem mutation hook"
```

---

## Task 4: Build `AddItemModal` Component

**Files:**
- Create: `apps/web/src/features/inventory/components/AddItemModal.tsx`
- Create: `apps/web/src/features/inventory/components/AddItemModal.test.tsx`

- [ ] **Step 1: Write the failing tests**

Create `apps/web/src/features/inventory/components/AddItemModal.test.tsx`:
```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
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
    await userEvent.click(screen.getByTestId('modal-backdrop'))
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
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: FAIL — `Cannot find module './AddItemModal'`.

- [ ] **Step 3: Implement `AddItemModal`**

Create `apps/web/src/features/inventory/components/AddItemModal.tsx`:
```tsx
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
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: all 8 tests in `AddItemModal.test.tsx` pass.

If `calls onClose when backdrop is clicked` fails (because clicking the child div also triggers the backdrop), check that `onClick={onClose}` is on the outer `data-testid="modal-backdrop"` div and `onClick={(e) => e.stopPropagation()}` is on the inner dialog div.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/features/inventory/components/AddItemModal.tsx apps/web/src/features/inventory/components/AddItemModal.test.tsx
git commit -m "feat(web): add AddItemModal component with form validation"
```

---

## Task 5: Wire Up `InventoryPage`

**Files:**
- Modify: `apps/web/src/features/inventory/index.ts`
- Modify: `apps/web/src/pages/InventoryPage.tsx`

- [ ] **Step 1: Export `AddItemModal` from the feature barrel**

Open `apps/web/src/features/inventory/index.ts`. Current content:
```ts
export { InventoryList } from './components/InventoryList'
export { useInventory, useUpdateQuantity } from './hooks/useInventory'
export { inventoryApi } from './api/inventoryApi'
export type { InventoryItem, QuantityLevel, Category } from './types'
```

Add the `AddItemModal` export:
```ts
export { InventoryList } from './components/InventoryList'
export { AddItemModal } from './components/AddItemModal'
export { useInventory, useUpdateQuantity, useAddItem } from './hooks/useInventory'
export { inventoryApi } from './api/inventoryApi'
export type { InventoryItem, InventoryItemInput, QuantityLevel, Category } from './types'
```

- [ ] **Step 2: Update `InventoryPage`**

Open `apps/web/src/pages/InventoryPage.tsx`. Current content:
```tsx
import { InventoryList } from '@/features/inventory'

export function InventoryPage() {
  return (
    <main>
      <h1>Estoque</h1>
      <InventoryList />
    </main>
  )
}
```

Replace with:
```tsx
import { useState } from 'react'
import { InventoryList, AddItemModal } from '@/features/inventory'

export function InventoryPage() {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <main>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Estoque</h1>
        <button onClick={() => setIsOpen(true)}>+ Adicionar Item</button>
      </div>
      <InventoryList />
      <AddItemModal isOpen={isOpen} onClose={() => setIsOpen(false)} houseId="demo" />
    </main>
  )
}
```

- [ ] **Step 3: Verify TypeScript build passes**

```bash
npm --workspace @food-stock/web run build
```

Expected: exits with code 0, no TypeScript errors.

- [ ] **Step 4: Run all tests**

```bash
npm --workspace @food-stock/web run test -- --reporter=verbose
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/features/inventory/index.ts apps/web/src/pages/InventoryPage.tsx
git commit -m "feat(web): wire AddItemModal into InventoryPage"
```

---

## Done

After all 5 tasks, verify the full build one more time:

```bash
npm run build
```

Expected: all packages build successfully.
