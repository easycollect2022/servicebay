// Smoke test for the self-contained demo jar (seeded shops). Run: node e2e/demo-smoke.mjs
import { createRequire } from 'node:module'
const { chromium } = createRequire(import.meta.url)('playwright')
const WEB = process.env.WEB_URL ?? 'http://localhost:8080'
const opts = { viewport: { width: 390, height: 844 } }
const b = await chromium.launch()
const c = await (await b.newContext(opts)).newPage()
const m = await (await b.newContext(opts)).newPage()
try {
  await c.goto(WEB)
  await c.getByLabel('Mobile number').fill('9876543210')
  await c.getByLabel('Your name').fill('Pavan')
  await c.getByRole('button', { name: 'Dev login (no SMS)' }).click()
  await c.getByRole('button', { name: 'Continue' }).click()
  await c.getByLabel('Your pincode').fill('560034')
  await c.getByRole('button', { name: 'Show', exact: true }).click()
  await c.getByText('2 shops near 560034').waitFor()
  await c.getByRole('link', { name: /Speed Auto Care/ }).click()
  await c.getByRole('radio', { name: /Puncture repair/ }).check()
  await c.getByRole('link', { name: 'Choose date & time' }).click()
  await c.locator('button[aria-pressed]:not([disabled])').nth(1).click()
  await c.locator('.grid-cols-4 button:not([disabled])').first().click()
  await c.getByLabel('Address').fill('Flat 1, Koramangala')
  await c.getByRole('button', { name: /Request booking/ }).click()
  const otp = (await c.locator('strong.tracking-\\[6px\\]').first().innerText()).trim()
  console.log('✓ customer booked a seeded shop, OTP', otp)

  await m.goto(WEB)
  await m.getByRole('tab', { name: "I'm a Merchant" }).click()
  await m.getByLabel('Mobile number').fill('9000012345')
  await m.getByRole('button', { name: 'Dev login (no SMS)' }).click()
  await m.getByRole('button', { name: 'Accept' }).first().click()
  await m.getByRole('tab', { name: 'upcoming' }).click()
  await m.getByRole('link', { name: /Puncture repair/ }).click()
  for (let i = 0; i < 4; i++) await m.getByLabel(`OTP digit ${i + 1}`).fill(otp[i])
  await m.getByRole('button', { name: 'Verify OTP & start' }).click()
  await m.getByRole('button', { name: 'Mark as completed' }).waitFor()
  console.log('✓ seeded merchant accepted and started with OTP\nDEMO SMOKE PASSED')
} catch (e) { console.error('FAILED', e.message); process.exitCode = 1 } finally { await b.close() }
