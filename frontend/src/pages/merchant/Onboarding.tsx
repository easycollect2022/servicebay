import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, errMsg } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { DAYS, isValidMobile, isValidPincode } from '../../lib/format'
import type { Day, MerchantProfile } from '../../lib/types'
import { ErrorBox, MerchantNav, Page, Spinner, useToast } from '../../components/ui'
import { Icon } from '../../components/Icon'
import { PushCard } from '../customer/Profile'

const hhmm = (t?: string) => (t ?? '').slice(0, 5)

export default function Onboarding({ first = false }: { first?: boolean }) {
  const { refresh, logout } = useAuth()
  const nav = useNavigate()
  const toast = useToast()
  const fileRef = useRef<HTMLInputElement>(null)
  const [p, setP] = useState<MerchantProfile | null>(null)
  const [f, setF] = useState({ shopName: '', phone: '', address: '', pincode: '', city: '', latitude: '' as number | '', longitude: '' as number | '',
    servesBike: true, servesCar: false, openDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] as Day[], openTime: '09:00', closeTime: '20:00' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [locating, setLocating] = useState(false)

  useEffect(() => {
    api<MerchantProfile>('/api/merchant/profile').then((m) => {
      setP(m)
      setF((x) => ({ ...x, shopName: m.shopName ?? '', phone: (m.phone ?? '').replace(/^\+91\s?/, '').replace(/\s/g, ''), address: m.address ?? '', pincode: m.pincode ?? '',
        city: m.city ?? '', latitude: m.latitude ?? '', longitude: m.longitude ?? '', servesBike: m.servesBike, servesCar: m.servesCar,
        openDays: m.openDays.length ? m.openDays : x.openDays, openTime: hhmm(m.openTime) || x.openTime, closeTime: hhmm(m.closeTime) || x.closeTime }))
    }).catch((e) => setError(errMsg(e)))
  }, [])

  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((x) => ({ ...x, [k]: v }))
  const toggleDay = (d: Day) => set('openDays', f.openDays.includes(d) ? f.openDays.filter((x) => x !== d) : [...f.openDays, d])

  function locate() {
    if (!navigator.geolocation) return setError('Location is not available on this device')
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => { set('latitude', +pos.coords.latitude.toFixed(6)); set('longitude', +pos.coords.longitude.toFixed(6)); setLocating(false) },
      () => { setError('Could not get your location. Allow location access and try again.'); setLocating(false) },
      { enableHighAccuracy: true, timeout: 10000 })
  }

  async function upload(file: File) {
    setBusy(true); setError(null)
    const form = new FormData(); form.append('file', file)
    try { setP(await api<MerchantProfile>('/api/merchant/photos', { form })) } catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }
  async function removePhoto(id: number) {
    try { setP(await api<MerchantProfile>(`/api/merchant/photos/${id}`, { method: 'DELETE' })) } catch (e) { setError(errMsg(e)) }
  }

  async function save() {
    setError(null)
    if (!f.shopName.trim()) return setError('Enter your shop name')
    if (!isValidMobile(f.phone)) return setError('Enter a valid 10-digit mobile number')
    if (!f.address.trim()) return setError('Enter the shop address')
    if (!isValidPincode(f.pincode)) return setError('Enter a valid 6-digit pincode')
    if (!f.servesBike && !f.servesCar) return setError('Select at least one vehicle type')
    if (!f.openDays.length) return setError('Select the days your shop is open')
    if (f.openTime >= f.closeTime) return setError('Closing time must be after opening time')
    setBusy(true)
    try {
      await api('/api/merchant/profile', { method: 'PUT', body: { ...f, phone: '+91' + f.phone, latitude: f.latitude === '' ? null : f.latitude, longitude: f.longitude === '' ? null : f.longitude } })
      if (first) { await refresh(); nav('/merchant/services', { replace: true }); toast('Shop saved', 'Now add your services and prices.') }
      else toast('Shop details saved')
    } catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }

  if (!p) return <Page>{error ? <ErrorBox msg={error} /> : <Spinner />}</Page>

  return (
    <Page bottomNav={first ? undefined : <MerchantNav />} className="flex flex-col gap-5">
      <div className="flex flex-col gap-2.5">
        {first && <>
          <span className="text-[13px] font-semibold text-muted">Step 1 of 2 · Shop details</span>
          <div className="grid grid-cols-2 gap-1.5"><span className="h-1 rounded bg-mint" /><span className="h-1 rounded bg-line" /></div>
        </>}
        <h1 className="mt-1 font-display text-2xl font-bold">{first ? 'Set up your shop' : 'Shop details'}</h1>
        <p className="text-sm text-muted">Customers see this on your shop page.</p>
      </div>

      <div><label className="label" htmlFor="sn">Shop name</label><input id="sn" className="input" value={f.shopName} onChange={(e) => set('shopName', e.target.value)} /></div>
      <div><label className="label" htmlFor="mn">Shop mobile number</label>
        <div className="flex gap-2"><span className="flex h-[52px] items-center rounded-[14px] border border-line bg-surface2 px-3.5 font-semibold">+91</span>
          <input id="mn" className="input" inputMode="numeric" maxLength={10} value={f.phone} onChange={(e) => set('phone', e.target.value.replace(/\D/g, ''))} /></div></div>
      <div><label className="label" htmlFor="ad">Shop address</label><textarea id="ad" rows={2} className="input h-auto resize-none py-3.5 leading-snug" value={f.address} onChange={(e) => set('address', e.target.value)} /></div>
      <div className="grid grid-cols-2 gap-2.5">
        <div><label className="label" htmlFor="pc">Pincode</label><input id="pc" className="input" inputMode="numeric" maxLength={6} value={f.pincode} onChange={(e) => set('pincode', e.target.value.replace(/\D/g, ''))} /></div>
        <div><label className="label" htmlFor="ct">City</label><input id="ct" className="input" value={f.city} onChange={(e) => set('city', e.target.value)} /></div>
      </div>

      <div>
        <span className="label">Shop location</span>
        <div className="card flex items-center justify-between gap-3 p-3.5">
          <span className="text-sm">{f.latitude !== '' ? <span className="flex items-center gap-1.5"><Icon name="pin" size={16} className="text-mint" />{f.latitude}, {f.longitude}</span> : <span className="text-muted">Not set — helps customers find you</span>}</span>
          {f.latitude !== '' && <a className="text-sm font-semibold" target="_blank" rel="noreferrer" href={`https://www.google.com/maps?q=${f.latitude},${f.longitude}`}>View</a>}
        </div>
        <button className="mt-2.5 flex items-center gap-2 text-sm font-semibold text-mint" disabled={locating} onClick={locate}>
          <Icon name="locate" size={18} />{locating ? 'Getting location…' : 'Use my current location (stand at the shop)'}
        </button>
      </div>

      <div>
        <span className="label">Vehicles you service</span>
        <div className="flex gap-2">
          {([['servesBike', 'Bike'], ['servesCar', 'Car']] as const).map(([k, label]) => (
            <button key={k} aria-pressed={f[k]} onClick={() => set(k, !f[k])} className={`chip h-10 px-4 ${f[k] ? 'border-mint bg-mint-soft text-mint' : ''}`}>{f[k] ? '✓ ' : ''}{label}</button>
          ))}
        </div>
      </div>

      <div className="card flex flex-col gap-3 p-4">
        <strong className="text-[15px]">Shop timings</strong>
        <div>
          <span className="label">Open on</span>
          <div className="grid grid-cols-7 gap-1.5">
            {DAYS.map((d) => {
              const on = f.openDays.includes(d.key)
              return <button key={d.key} aria-pressed={on} onClick={() => toggleDay(d.key)}
                className={`h-10 rounded-[10px] border text-xs font-semibold ${on ? 'border-mint bg-mint-soft text-mint' : 'border-line bg-surface2 text-muted'}`}>{d.short}</button>
            })}
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2.5">
          <div><label className="label" htmlFor="op">Opening time</label><input id="op" type="time" className="input" value={f.openTime} onChange={(e) => set('openTime', e.target.value)} /></div>
          <div><label className="label" htmlFor="cl">Closing time</label><input id="cl" type="time" className="input" value={f.closeTime} onChange={(e) => set('closeTime', e.target.value)} /></div>
        </div>
        <span className="text-xs text-muted">Customers can only book time slots within these hours.</span>
      </div>

      <div>
        <span className="label">Shop photos (up to 5)</span>
        <div className="grid grid-cols-4 gap-2">
          {p.photos.map((ph) => (
            <div key={ph.id} className="relative aspect-square">
              <img src={ph.url} alt="Shop" className="h-full w-full rounded-xl object-cover" />
              <button aria-label="Remove photo" onClick={() => removePhoto(ph.id)} className="absolute -right-1.5 -top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-bg text-danger"><Icon name="x" size={16} /></button>
            </div>
          ))}
          {p.photos.length < 5 && (
            <button onClick={() => fileRef.current?.click()} disabled={busy} className="flex aspect-square flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-mint text-xs font-semibold text-mint">
              <Icon name="camera" />Add
            </button>
          )}
        </div>
        <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => { const file = e.target.files?.[0]; if (file) upload(file); e.target.value = '' }} />
      </div>

      {!first && <PushCard />}
      <ErrorBox msg={error} />
      <button className="btn-primary w-full" disabled={busy} onClick={save}>{first ? 'Next: add services & prices' : 'Save changes'}</button>
      {!first && <button className="btn-ghost w-full" onClick={logout}><Icon name="logout" size={18} />Log out</button>}
    </Page>
  )
}
