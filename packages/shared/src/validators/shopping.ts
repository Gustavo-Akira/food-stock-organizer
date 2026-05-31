import { z } from 'zod'

export const ShoppingListStatusSchema = z.enum(['OPEN', 'SHOPPING', 'COMPLETED'])

export const ShoppingListSchema = z.object({
  id: z.string().uuid(),
  houseId: z.string().uuid(),
  name: z.string().min(1).max(255),
  status: ShoppingListStatusSchema,
  createdBy: z.string().uuid(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export const ShoppingListItemSchema = z.object({
  id: z.string().uuid(),
  shoppingListId: z.string().uuid(),
  inventoryItemId: z.string().uuid().nullable().optional(),
  name: z.string().min(1),
  quantity: z.number().int().positive(),
  checked: z.boolean(),
  createdAt: z.string().datetime(),
})

export const GenerateShoppingListSchema = z.object({
  houseId: z.string().uuid(),
  listName: z.string().min(1).optional(),
})
