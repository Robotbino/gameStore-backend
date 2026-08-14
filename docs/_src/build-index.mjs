/* ═══════════════════════════════════════════════════════════════════════════
   build-index.mjs — extract a cross-document search index from the six docs
   ═══════════════════════════════════════════════════════════════════════════

     node docs/_src/build-index.mjs

   Emits the SAME index into a GS:INDEX marker in every document, so ⌘K works
   from any page and finds hits in all six. The index is written as JavaScript
   that assigns window.GS_SEARCH — NOT as a JSON file fetched at runtime.

   ── Why a JS assignment and not fetch('index.json') ──────────────────────
   These documents must open straight off the filesystem. On a file:// origin
   every browser blocks fetch() and XMLHttpRequest for local files (it is a
   CORS "opaque origin"), so an index that has to be fetched simply does not
   load when someone double-clicks the HTML. A <script> that assigns a global
   has no such restriction. This is the single most important constraint in
   the whole search design, and getting it wrong is invisible until someone
   opens the file without a server.

   ── What gets indexed ────────────────────────────────────────────────────
     · every  <section id>            → kind: "section"
     · every  roadmap card            → kind: "task"     (id, verdict, prose)
     · every  quiz question           → kind: "quiz"

   Prose is stripped of tags and collapsed, then truncated — enough for a
   match and a snippet, not the whole section. ~60 KB for six documents, which
   is nothing to hold in memory and needs no worker.
   ═══════════════════════════════════════════════════════════════════════════ */

import { readFile, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { DOCS, docPath, docById } from "./docs.manifest.mjs";

const TEXT_CAP = 380;

const strip = (html) =>
  html
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<template[\s\S]*?<\/template>/gi, " ")
    .replace(/<pre[\s\S]*?<\/pre>/gi, " ")   // code is noise for prose search
    .replace(/<[^>]+>/g, " ")
    .replace(/&lt;/g, "<").replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&").replace(/&nbsp;/g, " ")
    .replace(/&[a-z]+;/gi, " ")
    .replace(/\s+/g, " ")
    .trim();

const clip = (s, n = TEXT_CAP) => (s.length > n ? s.slice(0, n).replace(/\s\S*$/, "") + "…" : s);

/* ── Sections ──────────────────────────────────────────────────────────── */
function sections(html, doc) {
  const out = [];
  // <section id="…">…</section>. Non-greedy; sections don't nest here.
  const re = /<section\s+id="([^"]+)"[^>]*>([\s\S]*?)<\/section>/gi;
  let m;
  while ((m = re.exec(html))) {
    const id = m[1];
    const body = m[2];
    const h = body.match(/<h[12][^>]*>([\s\S]*?)<\/h[12]>/i);
    const kicker = body.match(/class="kicker"[^>]*>([\s\S]*?)<\//i);
    const title = h ? strip(h[1]) : id;
    out.push({
      doc: doc.id,
      id,
      title,
      kicker: kicker ? strip(kicker[1]) : "",
      text: clip(strip(body)),
      kind: "section",
    });
  }
  return out;
}

/* ── Roadmap cards ─────────────────────────────────────────────────────── */
function tasks(html, doc) {
  if (!doc.components.includes("roadmap")) return [];
  const out = [];
  const cards = html.split(/<article class="rm-card/).slice(1);
  for (const card of cards) {
    const id = card.match(/data-ck="([A-Z0-9]+)"/)?.[1];
    if (!id) continue;
    const h = card.match(/<h3[^>]*>([\s\S]*?)<\/h3>/i);
    const verdict = card.match(/class="rm-verdict[^"]*">([\s\S]*?)</i);
    const body = card.slice(0, card.indexOf("</article>") + 1 || undefined);
    out.push({
      doc: doc.id,
      // The card has no anchor of its own, so search jumps to its section.
      id: (html.slice(0, html.indexOf(card)).match(/<section\s+id="([^"]+)"[^>]*>(?![\s\S]*<section)/g) || []).slice(-1)[0]?.match(/id="([^"]+)"/)?.[1] || "",
      title: `${id} · ${h ? strip(h[1]) : ""}`.trim(),
      kicker: verdict ? strip(verdict[1]) : "",
      text: clip(strip(body)),
      kind: "task",
      ref: id,
    });
  }
  return out;
}

/* ── Quiz questions ────────────────────────────────────────────────────── */
function quizzes(html, doc) {
  if (!doc.components.includes("learning")) return [];
  const out = [];
  // The QUIZZES array is authored inline. Pull each question/topic pair out
  // of the source rather than the rendered DOM (this runs in Node).
  const arr = html.match(/const QUIZZES\s*=\s*\[([\s\S]*?)\];/);
  if (!arr) return out;
  const objects = arr[1].split(/\}\s*,\s*\{/);
  for (const o of objects) {
    const q = o.match(/question:\s*(['"`])([\s\S]*?)\1/);
    const topic = o.match(/topic:\s*(['"`])([\s\S]*?)\1/);
    if (!q) continue;
    out.push({
      doc: doc.id,
      id: "quizzes",
      title: strip(q[2]),
      kicker: topic ? `quiz · ${topic[2]}` : "quiz",
      text: "",
      kind: "quiz",
    });
  }
  return out;
}

/* ── Build ─────────────────────────────────────────────────────────────── */
const index = [];
const meta = {};

for (const doc of DOCS) {
  const path = docPath(doc);
  if (!existsSync(path)) { console.warn(`  skip ${doc.id}: file not found`); continue; }
  const html = await readFile(path, "utf8");
  meta[doc.id] = { title: doc.title, role: doc.role, file: doc.file.replace(/^docs\//, "") };

  const s = sections(html, doc);
  const t = tasks(html, doc);
  const q = quizzes(html, doc);
  index.push(...s, ...t, ...q);
  console.log(`  ${doc.id.padEnd(11)} ${String(s.length).padStart(2)} sections  ${String(t.length).padStart(2)} tasks  ${String(q.length).padStart(2)} quizzes`);
}

/* Serialised compactly. The consumer (chrome.js search) reads window.GS_SEARCH
   for the entries and window.GS_SEARCH_META for the per-document titles. */
const payload =
  "/* GENERATED by docs/_src/build-index.mjs — do not edit. */\n" +
  `window.GS_SEARCH_META = ${JSON.stringify(meta)};\n` +
  `window.GS_SEARCH = ${JSON.stringify(index)};\n`;

// Stamp into every document's GS:INDEX marker, plus write a standalone copy
// for the front-door index.html to load with a normal <script src>.
const { writeFileSync } = await import("node:fs");
const { dirname, join } = await import("node:path");
const { fileURLToPath } = await import("node:url");
const SRC = dirname(fileURLToPath(import.meta.url));
writeFileSync(join(SRC, "search-index.generated.js"), payload);

console.log(`\nindex: ${index.length} entries, ${(payload.length / 1024).toFixed(1)} KB`);
console.log(`wrote _src/search-index.generated.js`);
console.log(`now run: node docs/_src/stamp.mjs   (stamps GS:INDEX into each document)`);
