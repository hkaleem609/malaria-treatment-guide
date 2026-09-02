# CLAUDE.md — National Malaria Treatment Protocol Portal

Reference notes for working on this repository.

---

## What this is

A single-page clinical decision support tool for the **National Malaria Treatment
Protocol**, branded for **Indus Hospital & Health Network**. A clinician selects four
patient parameters and the page renders the correct treatment regimen.

The audience is clinical staff at community and facility level, including on mobile
phones in the field. Treat it as an **official medical reference**, not a marketing page.

---

## Live URLs

| URL | Host | Notes |
|---|---|---|
| https://national-malaria-treatment-guide.vercel.app/ | Vercel | **Primary / canonical** |
| https://hkaleem609.github.io/malaria-treatment-guide/ | GitHub Pages | Secondary, same repo |

Both track `main` and auto-deploy. Vercel rebuilds in ~20–30s after a push.

## Repository

- **GitHub:** https://github.com/hkaleem609/malaria-treatment-guide
- **Owner:** `hkaleem609` (Kaleem Ul Hassan)
- **Local clone:** `D:\Automation\MalariaTreatmentGuide`
- **Branch:** `main` (only branch; deploy branch for both hosts)

---

## Deployment workflow

```
edit index.html → git commit → git push origin main → Vercel + Pages redeploy
```

There is **no build step**. The site is one static HTML file with inline CSS and JS —
no dependencies, no bundler, no package.json. Push and it is live.

### Git auth on this machine

Credentials are stored in Windows Credential Manager (`credential.helper manager`),
so `git push origin main` works without prompting. The GitHub MCP server is also
configured and authenticated, so file writes can go through MCP tools directly.

**PowerShell note:** each tool invocation is a fresh process that does not inherit an
updated PATH, so prefix git commands with:

```powershell
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
```

---

## File structure

```
MalariaTreatmentGuide/
├── index.html    ← the entire application (HTML + CSS + JS in one file)
└── CLAUDE.md     ← this file
```

Keep it a single file. It is deliberately dependency-free so it loads fast on poor
connections and could be used offline if needed.

---

## Brand system — Indus Hospital & Health Network

Defined as CSS custom properties in `:root`. Use the tokens, never raw hex values.

| Token | Value | Use |
|---|---|---|
| `--ih-red` | `#DC1F26` | Brand red — accents, alerts, the Rx card spine |
| `--ih-red-dk` | `#B0151B` | Gradient end, hover states |
| `--ih-blue` | `#1D4A9E` | Brand blue — primary UI, headings |
| `--ih-blue-dk` | `#13306B` | Headers, drug names, gradient start |
| `--ok` / `--warn` / `--danger` | | Semantic status colors |

**Colour semantics are load-bearing** — clinicians read them at a glance:
red = stop / contraindicated, amber = caution, blue = information, green = standard.
Do not repurpose them decoratively.

### Logo

The logo is an **inline SVG recreation** of the Indus Hospital mark: two red bars plus a
rotated ellipse combined in one path with `fill-rule="evenodd"`, which produces the
negative-space "H" of the original. It scales to any size with no image file.

If the official asset is ever added to the repo, swap `.lockup-mark` for
`<img src="logo.png" alt="Indus Hospital & Health Network" class="lockup-mark">`.
There is a comment in `index.html` marking the spot.

---

## Page architecture

```
Letterhead      logo lockup + "Clinical Decision Support" tag, red/blue rule
Title band      navy gradient, page title + program attribution
Step 1          Patient Assessment — 4 selects + context chip row
Step 2          Recommended Treatment — the Rx hero card (#protocol-output)
Toolbar         Print / Save as PDF, Reset Selection
Footer          clinical disclaimer + attribution
```

### The Rx card is the hero

Everything else is subordinate to it. The visual hierarchy exists specifically to
**reduce cognitive load** for a clinician reading under time pressure:

1. **Drug name** at ~29px is the largest text on the page — the single most important fact
2. **Strength / frequency tags** immediately beneath it
3. **3-day dosing timeline** — numbered day markers joined by a connector rail, large
   tabular-figure doses, and "Morning + Evening" notes that decode what `2 + 2 Tabs` means
4. **Total dispensed** strip
5. Primaquine protocol and clinical notes, demoted below so they do not compete

Preserve this ordering when editing. If something new is added, it goes below the
timeline unless it is more urgent than the drug name.

### Responsive behaviour

- Breakpoint at **640px** → single column; the dosing timeline rotates from a horizontal
  rail to a **vertical** one, and the page goes edge-to-edge
- Touch targets are **46px minimum** on selects
- Type scales with `clamp()`; nothing may overflow horizontally
- `@media print` styles exist so the protocol prints cleanly as a patient record

### Accessibility

`aria-live="polite"` on `#protocol-output` announces protocol changes; `:focus-visible`
rings on all interactive elements; `prefers-reduced-motion` disables animation. Keep these.

---

## Clinical logic — `runGuidelineEngine()`

Reads the four selects, then branches. **The clinical content is authoritative — do not
reword dosing text, drug names, or directives without an explicit instruction to do so.**
Presentation can change freely; medical substance cannot.

### Decision order

1. **Age `u6m` (< 6 months / < 5 kg)** — short-circuits everything. Renders the red
   emergency referral card. No dosing is shown for any species. Checked first, before
   species, deliberately.

2. **P. vivax**
   - CQ in stock → **Chloroquine**, 25 mg base/kg over 3 days (10 / 10 / 5 mg base/kg)
   - CQ stockout → **Artemether + Lumefantrine** as approved alternative, with a
     stockout policy warning banner

3. **P. falciparum**
   - 1st trimester pregnancy → **Oral Quinine** 10 mg/kg 8-hourly × 7 days + urgent
     DHQ referral. Renders as referral-only, no dosing timeline.
   - Otherwise → **Artemether + Lumefantrine**

4. **Mixed infection** → **Artemether + Lumefantrine** + 7-day Primaquine radical cure

### Primaquine

Contraindicated in pregnancy and lactation in **all** branches — `isPregLact` gates this.
Otherwise the dose differs by species: 0.5 mg/kg × 7 days for vivax and mixed
(radical cure), single 0.25 mg/kg on Day 1 for falciparum (gametocytocidal).

### `AL_DOSING`

Weight-band table for Artemether + Lumefantrine. Keys `g1`–`g4` match the age select
values. `d1`/`d2`/`d3` are per-day doses written as `morning + evening`; `total` is the
tablet count for the full course.

---

## Conventions

- **No emoji in the UI.** They were removed for professionalism. Use the inline SVGs in
  the `ICON` object; add new ones there in the same stroke style (24×24, `stroke-width:2`,
  round caps).
- No external requests — no CDN scripts, no web fonts. System font stack only.
- Helper functions `alertBox()`, `dayCard()`, `renderChips()` build markup; use them
  rather than hand-writing duplicate HTML strings.
- Line endings: the repo is LF, Windows checkout is CRLF. Git warns on commit; harmless.

---

## Gotchas

- Verify changes against the **live URL**, not just the local file — confirm the deploy
  actually landed before reporting success.
- `Ctrl+F5` to bypass browser cache when checking.
- Node is **not installed** on this machine, so there is no local linter or test runner.
  Validate by loading the page in a browser.
