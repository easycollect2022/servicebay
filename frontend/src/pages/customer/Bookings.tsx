import { useCallback, useEffect, useState } from 'react'
import { api, errMsg } from '../../lib/api'
import { duration, rupees, slotLabel, time12, vehicleLabel } from '../../lib/format'
import type { CustomerBooking } from '../../lib/types'
import { CustomerNav, Empty, ErrorBox, Page, Spinner, StatusPill, useToast } from '../../components/ui'
import { Icon } from '../../components/Icon'

const ACTIVE = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS']

export default function Bookings() {
  const [list, setList] = useState<CustomerBooking[] | null>(null)
  const [tab, setTab] = useState<'upcoming' | 'past'>('upcoming')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => api<CustomerBooking[]>('/api/bookings').then(setList).catch((e) => setError(errMsg(e))), [])
  useEffect(() => {
    load()
    const t = setInterval(load, 20000) // keep status fresh while open
    return () => clearInterval(t)
  }, [load])

  const upcoming = (list ?? []).filter((b) => ACTIVE.includes(b.status)).reverse()
  const past = (list ?? []).filter((b) => !ACTIVE.includes(b.status))
  const shown = tab === 'upcoming' ? upcoming : past

  return (
    <Page bottomNav={<CustomerNav />}>
      <h1 className="mb-4 font-display text-2xl font-bold">My bookings</h1>
      <div role="tablist" className="mb-4 flex gap-6 border-b border-line">
        {(['upcoming', 'past'] as const).map((t) => (
          <button key={t} role="tab" aria-selected={tab === t} onClick={() => setTab(t)}
            className={`pb-2.5 text-[15px] font-semibold ${tab === t ? 'border-b-2 border-mint' : 'text-muted'}`}>
            {t === 'upcoming' ? `Upcoming (${upcoming.length})` : 'Past'}
          </button>
        ))}
      </div>
      <ErrorBox msg={error} />
      {!list ? <Spinner /> : shown.length === 0
        ? <Empty icon="calendar" title={tab === 'upcoming' ? 'No upcoming bookings' : 'No past bookings'} text="Book a service from the Home tab." />
        : <div className="flex flex-col gap-3">{shown.map((b) => <BookingCard key={b.id} b={b} onChange={load} />)}</div>}
    </Page>
  )
}

function BookingCard({ b, onChange }: { b: CustomerBooking; onChange: () => void }) {
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [error, setError] = useState<string | null>(null)
  const steps = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED']
  const stepIdx = steps.indexOf(b.status)

  async function cancel() {
    if (!confirmCancel()) return
    setBusy(true)
    try { await api(`/api/bookings/${b.id}/cancel`, { method: 'POST' }); onChange() } catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }
  async function rate() {
    if (!rating) return setError('Tap a star to rate')
    setBusy(true); setError(null)
    try { await api(`/api/bookings/${b.id}/review`, { body: { rating, comment } }); toast('Thanks for your rating!'); onChange() }
    catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <div className="card flex flex-col gap-3 p-4">
      <div className="flex items-center justify-between gap-2"><strong>{b.shopName} · {vehicleLabel(b.vehicleType)}</strong><StatusPill status={b.status} /></div>
      <div className="flex flex-col gap-1.5 text-sm">
        <span className="flex items-center gap-2"><Icon name="calendar" size={16} className="text-mint" />{slotLabel(b.slotStart)} – {time12(b.slotEnd.split('T')[1])}</span>
        <ul className="flex flex-col gap-1 rounded-xl bg-surface2 px-3 py-2.5">
          {b.items.map((i) => <li key={i.serviceId} className="flex justify-between"><span>{i.name}</span><span className="text-muted">{rupees(i.price)}</span></li>)}
          <li className="flex justify-between border-t border-line pt-1.5 font-semibold"><span>Total · {duration(b.durationMin)}</span><span>{rupees(b.price)}</span></li>
        </ul>
        <span className="text-xs text-muted">Booking {b.code} · pay at shop</span>
      </div>

      {stepIdx >= 0 && stepIdx < 3 && (
        <div className="flex items-center gap-1.5 text-[11px] text-muted" aria-label="Booking progress">
          {['Requested', 'Accepted', 'Started'].map((label, i) => (
            <span key={label} className="contents">
              {i > 0 && <span className={`h-0.5 flex-1 ${i <= stepIdx ? 'bg-mint' : 'bg-line'}`} />}
              <span className={`h-2.5 w-2.5 rounded-full ${i <= stepIdx ? 'bg-mint' : 'border-2 border-[#4A5468]'}`} />
              <span>{label}</span>
            </span>
          ))}
        </div>
      )}

      {b.otp && (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-dashed border-mint bg-surface2 px-3.5 py-3">
          <div><span className="block text-[13px] font-semibold">Service start OTP</span><span className="text-xs text-muted">Share with the shop only when they start work</span></div>
          <strong className="font-display text-[26px] tracking-[6px] text-mint">{b.otp}</strong>
        </div>
      )}

      {(b.status === 'ACCEPTED' || b.status === 'IN_PROGRESS') && (
        <div className="grid grid-cols-2 gap-2.5">
          {b.shopPhone && <a href={`tel:${b.shopPhone}`} className="btn-ghost h-11 text-sm"><Icon name="phone" size={16} />Call shop</a>}
          <a target="_blank" rel="noreferrer" className="btn-ghost h-11 text-sm"
            href={b.shopLatitude != null ? `https://www.google.com/maps/dir/?api=1&destination=${b.shopLatitude},${b.shopLongitude}` : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(b.shopAddress ?? b.shopName)}`}>
            <Icon name="nav" size={16} />Directions
          </a>
        </div>
      )}
      {(b.status === 'REQUESTED' || b.status === 'ACCEPTED') && (
        <button className="btn-danger h-11 text-sm" disabled={busy} onClick={cancel}>Cancel booking</button>
      )}

      {b.status === 'COMPLETED' && (b.myRating
        ? <p className="flex items-center gap-2 text-sm text-muted">You rated <span className="text-star">{'★'.repeat(b.myRating)}</span></p>
        : (
          <div className="flex flex-col gap-2.5 border-t border-line pt-3">
            <span className="text-sm font-semibold">Rate this service</span>
            <div className="-ml-2.5 flex">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} aria-label={`${n} star${n > 1 ? 's' : ''}`} onClick={() => setRating(n)} className="flex h-11 w-11 items-center justify-center">
                  <Icon name="star" size={30} filled={n <= rating} className={n <= rating ? 'text-star' : 'text-[#4A5468]'} />
                </button>
              ))}
            </div>
            <label className="sr-only" htmlFor={`rv${b.id}`}>Review</label>
            <textarea id={`rv${b.id}`} rows={2} maxLength={1000} placeholder="Write a review (optional)" value={comment} onChange={(e) => setComment(e.target.value)}
              className="resize-none rounded-xl border border-line bg-surface2 px-3.5 py-3 text-sm outline-none focus:border-mint" />
            <button className="btn-primary h-12" disabled={busy} onClick={rate}>Submit rating</button>
          </div>
        ))}
      <ErrorBox msg={error} />
    </div>
  )
}

const confirmCancel = () => window.confirm('Cancel this booking?')
