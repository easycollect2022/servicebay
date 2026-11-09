import { useState } from 'react'
import { useAuth } from '../lib/auth'
import { ErrorBox } from '../components/ui'
import { errMsg } from '../lib/api'
import type { Role } from '../lib/types'
import { ROLE_HINT_KEY } from './Login'

export default function RoleSelect() {
  const { me, chooseRole, logout } = useAuth()
  const hint = (() => { try { return sessionStorage.getItem(ROLE_HINT_KEY) as Role | null } catch { return null } })()
  const [role, setRole] = useState<Role>(hint ?? 'CUSTOMER')
  const [name, setName] = useState(me?.name ?? '')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function save() {
    if (!name.trim()) return setError('Enter your name')
    setBusy(true); setError(null)
    try { await chooseRole(role, name.trim()) } catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col gap-6 px-6 pt-14">
      <h1 className="font-display text-[28px] font-bold">Welcome! Let's set up your account</h1>
      <div>
        <label className="label" htmlFor="nm">Your name</label>
        <input id="nm" className="input" value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" />
      </div>
      <fieldset className="flex flex-col gap-3">
        <legend className="label">I want to</legend>
        {([['CUSTOMER', 'Book services for my bike or car'], ['MERCHANT', 'List my service shop and receive bookings']] as [Role, string][]).map(([r, text]) => (
          <label key={r} className={`card flex cursor-pointer items-center gap-3 p-4 ${role === r ? 'border-mint' : ''}`}>
            <input type="radio" name="role" className="h-5 w-5 accent-[#3DDC97]" checked={role === r} onChange={() => setRole(r)} />
            <span><strong className="block">{r === 'CUSTOMER' ? 'Customer' : 'Merchant'}</strong><span className="text-sm text-muted">{text}</span></span>
          </label>
        ))}
      </fieldset>
      <p className="text-sm text-muted">You can't switch between customer and merchant later with the same number.</p>
      <ErrorBox msg={error} />
      <button className="btn-primary w-full" disabled={busy} onClick={save}>Continue</button>
      <button className="text-sm font-semibold text-muted" onClick={logout}>Use a different account</button>
    </div>
  )
}
