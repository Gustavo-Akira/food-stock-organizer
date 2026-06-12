# Workflow: New Frontend Feature

## Trigger
Use this workflow when adding a new user-facing feature to `apps/web` (React) or `apps/mobile` (Expo). This covers both new pages/screens and new sections within existing pages.

## Prerequisites
- The API endpoint this feature calls already exists and returns the expected shape
- `npm run build` passes from the repo root
- You know the target app: `web`, `mobile`, or both
- You know the API path and response shape

## Steps

### 1. Add Zod schema and TypeScript types to packages/shared

In `packages/shared/src/`, create or extend the relevant schema file:

```typescript
// packages/shared/src/<domain>.ts
import { z } from 'zod'

export const <Entity>Schema = z.object({
  id: z.string().uuid(),
  // add fields matching the API response shape exactly
})

export type <Entity> = z.infer<typeof <Entity>Schema>
```

Export from `packages/shared/src/index.ts`:

```typescript
export * from './<domain>'
```

### 2. Add API client helper to packages/shared

Create `packages/shared/src/api/<domain>.ts`:

```typescript
import axios from 'axios'
import { <Entity>Schema } from '../<domain>'
import type { <Entity> } from '../<domain>'

export async function get<Entity>(id: string): Promise<<Entity>> {
  const res = await axios.get(`/api/<context>/${id}`)
  return <Entity>Schema.parse(res.data)
}

export async function create<Entity>(data: Omit<<Entity>, 'id'>): Promise<<Entity>> {
  const res = await axios.post('/api/<context>', data)
  return <Entity>Schema.parse(res.data)
}
```

Export from `packages/shared/src/index.ts`:

```typescript
export * from './api/<domain>'
```

### 3. Create the feature folder

For web: `apps/web/src/features/<featureName>/`
For mobile: `apps/mobile/src/features/<featureName>/`

**Rule:** No imports from other feature folders. All files inside the feature folder are private to that feature.

### 4. Add a React Query hook

**For queries (reading data):**

Create `apps/web/src/features/<featureName>/use<Entity>.ts` (or `apps/mobile/src/features/<featureName>/use<Entity>.ts`):

```typescript
import { useQuery } from '@tanstack/react-query'
import { get<Entity> } from '@food-stock/shared'

export function use<Entity>(id: string) {
  return useQuery({
    queryKey: ['<entity>', id],
    queryFn: () => get<Entity>(id),
    enabled: !!id,
  })
}
```

**For mutations (creating/updating/deleting):**

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { create<Entity> } from '@food-stock/shared'
import type { <Entity> } from '@food-stock/shared'

export function useCreate<Entity>() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: Omit<<Entity>, 'id'>) => create<Entity>(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['<entity>'] })
    },
  })
}
```

### 5. Build the UI component

**Web** — Create `apps/web/src/features/<featureName>/<Component>.tsx`:

```typescript
import { use<Entity> } from './use<Entity>'

interface <Component>Props {
  id: string
}

export function <Component>({ id }: <Component>Props) {
  const { data, isLoading, error } = use<Entity>(id)

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading data</div>
  if (!data) return null

  return (
    <div>
      <p>{data.id}</p>
      {/* render other data fields */}
    </div>
  )
}
```

**Mobile** — Create `apps/mobile/src/features/<featureName>/<Component>.tsx`:

```typescript
import { View, Text, ActivityIndicator } from 'react-native'
import { use<Entity> } from './use<Entity>'

interface <Component>Props {
  id: string
}

export function <Component>({ id }: <Component>Props) {
  const { data, isLoading, error } = use<Entity>(id)

  if (isLoading) return <ActivityIndicator />
  if (error) return <Text>Error loading data</Text>
  if (!data) return null

  return (
    <View>
      <Text>{data.id}</Text>
      {/* render other data fields */}
    </View>
  )
}
```

### 6. Register the route

**Web** — Add to `apps/web/src/app/router.tsx` (or the main routing file):

```typescript
import { <Page> } from '../features/<featureName>/<Component>'

// inside the routes array:
{ path: '/<path>', element: <<Page> /> }
```

**Mobile** — Create `apps/mobile/src/app/<path>.tsx` (expo-router uses file-based routing):

```typescript
import { <Component> } from '../../features/<featureName>/<Component>'

export default function <Path>Screen() {
  const id = useLocalSearchParams<{ id: string }>().id
  return <<Component> id={id} />
}
```

Add `import { useLocalSearchParams } from 'expo-router'` at the top.

### 7. Verify build passes

Run from repo root: `npm run build`
Expected: all packages build successfully with no TypeScript errors.

If the web dev server is available: `npm --workspace @food-stock/web run dev`
If the mobile dev server is available: `npm --workspace @food-stock/mobile run start`

## Verification

- `npm run build` passes from repo root with no TypeScript errors
- Feature folder exists under `features/<featureName>/` with no imports from sibling feature folders
- Zod schema and TypeScript type are exported from `packages/shared/src/index.ts`
- React Query key is a string array starting with the entity name (e.g., `['<entity>', id]`)
- API client functions use `<Entity>Schema.parse(res.data)` for runtime validation
- Route is registered in the router (web) or as a file in `src/app/` (mobile)
