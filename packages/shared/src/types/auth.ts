export interface User {
  id: string
  email: string
  name: string
  createdAt: string
  updatedAt: string
}

export interface AuthToken {
  token: string
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginUser {
  id: string
  email: string
  name: string
}

export interface LoginResponse {
  token: string
  user: LoginUser
}
