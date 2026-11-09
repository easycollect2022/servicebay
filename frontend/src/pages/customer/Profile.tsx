import { useEffect, useState } from 'react'
import { useAuth } from '../../lib/auth'
import { enablePush, needsInstallForPush, pushState, type PushState } from '../../lib/push'
import { CustomerNav, Header, Page } from '../../components/ui'
import { Icon } from '../../components/Icon'

export default function Profile() {
  const { me, logout } = useAuth()
  return (
    <Page bottomNav={<CustomerNav />}>
      <Header title="Profile" />
      <div className="card mb-4 flex flex-col gap-1 p-4">
        <strong>{me?.name ?? 'Customer'}</strong>
        <span className="text-sm text-muted">{me?.phone ?? me?.email}</span>
        {me?.address && <span className="text-sm text-muted">{me.address} – {me.pincode}</span>}
      </div>
      <PushCard />
      <button className="btn-ghost mt-4 w-full" onClick={logout}><Icon name="logout" size={18} />Log out</button>
    </Page>
  )
}

export function PushCard() {
  const [state, setState] = useState<PushState | null>(null)
  useEffect(() => { pushState().then(setState) }, [])
  if (!state) return null
  return (
    <div className="card flex flex-col gap-2 p-4">
      <strong className="flex items-center gap-2"><Icon name="bell" size={18} className="text-mint" />Push notifications</strong>
      {state === 'granted' && <p className="text-sm text-mint">On — you'll get booking updates on this device.</p>}
      {state === 'denied' && <p className="text-sm text-muted">Blocked. Allow notifications for this site in your browser settings.</p>}
      {state === 'unsupported' && <p className="text-sm text-muted">{needsInstallForPush() ? 'On iPhone, tap Share › Add to Home Screen, then open the app from your home screen to turn on notifications.' : 'Not supported in this browser.'}</p>}
      {state === 'default' && (
        <button className="btn-primary h-11 text-sm" onClick={async () => setState(await enablePush(true))}>Turn on notifications</button>
      )}
    </div>
  )
}
