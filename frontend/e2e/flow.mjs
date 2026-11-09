// End-to-end browser test of the full booking flow (customer + merchant).
// Needs: backend on :8080 with AUTH_DEV_MODE=true, frontend built with VITE_DEV_AUTH=true served on :4173.
// Run: node e2e/flow.mjs [screenshotDir]
import { createRequire } from 'node:module'
const { chromium } = createRequire(import.meta.url)('playwright')

const WEB = process.env.WEB_URL ?? 'http://localhost:4173'
const SHOTS = process.argv[2]
const phone = { viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, geolocation: { latitude: 12.9352, longitude: 77.6245 }, permissions: ['geolocation'] }
let step = 0
const shot = async (page, name) => { if (SHOTS) await page.screenshot({ path: `${SHOTS}/${String(++step).padStart(2, '0')}-${name}.png`, fullPage: true }) }
const log = (m) => console.log('✓', m)

const browser = await chromium.launch()
const merchantCtx = await browser.newContext(phone)
const customerCtx = await browser.newContext(phone)
const m = await merchantCtx.newPage()
const c = await customerCtx.newPage()
for (const p of [m, c]) p.on('pageerror', (e) => { console.error('PAGE ERROR', e.message); process.exitCode = 1 })

try {
  // ---------- merchant signs up and onboards ----------
  await m.goto(WEB)
  await shot(m, 'login')
  await m.getByRole('tab', { name: "I'm a Merchant" }).click()
  await m.getByLabel('Mobile number').fill('9000012345')
  await m.getByLabel('Your name').fill('Ravi Kumar')
  await m.getByRole('button', { name: 'Dev login (no SMS)' }).click()
  await m.getByRole('radio', { name: /Merchant/ }).check()
  await m.getByRole('button', { name: 'Continue' }).click()
  await m.getByRole('heading', { name: 'Set up your shop' }).waitFor()
  await m.getByLabel('Shop name').fill('Speed Auto Care')
  await m.getByLabel('Shop address').fill('#42, 5th Cross, 80 Feet Rd, Koramangala 4th Block')
  await m.getByLabel('Pincode').fill('560034')
  await m.getByLabel('City').fill('Bengaluru')
  await m.getByRole('button', { name: /Use my current location/ }).click()
  await m.getByText('12.9352, 77.6245').waitFor()
  await m.getByRole('button', { name: 'Sun' }).click() // open on Sunday too
  await m.getByLabel('Opening time').fill('08:00')
  await m.getByLabel('Closing time').fill('22:00')
  await shot(m, 'merchant-onboarding')
  await m.getByRole('button', { name: 'Next: add services & prices' }).click()
  await m.getByRole('heading', { name: 'My services' }).waitFor()
  log('merchant onboarded with shop timings and location')

  for (const [cat, price, dur] of [['WATER_WASH', '149', '30'], ['OIL_CHANGE', '449', '20']]) {
    await m.getByRole('button', { name: 'Add service' }).first().click()
    await m.getByLabel('Category').selectOption(cat)
    await m.getByLabel('Price (₹)').fill(price)
    await m.getByLabel('Duration (min)').fill(dur)
    await m.getByRole('button', { name: 'Save', exact: true }).click()
    await m.getByText(`₹${price}`).first().waitFor()
  }
  await shot(m, 'merchant-services')
  log('merchant added 2 services')

  // ---------- customer finds the shop by pincode and books ----------
  await c.goto(WEB)
  await c.getByLabel('Mobile number').fill('9876543210')
  await c.getByLabel('Your name').fill('Rahul Sharma')
  await c.getByRole('button', { name: 'Dev login (no SMS)' }).click()
  await c.getByRole('button', { name: 'Continue' }).click()
  await c.getByLabel('Your pincode').fill('560034')
  await c.getByRole('button', { name: 'Show', exact: true }).click()
  await c.getByText('1 shop near 560034').waitFor()
  await shot(c, 'customer-home')
  log('customer sees shop for pincode 560034')

  await c.getByRole('link', { name: /Speed Auto Care/ }).click()
  await c.getByRole('radio', { name: /Water wash/ }).check()
  await shot(c, 'customer-shop')
  await c.getByRole('link', { name: 'Choose date & time' }).click()
  await c.getByRole('heading', { name: 'Schedule booking' }).waitFor()
  // choose tomorrow and the 4:30 PM slot
  await c.locator('button[aria-pressed]').nth(1).click()
  await c.getByRole('button', { name: '4:30', exact: true }).click()
  await c.getByLabel('Address').fill('Flat 302, Green Residency, 6th Main, Koramangala')
  await shot(c, 'customer-schedule')
  await c.getByRole('button', { name: /Request booking/ }).click()
  await c.getByRole('heading', { name: 'My bookings' }).waitFor()
  const otp = (await c.locator('strong.tracking-\\[6px\\]').first().innerText()).trim()
  if (!/^\d{4}$/.test(otp)) throw new Error('OTP not shown: ' + otp)
  await shot(c, 'customer-bookings-otp')
  log(`booking requested; customer sees start OTP ${otp}`)

  // ---------- merchant accepts and starts with OTP ----------
  await m.goto(WEB + '/merchant')
  await m.getByText('Water wash · Bike').waitFor()
  await m.getByText('Address & phone shown after you accept').waitFor()
  await shot(m, 'merchant-new-request')
  await m.getByRole('button', { name: 'Accept' }).first().click()
  await m.getByRole('tab', { name: 'upcoming' }).click()
  await m.getByRole('link', { name: /Water wash/ }).click()
  await m.getByText('Flat 302, Green Residency').waitFor()
  log('merchant accepted; customer address now visible')

  for (let i = 0; i < 4; i++) await m.getByLabel(`OTP digit ${i + 1}`).fill(String((Number(otp[i]) + 1) % 10))
  await m.getByRole('button', { name: 'Verify OTP & start' }).click()
  await m.getByText(/Wrong OTP\. 4 attempts left\./).waitFor()
  log('wrong OTP rejected')
  for (let i = 0; i < 4; i++) await m.getByLabel(`OTP digit ${i + 1}`).fill(otp[i])
  await shot(m, 'merchant-otp')
  await m.getByRole('button', { name: 'Verify OTP & start' }).click()
  await m.getByRole('button', { name: 'Mark as completed' }).click()
  await m.getByText('Completed').first().waitFor()
  await shot(m, 'merchant-completed')
  log('service started with correct OTP and completed')

  // ---------- customer rates ----------
  await c.reload()
  await c.getByRole('tab', { name: 'Past' }).click()
  await c.getByRole('button', { name: '5 stars' }).click()
  await c.getByLabel('Review').fill('Spotless and on time!')
  await c.getByRole('button', { name: 'Submit rating' }).click()
  await c.getByText('You rated').waitFor()
  await c.goto(WEB + '/notifications')
  await c.getByText('Booking accepted ✓').waitFor()
  await c.getByText('Service completed').waitFor()
  await shot(c, 'customer-notifications')
  log('customer rated 5★ and has accepted/started/completed notifications')

  // ---------- merchant sees review and replies ----------
  await m.goto(WEB + '/merchant/reviews')
  await m.getByText('Spotless and on time!').waitFor()
  await m.getByRole('button', { name: 'Reply' }).click()
  await m.getByLabel('Reply').fill('Thanks Rahul, see you again!')
  await m.getByRole('button', { name: 'Send reply' }).click()
  await m.getByText('Your reply:').waitFor()
  await shot(m, 'merchant-reviews')
  log('merchant sees 5.0 rating and replied')

  await c.goto(WEB)
  await c.getByText('5.0').first().waitFor()
  log('customer home shows updated rating 5.0')
  console.log('\nALL E2E CHECKS PASSED')
} catch (e) {
  console.error('E2E FAILED:', e.message)
  await m.screenshot({ path: '/tmp/e2e-merchant-fail.png', fullPage: true }).catch(() => {})
  await c.screenshot({ path: '/tmp/e2e-customer-fail.png', fullPage: true }).catch(() => {})
  process.exitCode = 1
} finally {
  await browser.close()
}
