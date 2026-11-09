import { getMessaging, getToken, isSupported, onMessage } from 'firebase/messaging'
import { app } from './firebase'
import { api } from './api'

let registration: ServiceWorkerRegistration | undefined
type Listener = (title: string, body: string, link?: string) => void
const listeners = new Set<Listener>()

/** Foreground pushes (app open) are shown as in-app toasts. */
export function onForegroundPush(fn: Listener) {
  listeners.add(fn)
  return () => { listeners.delete(fn) }
}

export async function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return
  const url = import.meta.env.DEV ? '/dev-sw.js?dev-sw' : '/sw.js'
  registration = await navigator.serviceWorker.register(url, { type: import.meta.env.DEV ? 'module' : 'classic' })
}

export type PushState = 'unsupported' | 'denied' | 'default' | 'granted'

export async function pushState(): Promise<PushState> {
  if (!app || !('Notification' in window) || !(await isSupported())) return 'unsupported'
  return Notification.permission as PushState
}

/** iPhone/iPad only allow web push after "Add to Home Screen". */
export const needsInstallForPush = () =>
  /iphone|ipad|ipod/i.test(navigator.userAgent) && !window.matchMedia('(display-mode: standalone)').matches

let enabled = false

/** Asks permission (if needed), gets the FCM token and registers it with the backend. */
export async function enablePush(askPermission = false): Promise<PushState> {
  const state = await pushState()
  if (state === 'unsupported' || state === 'denied') return state
  if (state === 'default' && !askPermission) return state
  if (state === 'default' && (await Notification.requestPermission()) !== 'granted') return Notification.permission as PushState
  if (enabled) return 'granted'
  if (!registration) await registerServiceWorker()
  const messaging = getMessaging(app!)
  const token = await getToken(messaging, {
    vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
    serviceWorkerRegistration: registration ?? (await navigator.serviceWorker.ready),
  })
  if (token) await api('/api/me/device-token', { method: 'PUT', body: { token } })
  onMessage(messaging, (p) => {
    const title = p.notification?.title ?? 'ServiceBay'
    const body = p.notification?.body ?? ''
    listeners.forEach((l) => l(title, body, p.data?.link))
  })
  enabled = true
  return 'granted'
}
