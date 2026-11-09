import type { BookingStatus, Day, ServiceCategory, VehicleType } from './types'

export const CATEGORIES: { key: ServiceCategory; label: string }[] = [
  { key: 'WATER_WASH', label: 'Water wash' },
  { key: 'CLEANING', label: 'Cleaning' },
  { key: 'FULL_SERVICE', label: 'Full service' },
  { key: 'PUNCTURE', label: 'Puncture' },
  { key: 'TYRE', label: 'Tyre' },
  { key: 'OIL_CHANGE', label: 'Oil change' },
  { key: 'BATTERY', label: 'Battery' },
]
export const categoryLabel = (c: ServiceCategory) => CATEGORIES.find((x) => x.key === c)?.label ?? c

export const DAYS: { key: Day; short: string }[] = [
  { key: 'MONDAY', short: 'Mon' }, { key: 'TUESDAY', short: 'Tue' }, { key: 'WEDNESDAY', short: 'Wed' },
  { key: 'THURSDAY', short: 'Thu' }, { key: 'FRIDAY', short: 'Fri' }, { key: 'SATURDAY', short: 'Sat' },
  { key: 'SUNDAY', short: 'Sun' },
]
const JS_DAY: Day[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

export const vehicleLabel = (v: VehicleType) => (v === 'BIKE' ? 'Bike' : 'Car')

export function rupees(n: number | undefined | null): string {
  if (n == null) return ''
  return '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 2 })
}

/** "YYYY-MM-DD" in local time. */
export function isoDate(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export interface DateChip { iso: string; dow: string; day: number; month: string; open: boolean; isToday: boolean }

/** Next `count` days starting today, flagged open/closed from the shop's open days. */
export function dateStrip(today: Date, count: number, openDays: Day[]): DateChip[] {
  const out: DateChip[] = []
  for (let i = 0; i < count; i++) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i)
    out.push({
      iso: isoDate(d),
      dow: i === 0 ? 'Today' : d.toLocaleDateString('en-IN', { weekday: 'short' }),
      day: d.getDate(),
      month: d.toLocaleDateString('en-IN', { month: 'short' }),
      open: openDays.includes(JS_DAY[d.getDay()]),
      isToday: i === 0,
    })
  }
  return out
}

/** "16:30" or "16:30:00" -> "4:30 PM" */
export function time12(t: string): string {
  const [h, m] = t.split(':').map(Number)
  const ampm = h >= 12 ? 'PM' : 'AM'
  const h12 = h % 12 === 0 ? 12 : h % 12
  return `${h12}:${String(m).padStart(2, '0')} ${ampm}`
}

/** "2026-11-10T16:30:00" -> "Tue, 10 Nov · 4:30 PM" */
export function slotLabel(iso: string): string {
  const [date, t] = iso.split('T')
  const [y, mo, d] = date.split('-').map(Number)
  const dt = new Date(y, mo - 1, d)
  const day = dt.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
  return `${day} · ${time12(t)}`
}

export type SlotGroup = 'Morning' | 'Afternoon' | 'Evening'
export function slotGroup(t: string): SlotGroup {
  const h = Number(t.split(':')[0])
  return h < 12 ? 'Morning' : h < 16 ? 'Afternoon' : 'Evening'
}

export function timeAgo(iso: string, now = new Date()): string {
  const s = Math.max(0, (now.getTime() - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'now'
  if (s < 3600) return `${Math.floor(s / 60)} min ago`
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`
  if (s < 7 * 86400) return `${Math.floor(s / 86400)} d ago`
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
}

export const STATUS: Record<BookingStatus, { label: string; cls: string }> = {
  REQUESTED: { label: 'Waiting for shop', cls: 'bg-amber-soft text-star' },
  ACCEPTED: { label: 'Accepted', cls: 'bg-mint-soft text-mint' },
  IN_PROGRESS: { label: 'In progress', cls: 'bg-mint-soft text-mint' },
  COMPLETED: { label: 'Completed', cls: 'bg-surface2 text-muted' },
  DECLINED: { label: 'Declined', cls: 'bg-danger-soft text-danger' },
  CANCELLED: { label: 'Cancelled', cls: 'bg-surface2 text-muted' },
}

export const isValidPincode = (p: string) => /^[1-9][0-9]{5}$/.test(p)
export const isValidMobile = (p: string) => /^[6-9][0-9]{9}$/.test(p.replace(/\s/g, ''))

/** 50 -> "50 min", 90 -> "1 h 30 min", 180 -> "3 h" */
export function duration(min: number): string {
  if (min < 60) return `${min} min`
  const h = Math.floor(min / 60), m = min % 60
  return m ? `${h} h ${m} min` : `${h} h`
}
