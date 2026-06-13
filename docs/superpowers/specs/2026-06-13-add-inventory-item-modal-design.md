# Add Inventory Item — Modal Feature

**Date:** 2026-06-13  
**Scope:** `apps/web` only  
**API endpoint:** `POST /api/v1/inventory` (already implemented)

## Summary

Add a modal dialog to `InventoryPage` that allows the user to add a new item to the inventory. The modal opens via a "+ Adicionar Item" button in the page header, submits to the existing API endpoint, and closes automatically on success while refreshing the list.

## Architecture

### Navigation pattern

Modal overlay on `/inventory` — no new route. `InventoryPage` owns `isOpen` state and renders `<AddItemModal>` conditionally. The router (`apps/web/src/app/router.tsx`) is not modified.

### Data flow

```
InventoryPage
  └─ isOpen (useState<boolean>)
  └─ <AddItemModal isOpen onClose houseId="demo">
       └─ useAddItem()
            └─ inventoryApi.addItem(houseId, data)
                 └─ POST /api/v1/inventory
                      Header: X-House-Id: {houseId}
                      Body: { name, category, quantityLevel, expiryDate?, notes? }
       └─ onSuccess:
            queryClient.invalidateQueries({ queryKey: ['inventory'] })
            onClose()
```

### Files changed

| File | Change |
|---|---|
| `packages/shared/src/validators/inventory.ts` | No change — `CreateInventoryItemSchema` already exists |
| `features/inventory/api/inventoryApi.ts` | Add `addItem(houseId, data)` function |
| `features/inventory/hooks/useInventory.ts` | Add `useAddItem()` mutation hook |
| `features/inventory/components/AddItemModal.tsx` | New component |
| `features/inventory/index.ts` | Export `AddItemModal` |
| `pages/InventoryPage.tsx` | Add `isOpen` state, "+ Adicionar Item" button, render `<AddItemModal>` |

## Components

### `AddItemModal`

Props:
```ts
interface AddItemModalProps {
  isOpen: boolean
  onClose: () => void
  houseId: string
}
```

Renders a full-screen backdrop + centered dialog. Calls `useAddItem()`. On submit: validates with `CreateInventoryItemSchema` client-side before sending. On `isPending`: Salvar button is disabled with text "Salvando...". On `isError`: shows a generic error banner at the top of the modal. On `isSuccess`: `onClose()` is called via the `onSuccess` option passed at the `useMutation` call site inside the component — the hook itself only handles cache invalidation.

Closing: clicking the ✕ button, "Cancelar", or the backdrop calls `onClose()`.

Form reset: the form state resets to empty values whenever `isOpen` transitions to `false`. Implemented via a `key={isOpen}` prop on the form element so React remounts it on each open.

### `useAddItem` hook

```ts
export function useAddItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ houseId, data }: { houseId: string; data: InventoryItemInput }) =>
      inventoryApi.addItem(houseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}
```

The `onClose` call happens in the component's `onSuccess` option (passed via `useMutation` options at the call site), keeping the hook decoupled from navigation concerns.

### `inventoryApi.addItem`

```ts
addItem: (houseId: string, data: InventoryItemInput): Promise<InventoryItem> =>
  apiClient
    .post('/api/v1/inventory', data, { headers: { 'X-House-Id': houseId } })
    .then((r) => r.data)
```

## Form Fields

| Field | Input type | Required | Validation |
|---|---|---|---|
| `name` | `<input type="text">` | yes | `minLength=1`, `maxLength=255` |
| `category` | `<select>` | yes | enum: FOOD / CLEANING / HYGIENE / OTHER |
| `quantityLevel` | `<select>` | yes | enum: RUNNING_OUT / ENOUGH / PLENTY |
| `expiryDate` | `<input type="date">` | no | valid date string |
| `notes` | `<textarea>` | no | `maxLength=1000` |

Client-side validation uses `CreateInventoryItemSchema.safeParse()` before the mutation fires. Field-level errors are shown inline below each input. The schema is imported from `@food-stock/shared`.

## Error Handling

- **Validation errors (client-side):** Inline message below the relevant field. Submit button remains disabled until the form is valid.
- **API error (4xx / 5xx):** Generic error banner at the top of the modal: "Não foi possível salvar o item. Tente novamente."
- **Loading state:** Salvar button text changes to "Salvando..." and is `disabled`.

## houseId

Follows the existing hardcoded `'demo'` convention used in `InventoryList`. The JWT-based `houseId` extraction is a known TODO in `InventoryController` and is out of scope for this feature.

## Testing

CI enforces ≥ 80% diff coverage on modified lines. New tests required:

### `useAddItem` (unit)
- Calls `inventoryApi.addItem` with correct args
- On success: invalidates `['inventory']` query
- On error: exposes error state

### `AddItemModal` (component)
- Renders all 5 fields
- Submit with valid data calls `useAddItem` mutation
- Submit with missing required field shows validation error, does not call mutation
- "Cancelar" button calls `onClose`
- During `isPending`: Salvar button is disabled
- On API error: error banner is visible
