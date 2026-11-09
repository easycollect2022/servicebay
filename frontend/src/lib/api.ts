import { auth } from './firebase'

const BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? 'http://localhost:8080'
const DEV_TOKEN_KEY = 'sb.devToken'

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message) }
}

export const devToken = {
  get: () => { try { return localStorage.getItem(DEV_TOKEN_KEY) } catch { return null } },
  set: (t: string | null) => { try { t ? localStorage.setItem(DEV_TOKEN_KEY, t) : localStorage.removeItem(DEV_TOKEN_KEY) } catch { /* ignore */ } },
}

async function token(): Promise<string | null> {
  const user = auth?.currentUser
  if (user) return user.getIdToken()
  return devToken.get()
}

export async function api<T>(path: string, opts: { method?: string; body?: unknown; form?: FormData } = {}): Promise<T> {
  const headers: Record<string, string> = {}
  const t = await token()
  if (t) headers.Authorization = `Bearer ${t}`
  let body: BodyInit | undefined
  if (opts.form) body = opts.form
  else if (opts.body !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(opts.body) }
  let res: Response
  try {
    res = await fetch(BASE + path, { method: opts.method ?? (body ? 'POST' : 'GET'), headers, body })
  } catch {
    throw new ApiError(0, 'NETWORK', 'Network error. Check your connection and try again.')
  }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) throw new ApiError(res.status, data?.code ?? 'ERROR', data?.detail ?? 'Something went wrong')
  return data as T
}

export const errMsg = (e: unknown) => (e instanceof Error ? e.message : 'Something went wrong')
