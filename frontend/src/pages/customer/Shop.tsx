import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api, errMsg } from '../../lib/api'
import { DAYS, rupees, time12, timeAgo } from '../../lib/format'
import type { MerchantDetail, ReviewSummary, VehicleType } from '../../lib/types'
import { ErrorBox, Page, Spinner, Stars } from '../../components/ui'
import { Icon } from '../../components/Icon'

export default function Shop() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const nav = useNavigate()
  const [data, setData] = useState<MerchantDetail | null>(null)
  const [reviews, setReviews] = useState<ReviewSummary | null>(null)
  const [tab, setTab] = useState<'services' | 'reviews' | 'about'>('services')
  const [vehicle, setVehicle] = useState<VehicleType>((params.get('vehicle') as VehicleType) || 'BIKE')
  const [selected, setSelected] = useState<number | null>(null)
  const [photo, setPhoto] = useState(0)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api<MerchantDetail>(`/api/merchants/${id}`).then((d) => {
      setData(d)
      if (!d.shop.servesBike) setVehicle('CAR')
      if (!d.shop.servesCar) setVehicle('BIKE')
    }).catch((e) => setError(errMsg(e)))
    api<ReviewSummary>(`/api/merchants/${id}/reviews`).then(setReviews).catch(() => undefined)
  }, [id])

  if (error) return <Page><ErrorBox msg={error} /></Page>
  if (!data) return <Page><Spinner /></Page>
  const { shop } = data
  const services = data.services.filter((s) => s.vehicleType === vehicle)
  const chosen = services.find((s) => s.id === selected)
  const mapUrl = shop.latitude != null
    ? `https://www.google.com/maps/dir/?api=1&destination=${shop.latitude},${shop.longitude}`
    : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${shop.address} ${shop.pincode}`)}`

  return (
    <div className="mx-auto min-h-screen max-w-md pb-32">
      <div className="relative flex h-[220px] items-center justify-center bg-surface2 text-faint">
        {shop.photos.length ? <img src={shop.photos[photo].url} alt={`${shop.shopName} photo ${photo + 1}`} className="h-full w-full object-cover" /> : <Icon name="image" size={40} />}
        <button aria-label="Back" onClick={() => nav(-1)} className="absolute left-4 top-4 flex h-11 w-11 items-center justify-center rounded-xl bg-bg/80"><Icon name="back" /></button>
        {shop.photos.length > 1 && (
          <button onClick={() => setPhoto((p) => (p + 1) % shop.photos.length)} className="absolute bottom-3 right-4 rounded-full bg-bg/80 px-3 py-1 text-xs">
            {photo + 1} / {shop.photos.length} photos ›
          </button>
        )}
      </div>

      <div className="flex flex-col gap-4 px-5 pt-5">
        <div className="flex flex-col gap-1.5">
          <h1 className="font-display text-2xl font-bold">{shop.shopName}</h1>
          <div className="flex flex-wrap items-center gap-1.5 text-sm">
            <Icon name="star" size={14} filled className="text-star" />
            <strong>{shop.ratingCount ? Number(shop.ratingAvg).toFixed(1) : 'New'}</strong>
            {shop.ratingCount > 0 && <button className="text-mint" onClick={() => setTab('reviews')}>{shop.ratingCount} reviews</button>}
            <span className="text-muted">· {data.openNow ? 'Open' : 'Closed now'} · {time12(shop.openTime)} – {time12(shop.closeTime)}</span>
          </div>
          <p className="text-sm leading-relaxed text-muted">{shop.address}{shop.city ? `, ${shop.city}` : ''} – {shop.pincode}</p>
        </div>

        <div className="grid grid-cols-2 gap-2.5">
          <a href={`tel:${shop.phone}`} className="chip h-11 justify-center rounded-xl"><Icon name="phone" size={18} />Call shop</a>
          <a href={mapUrl} target="_blank" rel="noreferrer" className="chip h-11 justify-center rounded-xl">
            <Icon name="nav" size={18} />Directions{data.distanceKm != null ? ` · ${data.distanceKm} km` : ''}
          </a>
        </div>

        <div role="tablist" className="flex gap-6 border-b border-line">
          {(['services', 'reviews', 'about'] as const).map((t) => (
            <button key={t} role="tab" aria-selected={tab === t} onClick={() => setTab(t)}
              className={`pb-2.5 text-[15px] font-semibold capitalize ${tab === t ? 'border-b-2 border-mint' : 'text-muted'}`}>{t}</button>
          ))}
        </div>

        {tab === 'services' && (
          <>
            {shop.servesBike && shop.servesCar && (
              <div className="flex gap-2">
                {(['BIKE', 'CAR'] as VehicleType[]).map((v) => (
                  <button key={v} className={`chip ${vehicle === v ? 'chip-on' : ''}`} onClick={() => { setVehicle(v); setSelected(null) }}>{v === 'BIKE' ? 'Bike' : 'Car'}</button>
                ))}
              </div>
            )}
            {services.length === 0 ? <p className="text-muted">No {vehicle === 'BIKE' ? 'bike' : 'car'} services listed yet.</p> : (
              <fieldset className="card overflow-hidden">
                <legend className="sr-only">Choose a service</legend>
                {services.map((s, i) => (
                  <label key={s.id} className={`flex cursor-pointer items-start gap-3 p-3.5 ${i ? 'border-t border-line' : ''} ${selected === s.id ? 'bg-[#1A2A24]' : ''}`}>
                    <input type="radio" name="svc" className="mt-1 h-5 w-5 accent-[#3DDC97]" checked={selected === s.id} onChange={() => setSelected(s.id)} />
                    {s.imageUrl && <img src={s.imageUrl} alt="" className="h-12 w-12 rounded-lg object-cover" />}
                    <span className="flex flex-1 flex-col gap-0.5">
                      <span className="flex justify-between gap-2"><strong>{s.name}</strong><strong>{rupees(s.price)}</strong></span>
                      <span className="text-[13px] text-muted">{s.description ? `${s.description} · ` : ''}{s.durationMin} min</span>
                    </span>
                  </label>
                ))}
              </fieldset>
            )}
          </>
        )}

        {tab === 'reviews' && (
          !reviews || reviews.reviews.length === 0 ? <p className="text-muted">No reviews yet.</p> : (
            <div className="flex flex-col gap-2.5">
              {reviews.reviews.map((r) => (
                <div key={r.id} className="card flex flex-col gap-1.5 p-3.5">
                  <div className="flex justify-between text-sm"><strong>{r.customerName}</strong><Stars value={r.rating} /></div>
                  <span className="text-xs text-muted">{r.serviceName} · {timeAgo(r.createdAt)}</span>
                  {r.comment && <p className="text-sm">{r.comment}</p>}
                  {r.reply && <p className="rounded-lg bg-surface2 px-3 py-2 text-[13px]"><strong>Shop reply:</strong> <span className="text-muted">{r.reply}</span></p>}
                </div>
              ))}
            </div>
          )
        )}

        {tab === 'about' && (
          <div className="card flex flex-col gap-2 p-4 text-sm">
            <p><span className="text-muted">Open on: </span>{DAYS.filter((d) => shop.openDays.includes(d.key)).map((d) => d.short).join(', ')}</p>
            <p><span className="text-muted">Timings: </span>{time12(shop.openTime)} – {time12(shop.closeTime)}</p>
            <p><span className="text-muted">Vehicles: </span>{[shop.servesBike && 'Bike', shop.servesCar && 'Car'].filter(Boolean).join(', ')}</p>
            <p><span className="text-muted">Phone: </span>{shop.phone}</p>
          </div>
        )}
      </div>

      <div className="fixed inset-x-0 bottom-0 border-t border-line bg-[#11151C] pb-[env(safe-area-inset-bottom)]">
        <div className="mx-auto flex max-w-md items-center gap-3.5 px-5 py-3.5">
          <div className="flex flex-col"><span className="text-xs text-muted">{chosen ? chosen.name : 'Select a service'}</span><strong className="text-lg">{chosen ? rupees(chosen.price) : '—'}</strong></div>
          {chosen
            ? <Link to={`/book/${shop.id}/${chosen.id}`} className="btn-primary flex-1">Choose date &amp; time</Link>
            : <button className="btn-primary flex-1" disabled>Choose date &amp; time</button>}
        </div>
      </div>
    </div>
  )
}
