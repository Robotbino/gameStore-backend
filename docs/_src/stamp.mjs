/* ═══════════════════════════════════════════════════════════════════════════
   stamp.mjs — write the shared layers into every document
   ═══════════════════════════════════════════════════════════════════════════

     node docs/_src/stamp.mjs            stamp all six
     node docs/_src/stamp.mjs --check    report drift, change nothing (exit 1)
     node docs/_src/stamp.mjs be-arch    stamp one document by id

   ── The problem this solves ──────────────────────────────────────────────
   The documents must be standalone: no external CSS or JS, openable straight
   off the filesystem with no server and no network. That requirement is what
   produced the mess this replaces — six inline copies of the same chrome,
   drifting apart, because inlining by hand is the only way to satisfy it
   manually.

   So the shared code lives once, in _src/, and is COPIED into each document
   between markers. The documents stay literally self-contained; the source of
   truth stays singular. Nothing outside a marker pair is ever touched, so
   hand-authored content and generated content can share a file safely.

   ── Markers ──────────────────────────────────────────────────────────────
     GS:CORE-CSS    fonts + core.css + this document's components
     GS:CHROME-JS   chrome.js
     GS:NAV         the cross-document switcher
     GS:INDEX       the search index  (added in a later stage; optional)

   A document may carry any subset. A missing pair is reported, not fatal —
   that is what lets the six be migrated one at a time.
   ═══════════════════════════════════════════════════════════════════════════ */

import { readFile, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { DOCS, STAMP_TARGETS, docPath, hrefBetween } from "./docs.manifest.mjs";

const SRC = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const CHECK = args.includes("--check");
const only = args.filter((a) => !a.startsWith("--"));

const read = (p) => readFile(p, "utf8");

/* ── Marker replacement ───────────────────────────────────────────────────
   Everything between the two markers is replaced. The markers themselves and
   their surrounding indentation survive, so re-running is a no-op when the
   source has not moved — which is what makes `--check` meaningful in CI. */
function stampRegion(html, name, body) {
  const start = `<!-- GS:${name}:START -->`;
  const end = `<!-- GS:${name}:END -->`;
  const i = html.indexOf(start);
  const j = html.indexOf(end);
  if (i === -1 || j === -1) return { html, found: false };
  if (j < i) throw new Error(`GS:${name} markers are in the wrong order`);

  const eol = html.includes("\r\n") ? "\r\n" : "\n";
  const normalised = body.replace(/\r?\n/g, eol);
  const next = html.slice(0, i + start.length) + eol + normalised + eol + html.slice(j);
  return { html: next, found: true, changed: next !== html };
}

/* ── The cross-document switcher ──────────────────────────────────────────
   Generated from the manifest, grouped by which side of the system each
   document describes, with the current one marked. Nine links used to be
   hand-maintained in six files and had rotted in both directions at once. */
function navFor(doc) {
  const groups = [
    ["frontend", DOCS.filter((d) => d.role === "frontend")],
    ["backend", DOCS.filter((d) => d.role === "backend")],
  ];

  const parts = ['<nav class="doc-switch" aria-label="Documentation set">', '  <div class="doc-switch-inner">'];
  parts.push('    <span class="ds-label">Docs</span>');

  for (const [role, docs] of groups) {
    parts.push(`    <span class="ds-repo">·&nbsp;${role}&nbsp;·</span>`);
    for (const d of docs) {
      const here = d.id === doc.id;
      const href = here ? "" : ` href="${hrefBetween(doc, d)}"`;
      const attrs = here ? ' class="is-here" aria-current="page"' : "";
      // The current document is still an <a> so the row reads as one set of
      // peers; it just has no href, which also removes it from the tab order.
      parts.push(`    <a${href}${attrs}>${d.title}</a>`);
    }
  }

  parts.push(
    '    <button type="button" class="theme-toggle" id="themeToggle" aria-label="Switch theme">',
    '      <svg class="tt-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"',
    '           stroke-linecap="round" aria-hidden="true">',
    '        <circle cx="12" cy="12" r="4" />',
    '        <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />',
    "      </svg>",
    '      <svg class="tt-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"',
    '           stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">',
    '        <path d="M20 14.5A8 8 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5z" />',
    "      </svg>",
    '      <span class="tt-label">Light</span>',
    "    </button>",
    "  </div>",
    "</nav>"
  );
  return parts.join("\n");
}

/* ── Assemble the CSS a given document gets ─────────────────────────────── */
async function cssFor(doc) {
  const banner =
    "/* ══════════════════════════════════════════════════════════════════\n" +
    "   GENERATED — edit docs/_src/core.css and run: node docs/_src/stamp.mjs\n" +
    `   Layers: fonts + core${doc.components.length ? " + " + doc.components.join(" + ") : ""}\n` +
    "   ══════════════════════════════════════════════════════════════════ */";

  const pieces = [banner];

  const fonts = join(SRC, "fonts.generated.css");
  if (existsSync(fonts)) pieces.push(await read(fonts));

  pieces.push(await read(join(SRC, "core.css")));

  for (const c of doc.components) {
    const p = join(SRC, "components", `${c}.css`);
    if (!existsSync(p)) throw new Error(`${doc.id}: no component stylesheet "${c}"`);
    pieces.push(await read(p));
  }

  return `<style>\n${pieces.join("\n\n")}\n</style>`;
}

async function jsFor(doc) {
  const banner =
    "/* GENERATED — edit docs/_src/chrome.js and run: node docs/_src/stamp.mjs */";
  const pieces = [];

  // A document that uses diagrams needs the vendored Mermaid library loaded
  // BEFORE chrome.js runs so initMermaid() sees a defined window.mermaid.
  // vendor/ sits next to every document, so a same-directory relative path
  // resolves on the filesystem, on GitHub Pages, and inside a preview server.
  if (doc.components.includes("diagram")) {
    pieces.push(`<script src="vendor/mermaid.min.js" defer></script>`);
  }

  pieces.push(`<script>\n${banner}\n${await read(join(SRC, "chrome.js"))}\n</script>`);
  return pieces.join("\n");
}

/* ── The search index ─────────────────────────────────────────────────────
   Stamped as an inline <script> that assigns window.GS_SEARCH, NEVER fetched:
   fetch() is blocked on file:// origins, so a fetched index would silently
   fail exactly when someone opens the doc straight off disk. This must be
   stamped BEFORE chrome.js so initSearch() sees the global. Generated by
   build-index.mjs; absent until that has been run, and the search UI simply
   does not appear without it. */
async function indexFor() {
  const p = join(SRC, "search-index.generated.js");
  if (!existsSync(p)) return null;
  return `<script>\n${await read(p)}</script>`;
}

/* ── Run ──────────────────────────────────────────────────────────────── */
const targets = only.length ? STAMP_TARGETS.filter((d) => only.includes(d.id)) : STAMP_TARGETS;
if (!targets.length) {
  console.error(`no documents matched: ${only.join(", ")}`);
  process.exit(1);
}

let drift = 0;
let missing = 0;

for (const doc of targets) {
  const path = docPath(doc);
  if (!existsSync(path)) {
    console.error(`  MISSING FILE  ${doc.id}  ${path}`);
    missing++;
    continue;
  }

  const before = await read(path);
  let html = before;
  const notes = [];

  const searchIndex = await indexFor();
  const regions = [
    ["CORE-CSS", await cssFor(doc)],
    ["NAV", navFor(doc)],
    // INDEX must be stamped before CHROME-JS so window.GS_SEARCH exists when
    // initSearch() runs. The marker order in the file enforces this too.
    ["INDEX", searchIndex],
    ["CHROME-JS", await jsFor(doc)],
  ];

  for (const [name, body] of regions) {
    if (body === null) continue;            // index not built yet
    const r = stampRegion(html, name, body);
    if (!r.found) notes.push(`no ${name} markers`);
    html = r.html;
  }

  const changed = html !== before;
  if (changed) drift++;

  if (changed && !CHECK) await writeFile(path, html, "utf8");

  const status = CHECK ? (changed ? "DRIFT " : "ok    ") : changed ? "写 wrote" : "ok    ";
  console.log(
    `  ${changed && CHECK ? "DRIFT" : changed ? "wrote" : "ok   "}  ${doc.id.padEnd(11)} ${doc.components.join("+") || "core only"}` +
      (notes.length ? `   [${notes.join("; ")}]` : "")
  );
}

if (missing) process.exit(1);
if (CHECK && drift) {
  console.log(`\n${drift} document(s) differ from _src — run: node docs/_src/stamp.mjs`);
  process.exit(1);
}
console.log(CHECK ? "\nall documents match _src" : `\nstamped ${targets.length} document(s)`);
