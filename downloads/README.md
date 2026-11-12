# Download the software

Two packages, both ready to run. Each was tested by extracting it into an empty
directory and starting it — the same thing you are about to do.

| File | What it is | Needs |
|------|-----------|-------|
| [`femzyk-fleet-desktop-2.0.0.zip`](femzyk-fleet-desktop-2.0.0.zip) | Desktop edition — runs offline on one machine | Java 17+ |
| [`femzyk-fleet-web-3.0.0.zip`](femzyk-fleet-web-3.0.0.zip) | Web edition — multi-user, browser-based | Node.js 20+, PostgreSQL |

Click the file, then use the **Download** button. Both contain a `START-HERE.md`
that walks through setup from scratch.

---

## Desktop edition

```bash
unzip femzyk-fleet-desktop-2.0.0.zip
cd femzyk-fleet-desktop-2.0.0
./run-linux-macos.sh --demo
```

On Windows, double-click `windows\FEMZYK-Fleet.bat` instead.

Sign in with `admin` / `Admin@2024`, then change the password. Everything —
vehicles, drivers, trips, fuel, maintenance, reports — works with no network
connection, storing data in a single SQLite file you can copy as a backup.

Install Java first from <https://adoptium.net> if you do not already have it.
The launcher tells you if it is missing and what to do.

**On the `.exe`:** this release has no `.exe`, and that is deliberate. Turning
the desktop application into a double-clickable Windows executable that needs no
Java installed requires `jpackage`, which only runs on Windows. These packages
were assembled on Linux with a Java *runtime* only — no JDK, no Windows
toolchain — so an `.exe` built here could not have been run even once before
shipping, and a broken binary fails silently at double-click. What you get
instead is `windows\FEMZYK-Fleet.bat`, which is tested and works. A genuine
installer with a bundled runtime can be built by running the
`build-desktop.yml` workflow from the repository's Actions tab.

---

## Web edition

```bash
unzip femzyk-fleet-web-3.0.0.zip
cd femzyk-fleet-web-3.0.0
npm install
cp .env.example .env.local        # fill in your Supabase URL + service-role key
npm run dev
```

Before it starts you need a PostgreSQL database with the schema applied. Create
a free Supabase project, then run the two files in `supabase/migrations/` in
order in the SQL editor. `START-HERE.md` has the details, plus the list of things
to change before letting anyone else use it.

This is the same build that runs at <https://femzyk-fleet.vercel.app>.

---

## Which one do I want?

**One person, one computer, works offline** → desktop.

**Several people, different locations, shared data** → web.

They are two front ends over the same business rules and the same role model,
so screens and reports look the same; only the storage engine and the way you
reach it differ. The version numbers differ because they are released
separately — the desktop tier is 2.0.0, the web tier 3.0.0.
