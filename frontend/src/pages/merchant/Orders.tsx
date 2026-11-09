import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errMsg } from '../../lib/api'
import { rupees, slotLabel, vehicleLabel } from '../../lib/format'
import type { MerchantBooking, MerchantProfile, NotificationList } from '../../lib/types'
import { Empty, ErrorBox, MerchantNav, Page, Spinner, StatusPill } from '../../components/ui'
import { Icon } from '../../components/Icon'

type Scope = 'pending' | 'upcoming' | 'history'

export default function Orders() {
  const [scope, setScope] = useState<Scope>('pending')
  const [list, setList] = useState<MerchantBooking[] | null>(null)
  const [profile, setProfile] = useState<MerchantProfile | null>(null)
  const [unread, setUnread] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const load = useCallback(() => {
    api<MerchantBooking[]>(`/api/merchant/bookings?scope=${scope}`).then(setList).catch((e) => setError(errMsg(e)))
    api<NotificationList>('/api/me/notifications').then((n) => setUnread(n.unread)).catch(() => undefined)
  }, [scope])

  useEffect(() => { setList(null); load(); const t = setInterval(load, 15000); return () => clearInterval(t) }, [load])
  useEffect(() => { api<MerchantProfile>('/api/merchant/profile').then(setProfile).catch(() => undefined) }, [])

  async function act(id: number, action: 'accept' | 'decline') {
    setBusyId(id); setError(null)
    try { await api(`/api/merchant/bookings/${id}/${action}`, { method: 'POST' }); load() } catch (e) { setError(errMsg(e)) } finally { setBusyId(null) }
  }
  async function toggleOnline() {
    if (!profile) return
    try { setProfile(await api<MerchantProfile>('/api/merchant/online', { method: 'PUT', body: { online: !profile.online } })) } catch (e) { setError(errMsg(e)) }
  }

  return (
    <Page bottomNav={<MerchantNav />}>
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="min-w-0"><span className="text-xs text-muted">Your shop</span><h1 className="truncate font-display text-[22px] font-bold">{profile?.shopName ?? '…'}</h1></div>
        <div className="flex items-center gap-2">
          {profile && (
            <button aria-pressed={profile.online} onClick={toggleOnline}
              className={`flex h-9 items-center gap-2 rounded-full border px-3 text-[13px] font-semibold ${profile.online ? 'border-mint bg-mint-soft text-mint' : 'border-line bg-surface2 text-muted'}`}>
              <span className={`h-2 w-2 rounded-full ${profile.online ? 'bg-mint' : 'bg-faint'}`} />{profile.online ? 'Online' : 'Offline'}
            </button>
          )}
          <Link to="/notifications" aria-label="Notifications" className="relative flex h-11 w-11 items-center justify-center rounded-xl border border-line bg-surface2">
            <Icon name="bell" />
            {unread > 0 && <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-mint px-1 text-[11px] font-bold text-mint-ink">{unread}</span>}
          </Link>
        </div>
      </div>

      {profile && !profile.online && <p className="mb-4 rounded-xl bg-surface p-3 text-sm text-muted">You're offline — customers can't book you until you go online.</p>}

      <div role="tablist" className="mb-4 grid grid-cols-3 gap-1 rounded-[14px] border border-line bg-surface p-1">
        {(['pending', 'upcoming', 'history'] as Scope[]).map((s) => (
          <button key={s} role="tab" aria-selected={scope === s} onClick={() => setScope(s)}
            className={`h-10 rounded-[10px] text-sm font-semibold capitalize ${scope === s ? 'bg-mint text-mint-ink' : 'text-muted'}`}>{s === 'pending' ? 'New requests' : s}</button>
        ))}
      </div>

      <ErrorBox msg={error} />
      {!list ? <Spinner /> : list.length === 0 ? (
        <Empty icon="calendar" title={scope === 'pending' ? 'No new requests' : scope === 'upcoming' ? 'No upcoming bookings' : 'No history yet'}
          text={scope === 'pending' ? "You'll get a push notification when a customer books." : undefined} />
      ) : (
        <div className="flex flex-col gap-3">
          {list.map((b) => (
            <div key={b.id} className="card flex flex-col gap-2.5 p-3.5">
              <Link to={`/merchant/orders/${b.id}`} className="flex flex-col gap-2.5">
                <div className="flex justify-between gap-2"><strong>{b.serviceName} · {vehicleLabel(b.vehicleType)}</strong><strong>{rupees(b.price)}</strong></div>
                <span className="flex items-center gap-2 text-sm"><Icon name="calendar" size={16} className="text-mint" />{slotLabel(b.slotStart)}</span>
                <span className="flex items-center justify-between text-[13px] text-muted">
                  <span>{b.customerName ?? 'Customer'} · {b.customerPincode}</span>
                  {scope !== 'pending' && <StatusPill status={b.status} />}
                </span>
                {b.status === 'REQUESTED' && <span className="flex items-center gap-2 text-[13px] text-muted"><Icon name="lock" size={14} />Address &amp; phone shown after you accept</span>}
              </Link>
              {b.status === 'REQUESTED' && (
                <div className="flex gap-2.5">
                  <button className="btn-danger h-11 flex-1 text-sm" disabled={busyId === b.id} onClick={() => act(b.id, 'decline')}>Decline</button>
                  <button className="btn-primary h-11 flex-1 text-sm" disabled={busyId === b.id} onClick={() => act(b.id, 'accept')}>Accept</button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </Page>
  )
}
