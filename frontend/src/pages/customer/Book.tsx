import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, ApiError, errMsg } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { dateStrip, isValidMobile, isValidPincode, rupees, slotGroup, time12, vehicleLabel, type SlotGroup } from '../../lib/format'
import type { CustomerBooking, MerchantDetail, SlotsResponse } from '../../lib/types'
import { ErrorBox, Header, Page, Spinner, useToast } from '../../components/ui'
import { Icon } from '../../components/Icon'

const DAYS_AHEAD = 7

export default function Book() {
  const { shopId, serviceId } = useParams()
  const { me } = useAuth()
  const nav = useNavigate()
  const toast = useToast()
  const [shop, setShop] = useState<MerchantDetail | null>(null)
  const [date, setDate] = useState<string | null>(null)
  const [slots, setSlots] = useState<SlotsResponse | null>(null)
  const [time, setTime] = useState<string | null>(null)
  const [address, setAddress] = useState(me?.address ?? '')
  const [pincode, setPincode] = useState(() => { try { return me?.pincode || localStorage.getItem('sb.pincode') || '' } catch { return me?.pincode ?? '' } })
  const [phone, setPhone] = useState((me?.phone ?? '').replace(/^\+91/, ''))
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => { api<MerchantDetail>(`/api/merchants/${shopId}`).then(setShop).catch((e) => setError(errMsg(e))) }, [shopId])

  const days = useMemo(() => (shop ? dateStrip(new Date(), DAYS_AHEAD + 1, shop.shop.openDays) : []), [shop])
  useEffect(() => { if (!date && days.length) setDate((days.find((d) => d.open) ?? days[0]).iso) }, [days, date])

  useEffect(() => {
    if (!date) return
    setSlots(null); setTime(null)
    api<SlotsResponse>(`/api/merchants/${shopId}/slots?date=${date}&serviceId=${serviceId}`).then(setSlots).catch((e) => setError(errMsg(e)))
  }, [date, shopId, serviceId])

  const service = shop?.services.find((s) => String(s.id) === serviceId)
  const groups = useMemo(() => {
    const g: Record<SlotGroup, NonNullable<typeof slots>['slots']> = { Morning: [], Afternoon: [], Evening: [] }
    slots?.slots.forEach((s) => g[slotGroup(s.time)].push(s))
    return g
  }, [slots])

  async function submit() {
    setError(null)
    if (!date || !time) return setError('Pick a date and time slot')
    if (!address.trim()) return setError('Enter your address')
    if (!isValidPincode(pincode)) return setError('Enter a valid 6-digit pincode')
    if (!isValidMobile(phone)) return setError('Enter a valid 10-digit mobile number')
    setBusy(true)
    try {
      await api<CustomerBooking>('/api/bookings', { body: { merchantId: Number(shopId), serviceId: Number(serviceId), date, time, address: address.trim(), pincode, phone: '+91' + phone.replace(/\s/g, '') } })
      toast('Booking requested', 'We will notify you when the shop accepts.')
      nav('/bookings', { replace: true })
    } catch (e) {
      setError(errMsg(e))
      if (e instanceof ApiError && e.code === 'SLOT_UNAVAILABLE' && date) {
        api<SlotsResponse>(`/api/merchants/${shopId}/slots?date=${date}&serviceId=${serviceId}`).then(setSlots).catch(() => undefined)
        setTime(null)
      }
    } finally { setBusy(false) }
  }

  if (!shop) return <Page>{error ? <ErrorBox msg={error} /> : <Spinner />}</Page>
  if (!service) return <Page><ErrorBox msg="This service is no longer available." /></Page>
  const chosenDay = days.find((d) => d.iso === date)

  return (
    <div className="mx-auto min-h-screen max-w-md pb-40">
      <div className="px-5 pt-5">
        <Header title="Schedule booking" back />
        <div className="flex flex-col gap-5">
          <div className="card flex items-center justify-between p-3.5">
            <div><strong className="block">{service.name} · {vehicleLabel(service.vehicleType)}</strong><span className="text-[13px] text-muted">{shop.shop.shopName} · {service.durationMin} min</span></div>
            <strong>{rupees(service.price)}</strong>
          </div>

          <section className="flex flex-col gap-3">
            <h2 className="section">Pick a date</h2>
            <div className="grid grid-cols-8 gap-1.5 overflow-x-auto">
              {days.map((d) => (
                <button key={d.iso} disabled={!d.open} aria-pressed={date === d.iso} onClick={() => setDate(d.iso)}
                  aria-label={`${d.dow} ${d.day} ${d.month}${d.open ? '' : ', closed'}`}
                  className={`flex h-16 flex-col items-center justify-center gap-0.5 rounded-xl border text-ink ${date === d.iso ? 'border-mint bg-mint text-mint-ink' : 'border-line bg-surface'} disabled:border-dashed disabled:bg-transparent disabled:text-faint`}>
                  <span className="text-[10px] font-medium">{d.dow}</span><span className="text-lg font-bold">{d.day}</span>
                </button>
              ))}
            </div>
          </section>

          <section className="flex flex-col gap-3">
            <h2 className="section">Pick a time slot</h2>
            {!slots ? <Spinner label="Loading slots…" /> : !slots.open ? <p className="text-muted">The shop is closed on this day.</p>
              : slots.slots.every((s) => !s.available) ? <p className="text-muted">No free slots on this day. Try another date.</p> : (
                (Object.keys(groups) as SlotGroup[]).filter((g) => groups[g].length).map((g) => (
                  <div key={g} className="flex flex-col gap-2">
                    <span className="text-[13px] font-semibold">{g}</span>
                    <div className="grid grid-cols-4 gap-2">
                      {groups[g].map((s) => (
                        <button key={s.time} disabled={!s.available} aria-pressed={time === s.time} onClick={() => setTime(s.time)}
                          className={`h-11 rounded-[10px] border text-[13px] font-semibold ${time === s.time ? 'border-mint bg-mint text-mint-ink' : 'border-line bg-surface'} disabled:border-dashed disabled:border-[#4A5468] disabled:bg-transparent disabled:text-faint disabled:line-through`}>
                          {time12(s.time).replace(/ (AM|PM)/, '')}
                        </button>
                      ))}
                    </div>
                  </div>
                ))
              )}
          </section>

          <section className="flex flex-col gap-3">
            <h2 className="section">Your details</h2>
            <div><label className="label" htmlFor="addr">Address</label>
              <textarea id="addr" rows={2} className="input h-auto resize-none py-3.5 leading-snug" value={address} onChange={(e) => setAddress(e.target.value)} /></div>
            <div className="grid grid-cols-2 gap-2.5">
              <div><label className="label" htmlFor="pc">Pincode</label><input id="pc" className="input" inputMode="numeric" maxLength={6} value={pincode} onChange={(e) => setPincode(e.target.value.replace(/\D/g, ''))} /></div>
              <div><label className="label" htmlFor="ph">Phone</label><input id="ph" className="input" inputMode="numeric" maxLength={10} value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))} /></div>
            </div>
            <p className="flex items-start gap-2.5 rounded-xl bg-surface p-3 text-[13px] text-muted">
              <Icon name="lock" size={18} className="text-mint" />Your address and phone are shared with the shop only after they accept.
            </p>
            <ErrorBox msg={error} />
          </section>
        </div>
      </div>

      <div className="fixed inset-x-0 bottom-0 border-t border-line bg-[#11151C] pb-[env(safe-area-inset-bottom)]">
        <div className="mx-auto flex max-w-md flex-col gap-3 px-5 py-3.5">
          <p className="flex items-center gap-2.5 text-sm"><Icon name="clock" size={18} className="text-mint" />
            <span><strong>{chosenDay && time ? `${chosenDay.dow === 'Today' ? 'Today' : chosenDay.dow}, ${chosenDay.day} ${chosenDay.month} · ${time12(time)}` : 'Pick a slot'}</strong><span className="text-muted"> · Pay at shop</span></span>
          </p>
          <button className="btn-primary w-full" disabled={busy || !time} onClick={submit}>Request booking · {rupees(service.price)}</button>
        </div>
      </div>
    </div>
  )
}
