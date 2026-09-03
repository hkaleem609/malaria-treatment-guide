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
Title band      title, one-line lede, 3-step orientation strip
Step 1          Patient Details — compact 5-control panel (#assess)
Step 2          Recommended protocol — the Rx card (#protocol-output)
Toolbar         Print / Save as PDF, Reset
Footer          clinical disclaimer + attribution
```

### The assessment panel is deliberately small

It is a thin bar, not a form. Controls are 42px, labels are 10.5px, option text is
abbreviated (`P. vivax`, not `Plasmodium vivax (P. vivax)`) so the panel stays one or two
rows on desktop. A first-visit cue (`.assess.cue`, a twice-repeating box-shadow pulse
removed on first interaction) plus the "Select below — the protocol updates instantly"
hint tell a new user where to start. **Do not let this panel grow** — every row added
here is a row stolen from the treatment.

### The Rx card is the hero

Everything else is subordinate to it. The hierarchy exists to **reduce cognitive load**
for a clinician reading under time pressure. The card holds two numbered regimen blocks
rendered in the *same* visual language, so neither reads as an afterthought:

```
Rx header      context chips (species · band · weight · group · stockout)
① Blood-stage treatment    drug name → tags → day timeline → total
② Primaquine therapy       drug name → tags → dose block → referral note
Clinical notes
```

Within each block: **drug name** (~27px, the largest text on the page) → strength and
frequency tags → dosing schedule → totals. New content goes *below* the schedule unless
it outranks the drug name in urgency.

Two schedule renderers exist — use the right one:
- `dayCard()` / `.course` — the connected 3-day timeline, for day-by-day varying doses
- `doseBlock()` — one wide row, for a single dose or an identical dose repeated N days
  (rendering seven identical Primaquine cards would be noise, not information)

### Responsive behaviour

Breakpoints: **900px** (two-column grids collapse), **640px** (phone), **380px** (small
phone). Type scales with `clamp()`; nothing may overflow horizontally.

Non-obvious rules that exist for a reason — do not "tidy" these away:

- **`.ctl` is `font-size:16px` on phones.** Below 16px, iOS Safari zooms the page when a
  form control is focused and does not zoom back out. The desktop size is 13.5px; the
  mobile bump is deliberate, not an inconsistency.
- **Touch targets are 48px** on phones (controls, buttons, `<details>` summaries).
- **Safe-area insets** (`env(safe-area-inset-*)`) on every full-bleed section, and on the
  footer's bottom padding, so content clears notches and the home indicator.
- **The dosing timeline becomes compact rows below 640px** — flex rows with the dose
  right-aligned, connector rail dropped, rather than three tall centred cards. Print
  restores the centred cards.

### The sticky case bar

`.stickybar` (mobile only) keeps the case summary visible through a long protocol and puts
the form one tap away via `jumpToDetails()`. `.assess` carries `scroll-margin-top` so the
bar does not cover the panel it scrolls to.

`.rx-context` inside the Rx header is **hidden below 640px** to avoid duplicating it — and
**forced back on in print** (`display:flex !important`), because the printed record must
always carry the case summary. Both render from the same `ctxParts` array, which is built
*before* the under-6-month early return so the bar stays correct on that screen too.

`@media print` styles exist so the protocol prints cleanly as a patient record.

### Accessibility

`aria-live="polite"` on `#protocol-output` announces protocol changes; `:focus-visible`
rings on all interactive elements; `prefers-reduced-motion` disables animation. Keep these.

---

## Clinical logic — `runGuidelineEngine()`

Reads the four selects, then branches. **The clinical content is authoritative — do not
reword dosing text, drug names, or directives without an explicit instruction to do so.**
Presentation can change freely; medical substance cannot.

**Source of truth:** the national weight-based dosing protocol sheet
(`3U_Malaria_Treatment_Protocol_EN.pdf`, an English translation of the Urdu original).
Both dosing tables in the code are transcribed from it. Check any dosing change against
that sheet before making it.

### Decision order

1. **Age `u6m` (< 6 months / < 5 kg)** — short-circuits everything. Renders the red
   emergency referral card. No dosing for any species. Checked first, before species,
   deliberately.

2. **P. vivax**
   - CQ available → **Chloroquine**, 25 mg/kg over 3 days (10 / 10 / 5 mg/kg)
   - CQ stockout → **Artemether + Lumefantrine**, with the protocol's footnote shown as
     a warning: second-line, but usable as first-line for uncomplicated vivax *provided
     the case is confirmed by microscopy or RDT*

3. **P. falciparum** → **Artemether + Lumefantrine**

4. **Mixed infection** → **Artemether + Lumefantrine**

### Pregnancy

The patient group select has **three** options — `general`, `preg`, `lact`. Trimester is
deliberately *not* asked (the user asked for a single "Pregnant" option).

Because trimester is unknown, **any branch using AL for a pregnant patient shows a red
alert** telling staff to confirm gestational age and referring to oral Quinine
(10 mg/kg 8-hourly × 7 days) for the first trimester. This alert is the only thing
carrying that safety information — **do not remove it** without restoring a trimester
input. Chloroquine branches do not need it.

### Primaquine — regimen block ②

Rendered as a full first-line regimen, not a footnote. Dose by species:

| Species | Rate | Schedule | Purpose |
|---|---|---|---|
| P. falciparum | 0.25 mg/kg | Single dose, Day 1 | Gametocytocidal — blocks transmission |
| P. vivax / Mixed | 0.5 mg/kg | Once daily × 7 days, with food | Radical cure — clears liver hypnozoites |

**Contraindicated in pregnancy and lactation** — `isPregLact` swaps the block to a struck
-through "Contraindicated" state. The block still renders rather than disappearing, so the
clinician can see Primaquine was considered and deferred.

### Dispensing pathways — the load-bearing distinction

**Who may dispense Primaquine depends on the care setting**, and this is the single most
safety-critical thing on the page:

| Level | Who | Action |
|---|---|---|
| **Facility** — BHU / RHC | Medical Officers, LHVs, Medical Technicians | **Dispense on site.** Stock held; staff screen contraindications and counsel on haemolysis |
| **Community** — iCCM / frontline CHW | Community health workers | **Refer — do not dispense.** Blood-stage treatment only, then referral slip + home follow-up |

CHWs are barred from dispensing because they lack laboratory support and emergency
response for **G6PD-induced haemolytic crises**. That rationale is printed in the
community lane on purpose — staff who understand *why* comply more reliably than staff
told only *what*.

### The role system — `ROLE`

Care setting is a property of **the user, not the patient**, so it is not in the Patient
Details panel. It is asked once and remembered.

- **First visit** → `initRole()` finds no stored value and shows the role gate
  (`#role-gate`), hiding `#app-main`. Two large targets plus a "show both — decide later"
  escape.
- **Stored** in `localStorage` under `nmtp-care-setting`. Every read and write is wrapped
  in try/catch — private browsing throws, and the page must still work (it falls back to
  asking again, which is harmless).
- **`ROLE`** is one of `facility` | `community` | `both`. `both` is a legitimate state,
  not an error — it is what the skip link produces.
- **Header role chip** (`#role-chip`) shows the active setting and reopens the gate in one
  tap. It replaces the "Clinical Decision Support" tag when a role is set.

**Why the chip must stay visible:** shared devices. A tablet at a BHU is used by several
staff, and a wrong remembered role that is invisible would give confidently wrong guidance
with no cue. The chip makes the current state obvious and correctable.

**Why the other lane is collapsed, never deleted:** a CHW writing a referral slip needs to
know what happens at the facility they are referring to, and a wrong role must stay
recoverable. `pathways()` renders the user's lane via `laneFull()` and the other via
`laneCollapsed()` (a native `<details>`). With `ROLE === "both"` both render side by side,
stacking below 760px.

### Community level inverts the Primaquine block

A CHW never hands Primaquine over, so rendering it as a large first-line drug hero
invited the wrong action. At `ROLE === "community"` regimen ② is restructured:

1. **The refer card comes first** — `laneFull(LANE.community, true)`, with a red
   `Refer — do not dispense` badge in the section head instead of the green
   `First-line regimen` badge
2. **The dose drops to `.info-only`** — a small red strip labelled *"For information
   only — dispensed at the facility, not by you"*, framed as what the facility will give
   so the CHW can brief the patient. **Do not restore the drug hero here** — its size is
   the whole point.
3. Facility lane collapsed below, then the G6PD warning

The G6PD text is also role-aware: at community level it is framed as a **home follow-up
duty**, at facility level as dispensing-time counselling.

Facility and `both` views keep the full dosing hero — those users do dispense.

### `actionPlan()`

A numbered "what you do" list, ordered by what happens first. Facility steps lead with
diagnosis, dispensing, contraindication screening and haemolysis counselling; community
steps lead with RDT confirmation, blood-stage dispensing, the referral slip and home
follow-up. Steps that are prohibitions get `.stop` (red, renders `!` instead of a number).

With `ROLE === "both"` it shows a prompt to choose a setting rather than a generic list —
a merged list would be wrong for both audiences.

**`[hidden]{display:none !important}`** is declared near the reset. Author `display`
rules (`.rolechip`, `.tag` are `inline-flex`) outrank the UA `[hidden]` rule, so without
it the gate toggling silently fails.

The exact national-protocol sentence is quoted in the community lane
(`.protocol-quote`) — keep it verbatim, it is the authority for the referral requirement.

A **G6PD haemolysis warning** (dark or tea-coloured urine → stop and return) sits below
the lanes because it applies at both levels. Contraindicated cases skip the lanes and
instead explain that facility screening is what catches pregnancy, lactation and age
under 6 months, then use `referralNote()` for the deferred course.

### Dosing tables

- **`AL_DOSING`** — Artemether + Lumefantrine tablets per dose, twice daily × 3 days.
  Keys `g1`–`g4` match the age select. `d` is a 3-element array of `morning + evening`
  strings. Underlying rate: Artemether 1.7 mg/kg, Lumefantrine 12 mg/kg.
- **`CQ_TABLE`** — Chloroquine tablets per day by body weight, from 11–15 kg through
  > 50 kg. Looked up with `cqRow(w)`, which returns the first row where `w <= r.max`, or
  `null` below 11 kg (the table's floor — falls back to showing the mg/kg rule).

### Body weight

Optional. When present it drives exact CQ tablet counts and exact Primaquine mg; when
absent the page falls back to mg/kg rules and prompts for it. `checkWeight()` compares the
entered weight against `bandFromWeight()` and shows an amber caution if it disagrees with
the selected age band — tablet dosing follows weight, so a mismatch is a real error worth
surfacing rather than silently resolving.

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
