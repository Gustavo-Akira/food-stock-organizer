# Web Login Flow - Design Spec

**Date:** 2026-08-29
**Scope:** `apps/web` and `packages/shared`
**API endpoint:** `POST /api/auth/login` (already implemented)

## Summary

Add the first usable web authentication flow using the backend contract that exists today. The backend login endpoint returns a JWT in the response body, and the web client will store that token in `localStorage.auth_token`. The existing axios request interceptor in `apps/web/src/shared/lib/apiClient.ts` will continue to send the token as `Authorization: Bearer <token>`.

This spec intentionally does not implement HttpOnly cookies. Cookie-based browser sessions remain a future migration once the app has real usage and the current Bearer flow has proven the product path.

## Current Contract

Backend:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}
```

Success response:

```json
{
  "token": "jwt-token",
  "user": {
    "id": "user-id",
    "name": "User Name",
    "email": "user@example.com"
  }
}
```

Authenticated API calls:

```http
Authorization: Bearer jwt-token
```

## Architecture

The feature is isolated under `apps/web/src/features/auth`. The feature owns login API calls, session storage helpers, auth state, route guards, and the login page. Shared API types are added to `packages/shared/src/types/auth.ts` so frontend code does not duplicate request/response shapes.

The app router gains `/login` plus protected routes for `/inventory` and `/shopping`. Protected pages are rendered only when a token exists in `localStorage.auth_token`; otherwise the user is redirected to `/login`. The login page redirects authenticated users back to `/inventory`.

## Data Flow

```text
LoginPage
  -> useLogin()
     -> authApi.login({ email, password })
        -> POST /api/auth/login
        -> LoginResponse { token, user }
     -> authSession.saveSession(response)
        -> localStorage.auth_token = token
        -> localStorage.auth_user = JSON.stringify(user)
     -> navigate('/inventory')

apiClient request interceptor
  -> reads localStorage.auth_token
  -> sets Authorization: Bearer <token>

apiClient response interceptor
  -> on 401:
     -> removes auth_token
     -> removes auth_user
     -> redirects to /login
```

## Files

| File | Change |
|---|---|
| `packages/shared/src/types/auth.ts` | Add `LoginRequest`, `LoginUser`, and `LoginResponse` types matching the backend response |
| `packages/shared/src/types/index.ts` | Export auth types if not already exported |
| `packages/shared/src/index.ts` | Ensure shared auth types remain exported |
| `apps/web/src/features/auth/api/authApi.ts` | New feature API helper for `POST /api/auth/login` |
| `apps/web/src/features/auth/authSession.ts` | New localStorage helpers for token/user session |
| `apps/web/src/features/auth/hooks/useAuth.ts` | New hook exposing auth state, login, and logout |
| `apps/web/src/features/auth/components/RequireAuth.tsx` | New route guard for protected routes |
| `apps/web/src/features/auth/index.ts` | New feature barrel |
| `apps/web/src/pages/LoginPage.tsx` | New login page |
| `apps/web/src/app/router.tsx` | Add `/login` route and protect `/inventory` and `/shopping` |
| `apps/web/src/shared/lib/apiClient.ts` | Also remove `auth_user` on 401 |

## Session Storage

Use two localStorage keys:

```ts
const AUTH_TOKEN_KEY = 'auth_token'
const AUTH_USER_KEY = 'auth_user'
```

Stored values:

```ts
localStorage.setItem('auth_token', token)
localStorage.setItem('auth_user', JSON.stringify(user))
```

The user object is convenience UI state only. Authorization depends exclusively on the JWT token.

## Route Behavior

| Path | Token exists | Token missing |
|---|---|---|
| `/login` | Redirect to `/inventory` | Show login form |
| `/inventory` | Show inventory page | Redirect to `/login` |
| `/shopping` | Show shopping page | Redirect to `/login` |
| `/` | Redirect to `/inventory` | Redirect chain reaches `/login` |

## Login Page Behavior

Fields:

| Field | Type | Required |
|---|---|---|
| `email` | `email` | yes |
| `password` | `password` | yes |

Submit behavior:

1. Disable submit button while login is pending.
2. Call `authApi.login({ email, password })`.
3. On success, save token/user and navigate to `/inventory`.
4. On failure, show `Email ou senha invalidos.`

## Error Handling

| Situation | UI behavior |
|---|---|
| Empty email or password | Disable submit until both fields contain text |
| Login returns 401/403 | Show invalid credentials message |
| Login returns 5xx/network error | Show `Nao foi possivel entrar. Tente novamente.` |
| Authenticated request returns 401 | Clear local session and redirect to `/login` |
| Authenticated request returns 403 | Leave session intact; feature-level UI can show access error |

## Out Of Scope

- No HttpOnly cookie support in this PR.
- No refresh token.
- No register page.
- No password reset.
- No mobile auth changes.
- No backend changes unless the frontend discovers a contract mismatch.

## Future Migration Note

When the app is ready for hardened browser sessions, add a separate backend plan for web HttpOnly cookies while keeping mobile Bearer token auth. The likely end state is transport-specific auth:

- Web: HttpOnly cookie set by the backend.
- Mobile: Bearer token stored with a secure native storage mechanism.

That migration should not block this first web login flow.

## Testing

Required web tests:

- `authSession` saves, reads, and clears token/user.
- `authApi.login` posts to `/api/auth/login` with the request payload.
- `useAuth` exposes authenticated state from localStorage and saves session on login.
- `RequireAuth` redirects to `/login` when no token exists.
- `RequireAuth` renders protected children when a token exists.
- `LoginPage` submits credentials and redirects on success.
- `LoginPage` shows a user-facing error on failed login.
- `apiClient` clears both `auth_token` and `auth_user` on `401`.

Verification commands:

```bash
npm.cmd run test
npm.cmd run build
```
