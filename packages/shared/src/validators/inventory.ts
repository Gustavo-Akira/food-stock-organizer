import { z } from 'zod'

export const QuantityLevelSchema = z.enum(['RUNNING_OUT', 'ENOUGH', 'PLENTY'])

export const CategorySchema = z.enum(['FOOD', 'CLEANING', 'HYGIENE', 'OTHER'])

export const InventoryItemSchema = z.object({
  id: z.string().uuid(),
  houseId: z.string().uuid(),
  name: z.string().min(1).max(255),
  category: CategorySchema,
  quantityLevel: QuantityLevelSchema,
  expiryDate: z.string().date().nullable().optional(),
  notes: z.string().nullable().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export const CreateInventoryItemSchema = InventoryItemSchema.omit({
  id: true,
  createdAt: true,
  updatedAt: true,
})

export type InventoryItemInput = z.infer<typeof CreateInventoryItemSchema>
