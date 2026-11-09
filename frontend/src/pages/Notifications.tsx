import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errMsg } from '../lib/api'
import { useAuth } from '../lib/auth'
import { timeAgo } from '../lib/format'
import type { NotificationList } from '../lib/types'
import { CustomerNav, Empty, ErrorBox, Header, MerchantNav, Page, Spinner } from '../components/ui'
import { Icon } from '../components/Icon'

const STYLE: Record<string, { icon: string; cls: string }> = {
  BOOKING_ACCEPTED: { icon: 'check', cls: 'bg-mint-soft text-mint' },
  SERVICE_STARTED: { icon: 'wrench', cls: 'bg-mint-soft text-mint' },
  BOOKING_REMINDER: { icon: 'clock', cls: 'bg-amber-soft text-star' },
  BOOKING_DECLINED: { icon: 'x', cls: 'bg-danger-soft text-danger' },
  BOOKING_CANCELLED: { icon: 'x', cls: 'bg-danger-soft text-danger' },
  SERVICE_COMPLETED: { icon: 'star', cls: 'bg-surface2 text-star' },
  NEW_REVIEW: { icon: 'star', cls: 'bg-surface2 text-star' },
  BOOKING_REQUESTED: { icon: 'bell', cls: 'bg-mint-soft text-mint' },
}

export default function Notifications() {
  const { me } = useAuth()
  const merchant = me?.role === 'MERCHANT'
  const [data, setData] = useState<NotificationList | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api<NotificationList>('/api/me/notifications').then((d) => {
      setData(d)
      if (d.unread) api('/api/me/notifications/read-all', { method: 'POST' }).catch(() => undefined)
    }).catch((e) => setError(errMsg(e)))
  }, [])

  const linkFor = (type: string, bookingId?: number) =>
    merchant ? (type === 'NEW_REVIEW' ? '/merchant/reviews' : bookingId ? `/merchant/orders/${bookingId}` : '/merchant') : '/bookings'

  return (
    <Page bottomNav={merchant ? <MerchantNav /> : <CustomerNav />}>
      <Header title="Notifications" back />
      <ErrorBox msg={error} />
      {!data ? <Spinner /> : data.items.length === 0 ? <Empty icon="bell" title="No notifications yet" /> : (
        <ul className="flex flex-col">
          {data.items.map((n) => {
            const s = STYLE[n.type] ?? { icon: 'bell', cls: 'bg-surface2 text-mint' }
            return (
              <li key={n.id} className="border-b border-line last:border-0">
                <Link to={linkFor(n.type, n.bookingId)} className="flex gap-3 py-3.5">
                  <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${s.cls}`}><Icon name={s.icon} /></span>
                  <span className="flex flex-1 flex-col gap-0.5">
                    <span className="flex justify-between gap-2"><strong className="text-sm">{n.title}</strong><span className="shrink-0 text-xs text-muted">{timeAgo(n.createdAt)}</span></span>
                    <span className="text-[13px] leading-snug text-muted">{n.body}</span>
                  </span>
                  {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-mint" aria-label="Unread" />}
                </Link>
              </li>
            )
          })}
        </ul>
      )}
    </Page>
  )
}
