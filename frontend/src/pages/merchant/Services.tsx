import { useEffect, useRef, useState } from 'react'
import { api, errMsg } from '../../lib/api'
import { CATEGORIES, categoryLabel, rupees } from '../../lib/format'
import type { MerchantProfile, ServiceCategory, ServiceItem, VehicleType } from '../../lib/types'
import { Empty, ErrorBox, MerchantNav, Page, Spinner, Thumb, Toggle, useToast } from '../../components/ui'
import { CATEGORY_ICON, Icon } from '../../components/Icon'

interface Draft { id?: number; category: ServiceCategory; vehicleType: VehicleType; name: string; description: string; price: string; durationMin: string; imageUrl?: string; active: boolean }
const blank = (v: VehicleType): Draft => ({ category: 'WATER_WASH', vehicleType: v, name: 'Water wash', description: '', price: '', durationMin: '30', active: true })

export default function Services() {
  const toast = useToast()
  const [list, setList] = useState<ServiceItem[] | null>(null)
  const [profile, setProfile] = useState<MerchantProfile | null>(null)
  const [vehicle, setVehicle] = useState<VehicleType>('BIKE')
  const [draft, setDraft] = useState<Draft | null>(null)
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  const load = () => api<ServiceItem[]>('/api/merchant/services').then(setList).catch((e) => setError(errMsg(e)))
  useEffect(() => {
    load()
    api<MerchantProfile>('/api/merchant/profile').then((p) => { setProfile(p); if (!p.servesBike) setVehicle('CAR') }).catch(() => undefined)
  }, [])

  const shown = (list ?? []).filter((s) => s.vehicleType === vehicle)
  const count = (v: VehicleType) => (list ?? []).filter((s) => s.vehicleType === v && s.active).length

  function edit(s: ServiceItem) {
    setFile(null)
    setDraft({ id: s.id, category: s.category, vehicleType: s.vehicleType, name: s.name, description: s.description ?? '', price: String(s.price), durationMin: String(s.durationMin), imageUrl: s.imageUrl, active: s.active })
  }

  async function save() {
    if (!draft) return
    setError(null)
    const price = Number(draft.price), durationMin = Number(draft.durationMin)
    if (!draft.name.trim()) return setError('Enter a service name')
    if (!(price >= 0) || draft.price === '') return setError('Enter a valid price')
    if (!(durationMin >= 5)) return setError('Duration must be at least 5 minutes')
    setBusy(true)
    try {
      const body = { category: draft.category, vehicleType: draft.vehicleType, name: draft.name.trim(), description: draft.description.trim() || null, price, durationMin, active: draft.active }
      const saved = await api<ServiceItem>(draft.id ? `/api/merchant/services/${draft.id}` : '/api/merchant/services', { method: draft.id ? 'PUT' : 'POST', body })
      if (file) { const form = new FormData(); form.append('file', file); await api(`/api/merchant/services/${saved.id}/image`, { form }) }
      toast('Service saved'); setDraft(null); setFile(null); load()
    } catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }

  async function setActive(s: ServiceItem, active: boolean) {
    try {
      await api(`/api/merchant/services/${s.id}`, { method: 'PUT', body: { category: s.category, vehicleType: s.vehicleType, name: s.name, description: s.description, price: s.price, durationMin: s.durationMin, active } })
      load()
    } catch (e) { setError(errMsg(e)) }
  }

  const vehicles: VehicleType[] = profile ? (['BIKE', 'CAR'] as VehicleType[]).filter((v) => (v === 'BIKE' ? profile.servesBike : profile.servesCar)) : ['BIKE', 'CAR']

  return (
    <Page bottomNav={<MerchantNav />} className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-bold">My services</h1>
        <button aria-label="Add service" onClick={() => { setFile(null); setDraft(blank(vehicle)) }} className="flex h-11 w-11 items-center justify-center rounded-xl bg-mint text-mint-ink"><Icon name="plus" /></button>
      </div>
      {vehicles.length > 1 && (
        <div className="flex gap-2">
          {vehicles.map((v) => <button key={v} className={`chip ${vehicle === v ? 'chip-on' : ''}`} onClick={() => setVehicle(v)}>{v === 'BIKE' ? 'Bike' : 'Car'} ({count(v)})</button>)}
        </div>
      )}

      {draft && (
        <div className="card flex flex-col gap-3.5 border-mint p-4">
          <div className="flex items-center justify-between"><strong>{draft.id ? `Edit · ${draft.name}` : 'New service'}</strong><span className="text-xs text-muted">{draft.vehicleType === 'BIKE' ? 'Bike' : 'Car'}</span></div>
          <div>
            <label className="label" htmlFor="cat">Category</label>
            <select id="cat" className="input" value={draft.category}
              onChange={(e) => { const c = e.target.value as ServiceCategory; setDraft({ ...draft, category: c, name: draft.id ? draft.name : categoryLabel(c) }) }}>
              {CATEGORIES.map((c) => <option key={c.key} value={c.key}>{c.label}</option>)}
            </select>
          </div>
          <div><label className="label" htmlFor="nm">Service name</label><input id="nm" className="input" value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} /></div>
          <div className="grid grid-cols-2 gap-2.5">
            <div><label className="label" htmlFor="pr">Price (₹)</label><input id="pr" className="input" inputMode="decimal" value={draft.price} onChange={(e) => setDraft({ ...draft, price: e.target.value.replace(/[^\d.]/g, '') })} /></div>
            <div><label className="label" htmlFor="du">Duration (min)</label><input id="du" className="input" inputMode="numeric" value={draft.durationMin} onChange={(e) => setDraft({ ...draft, durationMin: e.target.value.replace(/\D/g, '') })} /></div>
          </div>
          <div><label className="label" htmlFor="ds">Description</label><textarea id="ds" rows={2} maxLength={500} className="input h-auto resize-none py-3 leading-snug" value={draft.description} onChange={(e) => setDraft({ ...draft, description: e.target.value })} /></div>
          <div>
            <span className="label">Service image</span>
            <div className="flex items-center gap-2.5">
              {file ? <img src={URL.createObjectURL(file)} alt="" className="h-16 w-16 rounded-xl object-cover" /> : <Thumb src={draft.imageUrl} size={64} />}
              <button className="chip text-mint" onClick={() => fileRef.current?.click()}><Icon name="camera" size={16} />{draft.imageUrl || file ? 'Change' : 'Upload'}</button>
              <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => { setFile(e.target.files?.[0] ?? null); e.target.value = '' }} />
            </div>
          </div>
          <ErrorBox msg={error} />
          <div className="grid grid-cols-2 gap-2.5">
            <button className="btn-ghost h-12 text-sm" onClick={() => { setDraft(null); setError(null) }}>Cancel</button>
            <button className="btn-primary h-12 text-sm" disabled={busy} onClick={save}>Save</button>
          </div>
        </div>
      )}

      {!draft && <ErrorBox msg={error} />}
      {!list ? <Spinner /> : shown.length === 0 ? (
        <Empty icon="wrench" title="No services yet" text="Add the services you offer with your prices." action={<button className="btn-primary mt-2 h-11 text-sm" onClick={() => setDraft(blank(vehicle))}>Add service</button>} />
      ) : (
        <div className="card overflow-hidden">
          {shown.map((s, i) => (
            <div key={s.id} className={`flex items-center gap-3 px-3.5 py-3 ${i ? 'border-t border-line' : ''}`}>
              {s.imageUrl ? <Thumb src={s.imageUrl} size={48} /> : <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-[10px] bg-surface2 text-mint"><Icon name={CATEGORY_ICON[s.category]} /></span>}
              <button className="flex flex-1 flex-col text-left" onClick={() => edit(s)}>
                <strong className="text-[15px]">{s.name}</strong>
                <span className="text-[13px] text-muted">{rupees(s.price)} · {s.durationMin} min{s.active ? '' : ' · Paused'}</span>
              </button>
              <Toggle on={s.active} onChange={(v) => setActive(s, v)} label={`${s.name} ${s.active ? 'active' : 'paused'}`} />
            </div>
          ))}
        </div>
      )}
    </Page>
  )
}
