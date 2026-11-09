import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errMsg } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { CATEGORIES, isValidPincode, rupees } from '../../lib/format'
import type { MerchantSummary, NotificationList, ServiceCategory, VehicleType } from '../../lib/types'
import { CustomerNav, Empty, ErrorBox, Page, Spinner, Thumb } from '../../components/ui'
import { CATEGORY_ICON, Icon } from '../../components/Icon'

const PIN_KEY = 'sb.pincode'

export default function Home() {
  const { me } = useAuth()
  const [pincode, setPincode] = useState(() => { try { return localStorage.getItem(PIN_KEY) || me?.pincode || '' } catch { return me?.pincode || '' } })
  const [editing, setEditing] = useState(!pincode)
  const [draft, setDraft] = useState(pincode)
  const [vehicle, setVehicle] = useState<VehicleType>('BIKE')
  const [category, setCategory] = useState<ServiceCategory | null>(null)
  const [query, setQuery] = useState('')
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null)
  const [shops, setShops] = useState<MerchantSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    navigator.geolocation?.getCurrentPosition((p) => setCoords({ lat: p.coords.latitude, lng: p.coords.longitude }), () => undefined, { timeout: 8000 })
    api<NotificationList>('/api/me/notifications').then((n) => setUnread(n.unread)).catch(() => undefined)
  }, [])

  useEffect(() => {
    if (!isValidPincode(pincode)) return
    setShops(null); setError(null)
    const q = new URLSearchParams({ pincode, vehicle })
    if (category) q.set('category', category)
    if (coords) { q.set('lat', String(coords.lat)); q.set('lng', String(coords.lng)) }
    api<MerchantSummary[]>('/api/merchants?' + q).then(setShops).catch((e) => setError(errMsg(e)))
  }, [pincode, vehicle, category, coords])

  function savePincode() {
    if (!isValidPincode(draft)) return setError('Enter a valid 6-digit pincode')
    setError(null); setPincode(draft); setEditing(false)
    try { localStorage.setItem(PIN_KEY, draft) } catch { /* ignore */ }
  }

  const visible = (shops ?? []).filter((s) => s.shopName.toLowerCase().includes(query.trim().toLowerCase()))

  return (
    <Page bottomNav={<CustomerNav />}>
      <div className="mb-4 flex items-center justify-between">
        <button className="text-left" onClick={() => { setDraft(pincode); setEditing(true) }}>
          <span className="block text-xs text-muted">Showing shops in</span>
          <span className="flex items-center gap-1.5 font-semibold">
            <Icon name="pin" size={18} className="text-mint" />{pincode || 'Set pincode'}<Icon name="down" size={16} />
          </span>
        </button>
        <Link to="/notifications" aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}
          className="relative flex h-11 w-11 items-center justify-center rounded-xl border border-line bg-surface2">
          <Icon name="bell" />
          {unread > 0 && <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-mint px-1 text-[11px] font-bold text-mint-ink">{unread}</span>}
        </Link>
      </div>

      {editing && (
        <div className="card mb-4 flex flex-col gap-3 p-4">
          <label className="label !mb-0" htmlFor="pin">Your pincode</label>
          <div className="flex gap-2">
            <input id="pin" className="input" inputMode="numeric" maxLength={6} value={draft} onChange={(e) => setDraft(e.target.value.replace(/\D/g, ''))} placeholder="560034" />
            <button className="btn-primary" onClick={savePincode}>Show</button>
          </div>
        </div>
      )}

      <label className="mb-4 flex h-12 items-center gap-2.5 rounded-[14px] border border-line bg-surface px-3.5 text-muted">
        <Icon name="search" />
        <input aria-label="Search shops" placeholder="Search shop name" value={query} onChange={(e) => setQuery(e.target.value)}
          className="flex-1 bg-transparent text-[15px] text-ink outline-none placeholder:text-faint" />
      </label>

      <div className="mb-5 flex gap-2">
        {(['BIKE', 'CAR'] as VehicleType[]).map((v) => (
          <button key={v} className={`chip ${vehicle === v ? 'chip-on' : ''}`} aria-pressed={vehicle === v} onClick={() => setVehicle(v)}>
            {v === 'BIKE' ? 'Bike' : 'Car'}
          </button>
        ))}
      </div>

      <div className="mb-6 grid grid-cols-4 gap-y-3.5">
        {[...CATEGORIES, { key: null, label: 'All' } as const].map((c) => {
          const on = category === c.key
          return (
            <button key={c.label} onClick={() => setCategory(c.key)} aria-pressed={on} className="flex flex-col items-center gap-1.5 text-xs font-medium">
              <span className={`flex h-14 w-14 items-center justify-center rounded-2xl border ${on ? 'border-mint bg-mint-soft' : 'border-line bg-surface'} text-mint`}>
                <Icon name={c.key ? CATEGORY_ICON[c.key] : 'grid'} />
              </span>{c.label}
            </button>
          )
        })}
      </div>

      <ErrorBox msg={error} />
      {isValidPincode(pincode) && !shops && !error && <Spinner />}
      {shops && (
        <>
          <h2 className="mb-3 font-display text-lg font-semibold">{visible.length} shop{visible.length === 1 ? '' : 's'} near {pincode}</h2>
          {visible.length === 0
            ? <Empty icon="shop" title="No shops here yet" text="Try another pincode, vehicle type or service." />
            : (
              <div className="flex flex-col gap-3">
                {visible.map((s) => (
                  <Link key={s.id} to={`/shop/${s.id}?vehicle=${vehicle}`} className="card flex gap-3 p-3">
                    <Thumb src={s.coverPhoto} />
                    <div className="flex min-w-0 flex-1 flex-col gap-1">
                      <div className="flex justify-between gap-2">
                        <strong className="truncate">{s.shopName}</strong>
                        <span className={`text-xs font-semibold ${s.openNow ? 'text-mint' : 'text-muted'}`}>{s.openNow ? 'Open' : 'Closed now'}</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-[13px]">
                        <Icon name="star" size={14} filled className="text-star" />
                        <strong>{s.ratingCount ? Number(s.ratingAvg).toFixed(1) : 'New'}</strong>
                        <span className="text-muted">{s.ratingCount ? `(${s.ratingCount})` : ''}{s.distanceKm != null ? ` · ${s.distanceKm} km` : ''}</span>
                      </div>
                      <span className="truncate text-[13px] text-muted">{s.categories.map((c) => CATEGORIES.find((x) => x.key === c)?.label).join(' · ')}</span>
                      {s.fromPrice != null && <span className="text-[13px]">From <strong>{rupees(s.fromPrice)}</strong></span>}
                    </div>
                  </Link>
                ))}
              </div>
            )}
        </>
      )}
    </Page>
  )
}
