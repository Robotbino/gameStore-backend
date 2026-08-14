# `docs/_src/` — the documentation build source

The six Game Store documents are **standalone HTML**: open any of them straight
off disk, no server, no network, no build step to read them. That portability
is a hard requirement — and it is exactly what used to make them rot, because
the only way to satisfy it by hand was to paste the same chrome into six files
that then drifted apart.

This folder resolves that. The shared CSS and JS live here **once**, and are
stamped into each document between markers. The documents stay literally
self-contained; the source of truth stays singular. You edit here, then run one
command.

> **Canonical location.** `_src/` lives in the **`gameStore-backend`** repo only.
> The scripts write into both repos, which are assumed to sit as siblings under
> one parent directory (`…/Projects/gameStore-backend` and `…/Projects/gameStore`).
> That sibling assumption is made **only** at build time, by scripts you run by
> hand — never by the documents themselves.

---

## The one command

```bash
node docs/_src/stamp.mjs
```

Rewrites every marked region in all eight targets (six documents + two front
doors). Safe to re-run — it is idempotent, and `--check` makes it a CI gate:

```bash
node docs/_src/stamp.mjs --check     # exit 1 if any doc differs from _src
node docs/_src/stamp.mjs be-arch     # just one, by id
```

## The other three commands

```bash
node docs/_src/vendor.mjs            # fetch Mermaid + fonts into docs/vendor/ (both repos)
node docs/_src/build-index.mjs       # regenerate the ⌘K search index from the docs
node docs/_src/build-index-page.mjs  # regenerate the front-door index.html per repo
```

`vendor.mjs` needs a network (it downloads); the other three are offline and
read only what is already on disk. After `build-index*.mjs`, run `stamp.mjs` to
push the result into the documents.

---

## What lives where

| File | Role |
|---|---|
| `docs.manifest.mjs` | The one description of the doc set: the six documents, their repos, roles, and which component stylesheets each gets. Every other script reads this. |
| `core.css` | Tokens + base + chrome + furniture + Mermaid motion + search. Every document gets this. |
| `components/tables.css` | Tables, pills, accordions, compare panels, code blocks. |
| `components/roadmap.css` | Ranked-work cards, pairings, progress bar. |
| `components/diagram.css` | Diagram shells, generated key, toolbar, layer explorer, flow tabs. |
| `components/learning.css` | Quizzes, inline flow SVG. |
| `chrome.js` | All shared behaviour: theme, scroll-spy, progress, reveal, filters, Mermaid boot, prose↔diagram binding, and ⌘K search. |
| `stamp.mjs` | Writes the generated blocks into each document between markers. |
| `vendor.mjs` | Downloads Mermaid + the fonts. |
| `build-index.mjs` | Extracts the search index (sections + tasks + quizzes). |
| `build-index-page.mjs` | Generates the front-door `index.html`. |
| `migrate.mjs` | One-shot converter for a pre-refactor document. Already run on all six; kept for reference and for any new document that starts from an old copy. |
| `*.generated.*` | Build output. Committed (the docs must work from a fresh clone), never hand-edited. |

## The markers

Each document carries four regions. Everything between a `START` and its `END`
is generated; everything outside is hand-authored and never touched.

```html
<!-- GS:CORE-CSS:START --> … fonts + core + this doc's components …  <!-- GS:CORE-CSS:END -->
<!-- GS:NAV:START -->      … the cross-document switcher …           <!-- GS:NAV:END -->
<!-- GS:INDEX:START -->    … window.GS_SEARCH = […] …                <!-- GS:INDEX:END -->
<!-- GS:CHROME-JS:START --> … Mermaid tag (if used) + chrome.js …    <!-- GS:CHROME-JS:END -->
```

`INDEX` must precede `CHROME-JS` so `window.GS_SEARCH` exists when the search
UI initialises. The index is a **`<script>` that assigns a global**, never a
`fetch()` — because `fetch()` is blocked on `file://` origins, so a fetched
index would fail silently the moment someone opens a doc off disk.

---

## Recipes

**Change a colour** → edit the token in `core.css`, run `stamp.mjs`.

**Add a document**
1. Add an entry to `DOCS` in `docs.manifest.mjs` (repo, file, title, role, components).
2. Give the HTML file the four marker pairs, `<html data-gs-doc="…">`, and the
   pre-paint theme `<script>` (copy any existing document's first 15 lines).
3. `node docs/_src/build-index.mjs && node docs/_src/stamp.mjs`.

**Add a diagram** → `<div class="diagram-shell"><pre class="mermaid">…</pre></div>`.
The palette classes (`:::auth`, `:::spring`, …) and the generated key are
automatic. To bind prose to a node, mark the node with a class and add
`<span data-node="NodeId">…</span>` in the text.

**Add a quiz** → append one object to the `QUIZZES` array in the learning
guide's own trailing `<script>`. Persistence, scoring, filtering and review
mode all pick it up. Re-run `build-index.mjs` so search sees it.

**Add a roadmap item** → one `<article class="rm-card">` with a
`data-ck` id and an `.rm-verdict` badge. Shipped state is read from the badge,
so a badge reading "Shipped ✓" is the single source of truth — do not also
seed it in JS.

## The rule that keeps this from rotting

**`_src/` never holds document-specific content, and a document never holds
chrome.** Content is markup or data; behaviour is shared. If you find yourself
pasting the same thing into a second document, it belongs in `_src/` instead.
