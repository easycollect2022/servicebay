import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Icon } from './Icon'
import { STATUS } from '../lib/format'
import type { BookingStatus } from '../lib/types'
import { onForegroundPush } from '../lib/push'

export function Page({ children, bottomNav, className = '' }: { children: ReactNode; bottomNav?: ReactNode; className?: string }) {
  return (
    <div className="mx-auto min-h-screen max-w-md bg-bg">
      <main className={`px-5 pt-5 ${bottomNav ? 'pb-28' : 'pb-10'} ${className}`}>{children}</main>
      {bottomNav}
    </div>
  )
}

export function Header({ title, back, right }: { title: string; back?: string | true; right?: ReactNode }) {
  const nav = useNavigate()
  return (
    <div className="mb-4 flex items-center gap-3">
      {back && (
        <button aria-label="Back" className="flex h-11 w-11 items-center justify-center rounded-xl border border-line bg-surface2"
          onClick={() => (back === true ? nav(-1) : nav(back))}>
          <Icon name="back" />
        </button>
      )}
      <h1 className="flex-1 font-display text-[22px] font-bold">{title}</h1>
      {right}
    </div>
  )
}

interface Tab { to: string; label: string; icon: string; end?: boolean }
export function BottomNav({ tabs }: { tabs: Tab[] }) {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-[#11151C] pb-[env(safe-area-inset-bottom)]">
      <div className="mx-auto grid h-[68px] max-w-md" style={{ gridTemplateColumns: `repeat(${tabs.length}, minmax(0, 1fr))` }}>
        {tabs.map((t) => (
          <NavLink key={t.to} to={t.to} end={t.end}
            className={({ isActive }) => `flex flex-col items-center justify-center gap-1 text-xs font-medium ${isActive ? 'text-mint' : 'text-muted'}`}>
            <Icon name={t.icon} />{t.label}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}

export const CustomerNav = () => (
  <BottomNav tabs={[{ to: '/', label: 'Home', icon: 'home', end: true }, { to: '/bookings', label: 'Bookings', icon: 'calendar' }, { to: '/profile', label: 'Profile', icon: 'user' }]} />
)
export const MerchantNav = () => (
  <BottomNav tabs={[{ to: '/merchant', label: 'Orders', icon: 'calendar', end: true }, { to: '/merchant/services', label: 'Services', icon: 'wrench' },
    { to: '/merchant/reviews', label: 'Reviews', icon: 'star' }, { to: '/merchant/shop', label: 'Shop', icon: 'shop' }]} />
)

export function Stars({ value, size = 14 }: { value: number; size?: number }) {
  return (
    <span className="inline-flex" aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Icon key={n} name="star" size={size} filled className={n <= Math.round(value) ? 'text-star' : 'text-line'} />
      ))}
    </span>
  )
}

export function StatusPill({ status }: { status: BookingStatus }) {
  const s = STATUS[status]
  return <span className={`inline-flex h-[26px] items-center rounded-full px-2.5 text-xs font-semibold ${s.cls}`}>{s.label}</span>
}

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-muted" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-line border-t-mint" />{label}
    </div>
  )
}

export function ErrorBox({ msg }: { msg?: string | null }) {
  if (!msg) return null
  return <p role="alert" className="rounded-xl border border-danger-line bg-danger-soft px-4 py-3 text-sm text-danger">{msg}</p>
}

export function Empty({ icon, title, text, action }: { icon: string; title: string; text?: string; action?: ReactNode }) {
  return (
    <div className="card flex flex-col items-center gap-2 px-6 py-10 text-center">
      <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-surface2 text-mint"><Icon name={icon} /></span>
      <p className="font-semibold">{title}</p>
      {text && <p className="text-sm text-muted">{text}</p>}
      {action}
    </div>
  )
}

export function Thumb({ src, size = 84, icon = 'image' }: { src?: string; size?: number; icon?: string }) {
  return src
    ? <img src={src} alt="" className="shrink-0 rounded-xl object-cover" style={{ width: size, height: size }} />
    : <div className="flex shrink-0 items-center justify-center rounded-xl bg-surface2 text-faint" style={{ width: size, height: size }}><Icon name={icon} size={28} /></div>
}

export function Toggle({ on, onChange, label }: { on: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <button type="button" role="switch" aria-checked={on} aria-label={label} onClick={() => onChange(!on)}
      className={`relative h-[26px] w-11 shrink-0 rounded-full transition-colors ${on ? 'bg-mint' : 'bg-[#3A4458]'}`}>
      <span className={`absolute top-[3px] h-5 w-5 rounded-full bg-ink transition-all ${on ? 'left-[21px]' : 'left-[3px]'}`} />
    </button>
  )
}

// ---------- toasts (also used for foreground push notifications) ----------
interface ToastItem { id: number; title: string; body?: string; link?: string }
const ToastCtx = createContext<(title: string, body?: string, link?: string) => void>(() => undefined)
export const useToast = () => useContext(ToastCtx)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])
  const show = useCallback((title: string, body?: string, link?: string) => {
    const id = Date.now() + Math.random()
    setItems((x) => [...x, { id, title, body, link }])
    setTimeout(() => setItems((x) => x.filter((t) => t.id !== id)), 5000)
  }, [])
  useEffect(() => onForegroundPush(show), [show])
  return (
    <ToastCtx.Provider value={show}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 top-3 z-50 mx-auto flex max-w-md flex-col gap-2 px-3" aria-live="polite">
        {items.map((t) => {
          const inner = (
            <div className="pointer-events-auto rounded-2xl border border-[#3A4458] bg-[#232B38] p-3.5 shadow-xl">
              <p className="text-[15px] font-semibold">{t.title}</p>
              {t.body && <p className="mt-0.5 text-sm text-muted">{t.body}</p>}
            </div>
          )
          return t.link ? <Link key={t.id} to={t.link}>{inner}</Link> : <div key={t.id}>{inner}</div>
        })}
      </div>
    </ToastCtx.Provider>
  )
}
