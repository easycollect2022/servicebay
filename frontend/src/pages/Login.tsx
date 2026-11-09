import { useEffect, useRef, useState } from 'react'
import { GoogleAuthProvider, RecaptchaVerifier, signInWithPhoneNumber, signInWithPopup, type ConfirmationResult } from 'firebase/auth'
import { auth, devAuthEnabled, firebaseEnabled } from '../lib/firebase'
import { useAuth } from '../lib/auth'
import { ErrorBox } from '../components/ui'
import { Icon } from '../components/Icon'
import { isValidMobile } from '../lib/format'
import type { Role } from '../lib/types'

export const ROLE_HINT_KEY = 'sb.roleHint'

export default function Login() {
  const { devLogin } = useAuth()
  const [role, setRole] = useState<Role>('CUSTOMER')
  const [mobile, setMobile] = useState('')
  const [otp, setOtp] = useState('')
  const [name, setName] = useState('')
  const [confirm, setConfirm] = useState<ConfirmationResult | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resendIn, setResendIn] = useState(0)
  const verifier = useRef<RecaptchaVerifier | null>(null)

  useEffect(() => { try { sessionStorage.setItem(ROLE_HINT_KEY, role) } catch { /* ignore */ } }, [role])
  useEffect(() => {
    if (resendIn <= 0) return
    const t = setTimeout(() => setResendIn((s) => s - 1), 1000)
    return () => clearTimeout(t)
  }, [resendIn])

  async function sendOtp() {
    setError(null)
    if (!isValidMobile(mobile)) return setError('Enter a valid 10-digit mobile number')
    if (!auth) return
    setBusy(true)
    try {
      verifier.current ??= new RecaptchaVerifier(auth, 'recaptcha', { size: 'invisible' })
      setConfirm(await signInWithPhoneNumber(auth, '+91' + mobile.replace(/\s/g, ''), verifier.current))
      setResendIn(30)
    } catch (e) {
      setError(friendly(e))
      verifier.current?.clear()
      verifier.current = null
    } finally { setBusy(false) }
  }

  async function verifyOtp() {
    if (!confirm) return
    setError(null)
    if (!/^\d{6}$/.test(otp)) return setError('Enter the 6-digit OTP')
    setBusy(true)
    try { await confirm.confirm(otp) } catch (e) { setError(friendly(e)) } finally { setBusy(false) }
  }

  async function google() {
    if (!auth) return
    setError(null)
    try { await signInWithPopup(auth, new GoogleAuthProvider()) } catch (e) { setError(friendly(e)) }
  }

  async function dev() {
    if (!isValidMobile(mobile)) return setError('Enter a valid 10-digit mobile number')
    setBusy(true)
    try { await devLogin(mobile, name || (role === 'MERCHANT' ? 'Shop owner' : 'Customer')) } finally { setBusy(false) }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-md flex-col gap-7 px-6 pb-8 pt-14">
      <div className="flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-mint text-mint-ink"><Icon name="wrench" size={24} /></span>
        <span className="font-display text-[22px] font-bold">ServiceBay</span>
      </div>
      <div>
        <h1 className="mb-2.5 font-display text-[32px] font-bold leading-tight">Wash, fix &amp; service your ride — near you.</h1>
        <p className="text-muted">Book trusted bike &amp; car service shops in your pincode.</p>
      </div>

      <div role="tablist" className="grid grid-cols-2 gap-1 rounded-[14px] border border-line bg-surface p-1">
        {(['CUSTOMER', 'MERCHANT'] as Role[]).map((r) => (
          <button key={r} role="tab" aria-selected={role === r} onClick={() => setRole(r)}
            className={`h-11 rounded-[10px] text-[15px] font-semibold ${role === r ? 'bg-mint text-mint-ink' : 'text-muted'}`}>
            {r === 'CUSTOMER' ? "I'm a Customer" : "I'm a Merchant"}
          </button>
        ))}
      </div>

      <div className="flex flex-col gap-4">
        <div>
          <label className="label" htmlFor="mobile">Mobile number</label>
          <div className="flex gap-2">
            <span className="flex h-[52px] items-center rounded-[14px] border border-line bg-surface2 px-3.5 font-semibold">+91</span>
            <input id="mobile" className="input" inputMode="numeric" autoComplete="tel-national" maxLength={10}
              value={mobile} disabled={!!confirm} onChange={(e) => setMobile(e.target.value.replace(/\D/g, ''))} placeholder="98765 43210" />
          </div>
        </div>

        {firebaseEnabled && confirm && (
          <div>
            <label className="label" htmlFor="otp">Enter 6-digit OTP <span className="text-mint">· sent</span></label>
            <input id="otp" className="input tracking-[.5em] text-center text-xl" inputMode="numeric" autoComplete="one-time-code"
              maxLength={6} value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} />
            <p className="mt-2.5 text-[13px] text-muted">
              {resendIn > 0 ? `Resend OTP in 00:${String(resendIn).padStart(2, '0')}` :
                <button className="font-semibold text-mint" onClick={() => { setConfirm(null); setOtp('') }}>Change number / resend</button>}
            </p>
          </div>
        )}

        <ErrorBox msg={error} />

        {firebaseEnabled && (confirm
          ? <button className="btn-primary w-full" disabled={busy} onClick={verifyOtp}>Verify &amp; continue</button>
          : <button className="btn-primary w-full" disabled={busy} onClick={sendOtp}>Send OTP</button>)}
        <div id="recaptcha" />

        {!firebaseEnabled && devAuthEnabled && (
          <>
            <div>
              <label className="label" htmlFor="name">Your name</label>
              <input id="name" className="input" value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <button className="btn-primary w-full" disabled={busy} onClick={dev}>Dev login (no SMS)</button>
          </>
        )}
        {!firebaseEnabled && !devAuthEnabled && (
          <ErrorBox msg="Firebase is not configured. Add the VITE_FIREBASE_* values to .env (see .env.example)." />
        )}
      </div>

      {firebaseEnabled && (
        <>
          <div className="flex items-center gap-3 text-[13px] text-muted"><span className="h-px flex-1 bg-line" />or<span className="h-px flex-1 bg-line" /></div>
          <button className="btn-ghost w-full" onClick={google}>Continue with Google</button>
        </>
      )}

      <p className="mt-auto flex items-center justify-center gap-2 text-[13px] text-muted">
        <Icon name="lock" size={16} />Secured with Firebase Authentication
      </p>
    </div>
  )
}

function friendly(e: unknown): string {
  const code = (e as { code?: string })?.code ?? ''
  if (code.includes('invalid-verification-code')) return 'Wrong OTP. Please check and try again.'
  if (code.includes('code-expired')) return 'OTP expired. Request a new one.'
  if (code.includes('too-many-requests')) return 'Too many attempts. Please try again later.'
  if (code.includes('invalid-phone-number')) return 'Enter a valid mobile number'
  if (code.includes('popup-closed')) return 'Sign-in was cancelled'
  return 'Could not sign in. Please try again.'
}
