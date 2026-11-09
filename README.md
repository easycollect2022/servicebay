# ServiceBay — Bike & Car Service PWA

Customers find bike/car service shops by pincode, see services and prices, pick a date and time slot, and book.
Merchants onboard their shop (address, pincode, location, open days and timings, photos), set services and prices,
get a push notification for each request, accept or decline, and start the service only with the customer's 4-digit OTP.

| Part | Tech |
|---|---|
| Frontend (PWA) | React 18, TypeScript, Vite, Tailwind CSS, React Router, vite-plugin-pwa (Workbox) |
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway |
| Database | PostgreSQL 16 |
| Login | Firebase Authentication (mobile OTP + Google), ID tokens verified by the backend |
| Push | Firebase Cloud Messaging (web push), sent by the backend |
| Images | Local disk (dev) or Firebase Storage (prod), uploaded through the backend |

## Booking lifecycle

```
REQUESTED ──accept──▶ ACCEPTED ──OTP──▶ IN_PROGRESS ──▶ COMPLETED ──▶ customer rates
    │                     │
 decline/cancel        cancel
```

- **Slots:** generated from the shop's open days, opening/closing time and slot length. A slot is offered only if the whole
  service fits before closing and it starts at least 30 min from now. Bookings up to 7 days ahead.
- **No double booking:** a partial unique index on `(merchant_id, slot_start)` for active bookings, so two customers can never get the same slot even at the same instant.
- **Privacy:** the merchant sees only the customer's name and pincode until they accept; phone and address unlock after accepting.
- **Start OTP:** generated when the customer books, shown only to the customer, verified by the backend when the merchant starts. 5 wrong attempts lock it.
- **Push + in-app notifications:** new request → merchant; accepted / declined / started / completed / review reply → customer; cancelled / new review → merchant; reminder ~1 hour before the slot → customer.

## Run locally

Prerequisites: Java 21, Maven, Node 20+, Docker (or a local PostgreSQL 16).

```bash
docker compose up -d                      # PostgreSQL on :5432

cd backend
AUTH_DEV_MODE=true mvn spring-boot:run    # API on :8080, runs DB migrations

cd ../frontend
cp .env.example .env.local                # set VITE_DEV_AUTH=true to try without Firebase
npm install
npm run dev                               # app on http://localhost:5173
```

With `AUTH_DEV_MODE=true` (backend) and `VITE_DEV_AUTH=true` (frontend), the login screen shows **Dev login (no SMS)** so you can
try both roles without Firebase. Use two browser profiles: one merchant, one customer. **Never enable dev mode in production.**

## One-file demo (only Java 21 needed)

Embedded PostgreSQL + the PWA inside one jar, dev login on, two shops pre-loaded in pincode 560034
(merchant logins 9000012345 and 9000054321).

```bash
cd frontend && VITE_API_URL= VITE_DEV_AUTH=true npm run build
cd ../backend && mvn -Pdemo package -DskipTests
java -Dspring.profiles.active=demo -jar target/servicebay-demo.jar     # open http://localhost:8080
```
Data lives in `~/.servicebay-demo`. Smoke test: `node frontend/e2e/demo-smoke.mjs`.

## Firebase setup (login + push + images)

1. Create a Firebase project. Add a **Web app** and copy its config into `frontend/.env.local` (`VITE_FIREBASE_*`).
2. **Authentication › Sign-in method:** enable **Phone** and **Google**. Add your domain under *Authorized domains*.
3. **Cloud Messaging › Web Push certificates:** generate a key pair, put the public key in `VITE_FIREBASE_VAPID_KEY`.
4. **Project settings › Service accounts:** generate a private key JSON for the backend (keep it out of git).
5. Backend environment:

| Variable | Example |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | `jdbc:postgresql://host:5432/servicebay` |
| `FIREBASE_CREDENTIALS` | `/secrets/firebase-service-account.json` |
| `FIREBASE_PROJECT_ID` | `servicebay-prod` |
| `STORAGE_TYPE` | `firebase` (or `local`) |
| `FIREBASE_STORAGE_BUCKET` | `servicebay-prod.appspot.com` |
| `PUBLIC_BASE_URL` | `https://api.example.com` (used for local-storage image URLs) |
| `CORS_ORIGINS` | `https://app.example.com` |
| `AUTH_DEV_MODE` | `false` |

**iPhone:** web push works only after the user adds the app to the Home Screen (iOS 16.4+). The Profile / Shop screen explains this.

## Tests

```bash
cd backend && mvn test          # 10 unit tests (slots, OTP) + 4 API tests on real PostgreSQL (servicebay_test DB)
cd frontend && npm test         # date strip, time and validation helpers
cd frontend && node e2e/flow.mjs  # full browser flow; needs Playwright + both apps running in dev-auth mode
```

The API tests cover: onboarding and timing validation, image upload checks, pincode search, slot listing, booking,
double-booking conflict, contact privacy before/after accept, role enforcement, wrong/correct OTP, OTP lockout,
cancellation freeing the slot, reviews and rating average, merchant reply, and reminders sent exactly once.

## API overview

| Method & path | Who | Purpose |
|---|---|---|
| `GET /api/me`, `POST /api/me/role`, `PATCH /api/me` | any | profile and one-time role choice |
| `PUT /api/me/device-token` | any | register FCM token |
| `GET /api/me/notifications`, `POST …/read-all` | any | in-app notifications |
| `GET /api/merchants?pincode=&vehicle=&category=&lat=&lng=` | any | shops in a pincode |
| `GET /api/merchants/{id}`, `/{id}/slots?date=&serviceId=`, `/{id}/reviews` | any | shop details, free slots, reviews |
| `POST /api/bookings`, `GET /api/bookings`, `POST /api/bookings/{id}/cancel`, `POST /api/bookings/{id}/review` | customer | book, list (with OTP), cancel, rate |
| `GET/PUT /api/merchant/profile`, `PUT /api/merchant/online`, `POST/DELETE /api/merchant/photos` | merchant | shop details, timings, photos |
| `GET/POST/PUT/DELETE /api/merchant/services`, `POST /api/merchant/services/{id}/image` | merchant | services and prices |
| `GET /api/merchant/bookings?scope=pending\|upcoming\|history`, `POST …/{id}/accept\|decline\|start\|complete` | merchant | orders |
| `GET /api/merchant/reviews`, `POST /api/merchant/reviews/{id}/reply` | merchant | ratings and replies |

## Deploy free on Render (testing)

1. Push this repo to GitHub.
2. Render dashboard › **New › Blueprint** › pick the repo. `render.yaml` creates:
   - `servicebay` — one Docker web service serving the PWA and the API (free plan, Singapore)
   - `servicebay-db` — PostgreSQL (free plan; **expires after 30 days**)
3. When asked for the Firebase values, leave them empty for now. Open the `servicebay` URL (`https://servicebay-xxxx.onrender.com`).

Free plan notes: the service sleeps after 15 min idle (first request takes ~1 min); photos are stored in PostgreSQL
(`STORAGE_TYPE=db`) because the free disk is wiped on restart. Dev login is ON (`AUTH_DEV_MODE`, `VITE_DEV_AUTH`):
anyone with the link can sign in as any number, so share it only with testers and switch to Firebase OTP before real use.

## Deploying

- **Backend:** `backend/Dockerfile` → any container host (AWS ECS/App Runner, Render, Railway, a VM). Managed PostgreSQL (RDS, Cloud SQL, Neon).
- **Frontend:** `npm run build` → static `dist/` on Firebase Hosting, Netlify, Vercel or S3 + CloudFront. Must be served over HTTPS for the PWA and push. Route all paths to `index.html`.
