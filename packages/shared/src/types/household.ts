export type MemberRole = 'OWNER' | 'MEMBER'

export type MemberStatus = 'PENDING' | 'ACTIVE' | 'REJECTED'

export interface House {
  id: string
  name: string
  ownerId: string
  createdAt: string
  updatedAt: string
}

export interface HouseMember {
  id: string
  houseId: string
  userId: string
  role: MemberRole
  status: MemberStatus
  createdAt: string
}
