import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, errMsg } from '../../lib/api'
import { rupees, slotLabel, vehicleLabel } from '../../lib/format'
import type { MerchantBooking } from '../../lib/types'
import { ErrorBox, Header, Page, Spinner, StatusPill, useToast } from '../../components/ui'
import { Icon } from '../../components/Icon'

export default function OrderDetail() {
  const { id } = useParams()
  const toast = useToast()
  const [b, setB] = useState<MerchantBooking | null>(null)
  const [otp, setOtp] = useState(['', '', '', ''])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const refs = useRef<(HTMLInputElement | null)[]>([])

  useEffect(() => { api<MerchantBooking>(`/api/merchant/bookings/${id}`).then(setB).catch((e) => setError(errMsg(e))) }, [id])

  async function act(action: 'accept' | 'decline' | 'complete' | 'start') {
    setBusy(true); setError(null)
    try {
      const body = action === 'start' ? { otp: otp.join('') } : undefined
      const res = await api<MerchantBooking>(`/api/merchant/bookings/${id}/${action}`, { method: 'POST', body })
      setB(res)
      if (action === 'start') toast('OTP verified', 'Service started.')
      if (action === 'complete') toast('Service completed', 'The customer has been asked to rate you.')
    } catch (e) {
      setError(errMsg(e))
      if (action === 'start') { setOtp(['', '', '', '']); refs.current[0]?.focus() }
    } finally { setBusy(false) }
  }

  function typeDigit(i: number, v: string) {
    const d = v.replace(/\D/g, '')
    if (d.length > 1) { // pasted full OTP
      const all = d.slice(0, 4).split('')
      setOtp([0, 1, 2, 3].map((k) => all[k] ?? ''))
      refs.current[Math.min(all.length, 3)]?.focus()
      return
    }
    const next = [...otp]; next[i] = d; setOtp(next)
    if (d && i < 3) refs.current[i + 1]?.focus()
  }

  if (!b) return <Page>{error ? <ErrorBox msg={error} /> : <Spinner />}</Page>
  const contact = !!b.customerPhone
  const full = otp.every((d) => d)

  return (
    <div className="mx-auto min-h-screen max-w-md pb-36">
      <div className="flex flex-col gap-4 px-5 pt-5">
        <Header title={`Booking ${b.code}`} back="/merchant" right={<StatusPill status={b.status} />} />

        <div className="flex items-center gap-3 rounded-2xl border border-[#2A6B50] bg-mint-soft p-4">
          <Icon name="calendar" size={28} className="text-mint" />
          <div><span className="block text-xs text-[#9FE6C4]">Scheduled slot</span><strong className="font-display text-lg">{slotLabel(b.slotStart)}</strong></div>
        </div>

        <h2 className="section">Customer</h2>
        <div className="card flex flex-col gap-3.5 p-4">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-surface2 font-bold">{(b.customerName ?? 'C').split(' ').map((s) => s[0]).join('').slice(0, 2).toUpperCase()}</span>
            <div className="flex flex-1 flex-col"><strong>{b.customerName ?? 'Customer'}</strong><span className="text-sm text-muted">{contact ? b.customerPhone : 'Phone hidden until you accept'}</span></div>
            {contact && <a href={`tel:${b.customerPhone}`} aria-label="Call customer" className="flex h-11 w-11 items-center justify-center rounded-xl bg-mint text-mint-ink"><Icon name="phone" /></a>}
          </div>
          <div className="h-px bg-line" />
          <div className="flex items-start gap-3">
            <Icon name={contact ? 'pin' : 'lock'} className="mt-0.5 text-mint" />
            <span className="flex-1 text-sm leading-relaxed">{contact ? `${b.customerAddress} – ${b.customerPincode}` : `Pincode ${b.customerPincode}. Full address is shown after you accept.`}</span>
            {contact && <a aria-label="Navigate" target="_blank" rel="noreferrer" href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${b.customerAddress} ${b.customerPincode}`)}`}
              className="flex h-11 w-11 items-center justify-center rounded-xl border border-line bg-surface2"><Icon name="nav" /></a>}
          </div>
        </div>

        <h2 className="section">Service</h2>
        <div className="card flex flex-col gap-2.5 p-4 text-sm">
          <div className="flex justify-between"><span className="text-muted">Service</span><strong>{b.serviceName}</strong></div>
          <div className="flex justify-between"><span className="text-muted">Vehicle</span><strong>{vehicleLabel(b.vehicleType)}</strong></div>
          <div className="flex justify-between"><span className="text-muted">Amount · pay at shop</span><strong>{rupees(b.price)}</strong></div>
        </div>

        {b.status === 'ACCEPTED' && (
          <>
            <h2 className="section">Start service</h2>
            <div className="card flex flex-col gap-3 border-mint p-4">
              <label htmlFor="otp0" className="text-sm font-semibold">Enter the customer's 4-digit OTP</label>
              <div className="grid grid-cols-4 gap-2.5">
                {otp.map((d, i) => (
                  <input key={i} id={`otp${i}`} ref={(el) => { refs.current[i] = el }} aria-label={`OTP digit ${i + 1}`} inputMode="numeric" autoComplete="one-time-code"
                    value={d} onChange={(e) => typeDigit(i, e.target.value)} onKeyDown={(e) => { if (e.key === 'Backspace' && !d && i > 0) refs.current[i - 1]?.focus() }}
                    className={`h-14 rounded-xl border bg-surface2 text-center text-2xl font-bold outline-none ${d ? 'border-mint' : 'border-line'} focus:border-mint`} />
                ))}
              </div>
              <span className="text-xs text-muted">The customer sees this OTP in their My bookings screen. The service can't start without it.</span>
            </div>
          </>
        )}
        <ErrorBox msg={error} />
      </div>

      {['REQUESTED', 'ACCEPTED', 'IN_PROGRESS'].includes(b.status) && (
        <div className="fixed inset-x-0 bottom-0 border-t border-line bg-[#11151C] pb-[env(safe-area-inset-bottom)]">
          <div className="mx-auto flex max-w-md gap-2.5 px-5 py-3.5">
            {b.status === 'REQUESTED' && <>
              <button className="btn-danger flex-1" disabled={busy} onClick={() => act('decline')}>Decline</button>
              <button className="btn-primary flex-[2]" disabled={busy} onClick={() => act('accept')}>Accept</button>
            </>}
            {b.status === 'ACCEPTED' && <button className="btn-primary flex-1" disabled={busy || !full} onClick={() => act('start')}>Verify OTP &amp; start</button>}
            {b.status === 'IN_PROGRESS' && <button className="btn-primary flex-1" disabled={busy} onClick={() => act('complete')}>Mark as completed</button>}
          </div>
        </div>
      )}
    </div>
  )
}
