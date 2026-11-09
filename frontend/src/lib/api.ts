import { auth } from './firebase'

const BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? 'http://localhost:8080'
const DEV_TOKEN_KEY = 'sb.devToken'

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message) }
}

// ---- "server is waking up" state (free hosting sleeps when idle) ----
type WakeListener = (waking: boolean) => void
const wakeListeners = new Set<WakeListener>()
let waking = false
function setWaking(v: boolean) {
  if (waking === v) return
  waking = v
  wakeListeners.forEach((l) => l(v))
}
export function onServerWaking(fn: WakeListener) {
  wakeListeners.add(fn)
  fn(waking) // report the current state right away
  return () => { wakeListeners.delete(fn) }
}

const RETRY_STATUSES = [502, 503, 504]
const WAKE_TIMEOUT_MS = 90_000
const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

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
  const method = opts.method ?? (body ? 'POST' : 'GET')
  // GETs are retried while the server wakes up; other requests only when the
  // gateway answered 502/503/504 (the request never reached the app).
  const started = Date.now()
  let res: Response
  for (let attempt = 0; ; attempt++) {
    try {
      res = await fetch(BASE + path, { method, headers, body })
      if (!RETRY_STATUSES.includes(res.status)) break
    } catch {
      if (method !== 'GET' || Date.now() - started > WAKE_TIMEOUT_MS) {
        setWaking(false)
        throw new ApiError(0, 'NETWORK', 'Could not reach the server. Check your connection and try again.')
      }
    }
    if (Date.now() - started > WAKE_TIMEOUT_MS) {
      setWaking(false)
      throw new ApiError(503, 'SERVER_ASLEEP', 'The server is taking too long to start. Please try again in a minute.')
    }
    setWaking(true)
    await sleep(Math.min(2000 + attempt * 1000, 5000))
  }
  setWaking(false)
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) throw new ApiError(res.status, data?.code ?? 'ERROR', data?.detail ?? 'Something went wrong')
  return data as T
}

/** Wakes a sleeping server early (called on app start). */
export async function pingServer() {
  try { await api('/actuator/health') } catch { /* shown via waking banner / later calls */ }
}

export const errMsg = (e: unknown) => (e instanceof Error ? e.message : 'Something went wrong')
