import { useEffect, useState } from 'react'
import { api, errMsg } from '../../lib/api'
import { timeAgo, vehicleLabel } from '../../lib/format'
import type { ReviewSummary } from '../../lib/types'
import { Empty, ErrorBox, MerchantNav, Page, Spinner, Stars } from '../../components/ui'

export default function Reviews() {
  const [data, setData] = useState<ReviewSummary | null>(null)
  const [replying, setReplying] = useState<number | null>(null)
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)

  const load = () => api<ReviewSummary>('/api/merchant/reviews').then(setData).catch((e) => setError(errMsg(e)))
  useEffect(() => { load() }, [])

  async function reply(id: number) {
    if (!text.trim()) return
    try { await api(`/api/merchant/reviews/${id}/reply`, { body: { reply: text.trim() } }); setReplying(null); setText(''); load() } catch (e) { setError(errMsg(e)) }
  }

  return (
    <Page bottomNav={<MerchantNav />} className="flex flex-col gap-4">
      <h1 className="font-display text-2xl font-bold">Ratings &amp; reviews</h1>
      <ErrorBox msg={error} />
      {!data ? <Spinner /> : (
        <>
          <div className="card flex items-center gap-5 p-4">
            <div className="flex w-24 flex-col items-center gap-0.5">
              <strong className="font-display text-[40px]">{data.ratingCount ? Number(data.ratingAvg).toFixed(1) : '–'}</strong>
              <Stars value={Number(data.ratingAvg)} />
              <span className="text-xs text-muted">{data.ratingCount} review{data.ratingCount === 1 ? '' : 's'}</span>
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              {[5, 4, 3, 2, 1].map((n) => {
                const c = data.distribution[String(n)] ?? 0
                const pct = data.ratingCount ? (c / data.ratingCount) * 100 : 0
                return (
                  <div key={n} className="flex items-center gap-2 text-xs">
                    <span className="w-2">{n}</span>
                    <span className="relative h-1.5 flex-1 overflow-hidden rounded bg-line"><span className="absolute inset-y-0 left-0 rounded bg-star" style={{ width: `${pct}%` }} /></span>
                  </div>
                )
              })}
            </div>
          </div>
          {data.reviews.length === 0 ? <Empty icon="star" title="No reviews yet" text="Customers can rate you after a completed service." /> : data.reviews.map((r) => (
            <div key={r.id} className="card flex flex-col gap-1.5 p-3.5">
              <div className="flex justify-between text-sm"><strong>{r.customerName}</strong><Stars value={r.rating} /></div>
              <span className="text-xs text-muted">{r.serviceName} · {vehicleLabel(r.vehicleType)} · {timeAgo(r.createdAt)}</span>
              {r.comment && <p className="text-sm leading-relaxed">{r.comment}</p>}
              {r.reply ? <p className="mt-1 rounded-[10px] bg-surface2 px-3 py-2.5 text-[13px]"><strong>Your reply:</strong> <span className="text-muted">{r.reply}</span></p>
                : replying === r.id ? (
                  <div className="mt-1 flex flex-col gap-2">
                    <label className="sr-only" htmlFor={`rp${r.id}`}>Reply</label>
                    <textarea id={`rp${r.id}`} rows={2} maxLength={1000} value={text} onChange={(e) => setText(e.target.value)} className="resize-none rounded-xl border border-line bg-surface2 px-3 py-2.5 text-sm outline-none focus:border-mint" />
                    <div className="flex gap-2"><button className="btn-ghost h-10 flex-1 text-sm" onClick={() => setReplying(null)}>Cancel</button><button className="btn-primary h-10 flex-1 text-sm" onClick={() => reply(r.id)}>Send reply</button></div>
                  </div>
                ) : <button className="min-h-8 self-start text-[13px] font-semibold text-mint" onClick={() => { setReplying(r.id); setText('') }}>Reply</button>}
            </div>
          ))}
        </>
      )}
    </Page>
  )
}
