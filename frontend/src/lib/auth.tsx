import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { onAuthStateChanged, signOut } from 'firebase/auth'
import { auth } from './firebase'
import { api, devToken } from './api'
import type { Me, Role } from './types'
import { enablePush } from './push'

interface AuthState {
  loading: boolean
  signedIn: boolean
  me: Me | null
  refresh: () => Promise<Me | null>
  chooseRole: (role: Role, name?: string) => Promise<Me>
  devLogin: (phone: string, name: string) => Promise<void>
  logout: () => Promise<void>
}

const Ctx = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true)
  const [signedIn, setSignedIn] = useState(false)
  const [me, setMe] = useState<Me | null>(null)

  const refresh = useCallback(async () => {
    try {
      const m = await api<Me>('/api/me')
      setMe(m)
      setSignedIn(true)
      if (m.role) enablePush().catch(() => undefined)
      return m
    } catch {
      setMe(null)
      setSignedIn(false)
      return null
    }
  }, [])

  useEffect(() => {
    if (auth) {
      return onAuthStateChanged(auth, async (u) => {
        if (u) await refresh()
        else if (devToken.get()) await refresh()
        else { setMe(null); setSignedIn(false) }
        setLoading(false)
      })
    }
    ;(devToken.get() ? refresh() : Promise.resolve(null)).finally(() => setLoading(false))
  }, [refresh])

  const chooseRole = useCallback(async (role: Role, name?: string) => {
    const m = await api<Me>('/api/me/role', { body: { role, name } })
    setMe(m)
    enablePush().catch(() => undefined)
    return m
  }, [])

  const devLogin = useCallback(async (phone: string, name: string) => {
    const digits = phone.replace(/\D/g, '')
    devToken.set(`dev:dev-${digits}:+91${digits}:${name}`)
    await refresh()
  }, [refresh])

  const logout = useCallback(async () => {
    devToken.set(null)
    if (auth) await signOut(auth)
    setMe(null)
    setSignedIn(false)
  }, [])

  return (
    <Ctx.Provider value={{ loading, signedIn, me, refresh, chooseRole, devLogin, logout }}>
      {children}
    </Ctx.Provider>
  )
}

export function useAuth() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useAuth outside AuthProvider')
  return v
}
