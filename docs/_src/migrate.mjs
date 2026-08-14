/* ═══════════════════════════════════════════════════════════════════════════
   migrate.mjs — one-shot conversion of an old-style document to the shared core
   ═══════════════════════════════════════════════════════════════════════════

   The five documents that still carry their own inline chrome all follow the
   same pattern:

     1. A pre-paint theme script at the top
     2. Google Fonts <link>s and a <link rel="stylesheet"> to docs-theme.css
     3. One big inline <style>…</style> block
     4. A hand-written <nav class="doc-switch">…</nav>
     5. A final <script>…</script> containing the theme toggle IIFE, some
        document-specific state (quizzes / checkbox persistence), and the
        PAGE CHROME block (progress + back-to-top + scroll-spy + reveal)

   Migration transforms them into:

     1. Pre-paint theme script (unchanged in shape, cleaned up)
     2. <!-- GS:CORE-CSS:START --> <!-- GS:CORE-CSS:END -->
     3. Any surviving inline <style> that is truly document-specific
        (kept, but with the shared prelude removed)
     4. <!-- GS:NAV:START --> <!-- GS:NAV:END -->
     5. <!-- GS:CHROME-JS:START --> <!-- GS:CHROME-JS:END -->
        + a residual <script> holding only the document-specific state

   Doing this by hand five more times would be error-prone and unreviewable.
   This does it in one pass per file and prints exactly what it removed, so
   the diff is easy to sanity-check.

   Usage:  node docs/_src/migrate.mjs <doc-id>
   ═══════════════════════════════════════════════════════════════════════════ */

import { readFile, writeFile } from "node:fs/promises";
import { docById, docPath } from "./docs.manifest.mjs";

const [, , id] = process.argv;
if (!id) { console.error("usage: node docs/_src/migrate.mjs <doc-id>"); process.exit(1); }
const doc = docById(id);
if (!doc) { console.error(`no such doc: ${id}`); process.exit(1); }

const path = docPath(doc);
let s = await readFile(path, "utf8");
const before = s;
const eol = s.includes("\r\n") ? "\r\n" : "\n";

/* Utility — bail out if a marker we intend to insert already exists. Prevents
   a re-run turning `<!-- GS:CORE-CSS:START -->` into
   `<!-- GS:CORE-CSS:START --><!-- GS:CORE-CSS:START -->`. */
function requireAbsent(marker) {
  if (s.includes(marker)) throw new Error(`already migrated: found ${marker}`);
}

/* ── 1. <html> gets the doc id ───────────────────────────────────────── */
if (!/data-gs-doc=/.test(s)) {
  s = s.replace(/<html([^>]*)>/, (m, attrs) => `<html${attrs} data-gs-doc="${doc.id}">`);
}

/* ── 2. Pre-paint theme script → keep the compact idiomatic form ────── */
s = s.replace(
  /<script>\s*\/\* *Apply a stored theme[\s\S]*?<\/script>/,
  [
    "<script>",
    "  /* Pre-paint theme apply — runs BEFORE the stylesheet parses so the page",
    "     never flashes the wrong scheme. Deliberately isolated from chrome.js",
    "     (which runs after DOM ready) and deliberately unstyled. */",
    "  try {",
    "    var t = localStorage.getItem(\"gs-docs-theme\");",
    "    if (t === \"light\" || t === \"dark\") document.documentElement.dataset.theme = t;",
    "  } catch (e) {}",
    "</script>",
  ].join(eol)
);

/* ── 3. Font preconnects + font link + stylesheet link → CORE-CSS ────── */
requireAbsent("<!-- GS:CORE-CSS:START -->");
s = s.replace(
  /<link[^>]*rel="preconnect"[^>]*>\s*<link[^>]*rel="preconnect"[^>]*>\s*<link[^>]*fonts\.googleapis[^>]*>\s*<link[^>]*docs-theme\.css[^>]*\/>/,
  `<!-- GS:CORE-CSS:START -->${eol}<!-- GS:CORE-CSS:END -->`
);

/* ── 4. The main inline <style> that duplicates what core.css now provides ──
   The heuristic: the FIRST <style> block that starts with `* { box-sizing`
   (every one of them does) is the shared one. Any later <style> block is
   document-specific and is left alone. */
const styleRegex = /<style>\s*\*\s*\{\s*box-sizing:\s*border-box;[\s\S]*?<\/style>/;
const styleMatch = s.match(styleRegex);
if (styleMatch) s = s.replace(styleRegex, "");

/* ── 5. Hand-written doc-switch nav → GS:NAV markers ─────────────────── */
requireAbsent("<!-- GS:NAV:START -->");
s = s.replace(
  /<nav class="doc-switch"[^>]*>[\s\S]*?<\/nav>/,
  `<!-- GS:NAV:START -->${eol}<!-- GS:NAV:END -->`
);

/* ── 6a. Remove the standalone <script src="…mermaid.min.js"> tag. It used
       to load Mermaid off a CDN, which broke offline reads. chrome.js loads
       the vendored copy instead, so this tag is no longer needed. */
s = s.replace(/<script[^>]*mermaid\.min\.js[^>]*><\/script>\s*/g, "");

/* ── 6b. Remove any standalone theme-toggle <script>. The architecture
       briefs kept the theme IIFE in its OWN <script> block, separate from
       the block that holds the rest of the page chrome. */
s = s.replace(
  /<script>\s*(?:\/\/|\/\*)[^\n]*Theme toggle[\s\S]*?\}\)\(\);\s*<\/script>/g,
  ""
);

/* ── 6c. Rewrite the trailing <script>. This is where the pre-refactor
       documents mixed FIVE separate concerns:

         · Mermaid initialisation             ← chrome.js does this now
         · Diagram palette + legend builder   ← chrome.js does this now
         · Document-specific behaviour        ← keep (layer explorer, tabs, …)
         · Page chrome (progress, back-top,   ← chrome.js does this now
                        scroll-spy, reveal)
         · Anything else per-doc              ← keep

       Rather than one greedy regex — which either kept too much (leaving a
       duplicated Mermaid init) or too little (nuking the layer explorer as
       collateral damage) — the trailing script is split on its own section
       headers ("// ── Section name ──") and each section is either kept or
       dropped by name. The section names are the ones the humans authored;
       adding a new one to the blacklist is one line. */
requireAbsent("<!-- GS:CHROME-JS:START -->");

const scriptMatches = [...s.matchAll(/<script>[\s\S]*?<\/script>/g)];
if (!scriptMatches.length) throw new Error("no trailing <script> found");
const last = scriptMatches[scriptMatches.length - 1];
let scriptBody = last[0].replace(/^<script>\r?\n?/, "").replace(/\r?\n?<\/script>$/, "");

// Sections chrome.js now owns wholesale.
const SHARED_SECTIONS = [
  "Diagram palette",
  "Reading progress bar",
  "Back to top",
  "Scroll handler",
  "Scroll-spy",
  "Scroll reveal",
];

// Drop the preamble before the first section header — that is invariably the
// Mermaid.initialize({ … }) block, which chrome.js already runs.
scriptBody = scriptBody.replace(
  /^[\s\S]*?(?=\r?\n\s*\/\/\s*──\s+[A-Z])/,
  ""
);

// Drop the PAGE CHROME box-drawing preamble that used to introduce the
// second half of the script — chrome.js now provides everything under it.
scriptBody = scriptBody.replace(
  /\s*\/\*\s*═+\s*\r?\n\s*PAGE CHROME[\s\S]*?═+\s*\*\/\s*(?:const\s+motionOK[^\n]*\r?\n)?/,
  ""
);

// Split on section headers of the form  //  ── NAME  ──  … and keep only the
// ones NOT in SHARED_SECTIONS. The split preserves the header so surviving
// sections still read as annotated.
const HEAD = /^\s*\/\/\s*──\s+([^─\n]+?)\s+──.*$/m;
const kept = [];
let rest = scriptBody;
while (true) {
  const m = rest.match(HEAD);
  if (!m) break;
  const nextMatch = rest.slice(m.index + m[0].length).match(HEAD);
  const sectionEnd = nextMatch ? m.index + m[0].length + nextMatch.index : rest.length;
  // Only stop at a colon or an em/en dash. A hyphen appears INSIDE section
  // names ("Scroll-spy: highlight the subnav link") and had been eating them
  // — "Scroll-spy" was being normalised to "Scroll" and slipping through the
  // SHARED_SECTIONS check, so chrome.js's scroll-spy ran alongside the old
  // inline one and every section link was set twice per scroll.
  const name = m[1].trim().replace(/[:—–].*$/, "").trim();
  const block = rest.slice(m.index, sectionEnd);
  if (!SHARED_SECTIONS.some((s) => name.toLowerCase().startsWith(s.toLowerCase()))) {
    kept.push(block.trimEnd());
  }
  rest = rest.slice(sectionEnd);
}
// Any text before the first header (unusual — the preamble strip above should
// have caught it) survives too.

scriptBody = kept.join(eol + eol).trim();

// The flow-tab handler used to call a bare `renderMermaid()` that lived in
// the same script. chrome.js now owns that function and exposes it as
// window.GS.renderMermaid, so rewrite the reference. Doing it here rather
// than editing every document by hand keeps every doc consistent.
scriptBody = scriptBody.replace(
  /(?<!\w)renderMermaid\s*\(/g,
  "window.GS.renderMermaid("
);

// The section-slicer alone handles what used to need a second pass. Any
// theme-toggle IIFE and any PAGE CHROME box-drawing comment fell inside a
// SHARED_SECTIONS block and was dropped there.
//
// The one place that still needs help is documents whose trailing script has
// NO section headers at all — the roadmaps and the learning guide, where the
// whole "Checkbox persistence" block is one flat run of code. For those the
// section-slicer emits nothing, and we fall back to the original scriptBody
// with the two shared preludes stripped by name.
const residualTrimmed = scriptBody.trim() || (() => {
  let r = last[0].replace(/^<script>\r?\n?/, "").replace(/\r?\n?<\/script>$/, "");
  r = r.replace(/(?:\s*\/\/ *Theme toggle\.[\s\S]*?)\}\)\(\);\s*/, "");
  r = r.replace(/\s*\/\*\s*═+\s*(?:\r?\n)?\s*PAGE CHROME[\s\S]*$/, "");
  return r.trim();
})();

const docSpecific = residualTrimmed
  ? [
      "",
      "<script>",
      "  /* ══════════════════════════════════════════════════════════════",
      "     Document-specific state. The generic chrome (theme, scroll-spy,",
      "     progress, back-to-top, filter chips) lives in GS:CHROME-JS above.",
      "     Only what is unique to THIS document belongs here.",
      "     ══════════════════════════════════════════════════════════════ */",
      residualTrimmed,
      "</script>",
    ].join(eol)
  : "";

const replacement =
  `<!-- GS:CHROME-JS:START -->${eol}<!-- GS:CHROME-JS:END -->` + docSpecific;

s = s.slice(0, last.index) + replacement + s.slice(last.index + last[0].length);

if (s === before) { console.log(`${id}: nothing to migrate`); process.exit(0); }
await writeFile(path, s, "utf8");

// Report the diff shape without dumping it all — one glance to confirm the
// three big regions moved and the residual is roughly the size expected.
const removed = before.length - s.length + replacement.length;
console.log(`migrated ${id}`);
console.log(`  ${before.length.toString().padStart(6)} bytes -> ${s.length.toString().padStart(6)} bytes  (${(before.length - s.length).toLocaleString()} chars pulled to _src)`);
console.log(`  doc-specific script residual: ${residualTrimmed.length} chars`);
console.log(`  now stamp with:  node docs/_src/stamp.mjs ${id}`);
