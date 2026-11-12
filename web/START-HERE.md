# FEMZYK Fleet Management — web edition (v3.0.0)

This folder is the **web edition**: a multi-user application you run on a server
and reach from any browser. It is the version deployed at
<https://femzyk-fleet.vercel.app>.

Running it locally is useful for testing and for development. There are two
paths below — pick **A** to try it quickly, **B** to run it properly against
your own database.

---

## A. Quickest local run (no database setup)

The web edition talks to a PostgreSQL database. Supabase's free tier is the
easiest one to point it at, and the project already ships the migrations.

1. **Install Node.js 20 or newer** — <https://nodejs.org> (LTS installer).

2. **Install the dependencies:**

   ```bash
   npm install
   ```

3. **Create a Supabase project** at <https://supabase.com> (free tier is fine).
   From *Project Settings → API*, copy:

   - the **Project URL** (looks like `https://abcdefgh.supabase.co`)
   - the **`service_role`** key — *not* the `anon` key

   There is a ready-made `.env.example` in this folder; copy it to `.env.local`
   and fill in the values rather than typing the block below from scratch.

4. **Create the database schema.** Open the Supabase **SQL Editor**, then paste
   and run, in this order:

   - `supabase/migrations/0001_schema.sql`
   - `supabase/migrations/0002_registration_requests.sql`

   (If your copy has a single combined migration, run that one instead. The
   order matters: `0002` adds tables that reference `0001`.)

5. **Create `.env.local`** in this folder with exactly these values:

   ```dotenv
   NEXT_PUBLIC_SUPABASE_URL=https://YOUR-PROJECT.supabase.co
   SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
   SESSION_SECRET=any-long-random-string-at-least-32-characters
   NEXT_PUBLIC_APP_VERSION=3.0.0

   # Show the demo account buttons on the sign-in page and seed demo accounts.
   # Set to false for anything real. Default is off.
   DEMO_MODE=false
   ```

   Generate a session secret with:

   ```bash
   node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
   ```

6. **Start it:**

   ```bash
   npm run dev
   ```

7. Open <http://localhost:3000>. The first visit creates the administrator
   account. Sign in as `admin` with `Admin@2024` and **change that password
   immediately** — it is published in this file and in the source code.

> **Never commit `.env.local`.** It contains a key that bypasses all database
> security. It is already listed in `.gitignore`; keep it that way.

---

## B. Running it as a real deployment

```bash
npm run build
npm start
```

`npm start` serves the production build on port 3000. Put it behind a reverse
proxy (nginx, Caddy) that terminates TLS, or deploy the same folder to Vercel —
set the project's **root directory** to `web` and add the same environment
variables in the Vercel dashboard.

Before you let anyone else use it:

- [ ] Set `DEMO_MODE=false` (or leave it unset — off is the default).
- [ ] Change the `admin` password, and delete the other demo accounts.
- [ ] Generate a fresh `SESSION_SECRET`; do not reuse the one from testing.
- [ ] Confirm the site is only reachable over HTTPS.

---

## Creating accounts

There is **no open sign-up**. This is deliberate: the system holds staff
records, driver licence numbers, salary-adjacent expense data and vehicle
documents, and it has no e-mail service wired up, so it cannot verify that
someone owns the address they type.

Instead, a person who needs access fills in **Request an account** on the
sign-in page. That creates a *request*, which appears for administrators under
**Users & roles → Account requests**. An administrator approves it, choosing the
role and a temporary password, and hands those to the person directly. The new
account must change its password at first sign-in.

Unapproved requests grant nothing — they cannot sign in, and they are not user
records until an administrator approves them.

To let people create their own accounts with no approval, a developer would
have to add an e-mail provider and switch `/api/register` from creating a
request to creating a user. That is a product decision, not a configuration
flag, and it is intentionally not switched on here.

---

## Where the database lives

This edition does not use a local file. Everything is in your Supabase project's
PostgreSQL database, and Supabase runs the backups (daily on the free tier).
Use *Database → Backups* in the Supabase dashboard.

To move the data somewhere else, use `pg_dump`:

```bash
pg_dump "postgresql://postgres:PASSWORD@db.YOUR-PROJECT.supabase.co:5432/postgres" \
  --no-owner --no-acl -f femzyk-backup.sql
```

---

## Checking a deployment is healthy

Two endpoints, both safe to call at any time:

```bash
curl http://localhost:3000/api/health
# {"ok":true,"database":"connected","seeded":true,...}

curl "http://localhost:3000/api/health/deep?secret=$BOOTSTRAP_SECRET"
# {"ok":true,"passed":31,"failed":0}
```

`/api/health/deep` runs 31 checks: that every table reads, that each of the four
roles gets exactly the permissions it should, that a wrong password is actually
rejected, that CSV and printable exports work, and that the audit log is
recording. It needs the secret in `BOOTSTRAP_SECRET`; without it the endpoint
refuses to run, so it cannot be used to probe your system.

Every check in it is **read-only** — it verifies credentials without writing
`LOGIN`/`LOGIN_FAILED` rows, so running it does not grow the audit log.

---

## Tests

```bash
npm run typecheck     # TypeScript, no emit
npm run build         # production build
npm test              # UI component tests (15)
```

---

## What is in this folder

```
src/app/                    pages and API routes (Next.js App Router)
src/lib/                    auth, sessions, permissions, services, domain logic
src/components/             shared UI components
supabase/migrations/        the database schema, in order
tests/                      component tests and the bundler they use
.env.example                template for your environment file
```

The full architecture — how the layers fit together, how sessions work, how
permissions are enforced — is in `documentation/ARCHITECTURE.md`.

---

## Common problems

**`TypeError: fetch failed` on every request**
`NEXT_PUBLIC_SUPABASE_URL` is wrong or unreachable. Check the URL, and check the
machine can reach `*.supabase.co`.

**`SUPABASE_SERVICE_ROLE_KEY` errors**
You copied the `anon` key. The server needs the `service_role` key. Keep it
server-side only — never put it in a `NEXT_PUBLIC_` variable.

**Everyone is logged out after a restart**
`SESSION_SECRET` changed, so existing session cookies no longer verify. Set it
once and keep it.

**"relation does not exist"**
The migrations were not run, or `0002` was run before `0001`. Re-run them in
order.

**Sign-in page shows demo account buttons in production**
`DEMO_MODE` is set to `true`. Unset it or set it to `false`, then redeploy —
environment changes need a new deployment to take effect.
