/* ═══════════════════════════════════════════════════════════════════════════
   build-index-page.mjs — generate the front door (docs/index.html) per repo
   ═══════════════════════════════════════════════════════════════════════════

     node docs/_src/build-index-page.mjs

   There was no way IN to the documentation set: six large HTML files, no
   landing page, no map. This writes one index.html into each repo's docs/,
   generated from the manifest so it always lists exactly the six documents
   and their real links. It carries the same core.css (via the stamp markers)
   and the same search, so the front door is part of the system, not a
   separate hand-made page that drifts.

   The page is written with the four stamp markers already in place, then
   stamp.mjs fills them — so re-running stamp keeps the front door current
   with everything else.
   ═══════════════════════════════════════════════════════════════════════════ */

import { writeFile } from "node:fs/promises";
import { join } from "node:path";
import { DOCS, REPOS, repoById, hrefBetween } from "./docs.manifest.mjs";

// A synthetic "document" standing in for the front door of a given repo, so
// hrefBetween() can compute links from it to each real doc.
function frontDoc(repoId) {
  return { id: "__index__", repo: repoId, file: "docs/index.html", role: null };
}

function card(from, doc) {
  const href = hrefBetween(from, doc);
  const roleClass = doc.role === "backend" ? "role-be" : "role-fe";
  return `      <a class="fd-card ${roleClass}" href="${href}">
        <span class="fd-role">${doc.role}</span>
        <span class="fd-title">${doc.title}</span>
        <span class="fd-blurb">${doc.blurb}</span>
      </a>`;
}

function page(repo) {
  const from = frontDoc(repo.id);
  const frontendCards = DOCS.filter((d) => d.role === "frontend").map((d) => card(from, d)).join("\n");
  const backendCards = DOCS.filter((d) => d.role === "backend").map((d) => card(from, d)).join("\n");
  const otherRepoName = repo.id === "backend" ? "gameStore" : "gameStore-backend";

  return `<!DOCTYPE html>
<html lang="en" data-gs-doc="index">
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Game Store — Documentation</title>

<script>
  /* Pre-paint theme apply — see any document for the rationale. */
  try {
    var t = localStorage.getItem("gs-docs-theme");
    if (t === "light" || t === "dark") document.documentElement.dataset.theme = t;
  } catch (e) {}
</script>

<!-- GS:CORE-CSS:START -->
<!-- GS:CORE-CSS:END -->

<style>
  /* Front-door-only layout. Everything visual comes from core.css tokens. */
  .fd-hero { padding: 72px 0 32px; }
  .fd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 8px 0 22px; }
  .fd-col-h {
    font-family: var(--mono); font-size: 11px; letter-spacing: .14em;
    text-transform: uppercase; color: var(--ink-faint); margin: 26px 0 4px;
    display: flex; align-items: center; gap: 10px;
  }
  .fd-col-h::before { content: ""; width: 22px; height: 1px; background: var(--accent); }
  .fd-card {
    display: flex; flex-direction: column; gap: 6px;
    background: var(--panel); border: 1px solid var(--line);
    border-radius: var(--radius); padding: 18px 20px;
    text-decoration: none; color: inherit;
    box-shadow: var(--shadow-sm);
    transition: transform .2s var(--ease), box-shadow .2s var(--ease), border-color .2s var(--ease);
  }
  .fd-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); border-color: var(--line-2); text-decoration: none; }
  .fd-card .fd-role {
    font-family: var(--mono); font-size: 9.5px; letter-spacing: .1em;
    text-transform: uppercase; align-self: flex-start;
    padding: 2px 8px; border-radius: 999px; border: 1px solid var(--line-2);
  }
  .fd-card.role-be .fd-role { color: var(--spring); border-color: var(--spring-dim); background: var(--spring-wash); }
  .fd-card.role-fe .fd-role { color: var(--info); border-color: var(--info-dim); background: var(--info-wash); }
  .fd-card .fd-title { font-family: var(--display); font-weight: 700; font-size: 1.15rem; }
  .fd-card .fd-blurb { color: var(--ink-soft); font-size: .9rem; line-height: 1.5; }
  .fd-hint {
    font-family: var(--mono); font-size: 12px; color: var(--ink-faint);
    border: 1px solid var(--line); border-radius: var(--radius-sm);
    padding: 12px 16px; margin: 8px 0 0; line-height: 1.7;
  }
  .fd-hint kbd { background: var(--panel-2); border: 1px solid var(--line); border-radius: 4px; padding: 0 5px; }
  @media (max-width: 720px) { .fd-grid { grid-template-columns: 1fr; } }
</style>

<!-- GS:NAV:START -->
<!-- GS:NAV:END -->

<header class="hero fd-hero">
  <div class="wrap">
    <p class="eyebrow">Game Store · Documentation</p>
    <h1>One system, six views</h1>
    <p class="lede">
      A full-stack Game Store: a React&nbsp;19 SPA and a Spring&nbsp;Boot API, documented as a single
      cross-linked set. Start anywhere — the briefs explain how each half is built, the roadmaps rank
      what's next, and the learning guide reads the codebase with you. Press <kbd>⌘K</kbd> to search
      all six at once.
    </p>
  </div>
</header>

<section id="map">
  <div class="wrap">
    <p class="fd-col-h">Frontend · React 19 + Vite</p>
    <div class="fd-grid">
${frontendCards}
    </div>

    <p class="fd-col-h">Backend · Spring Boot</p>
    <div class="fd-grid">
${backendCards}
    </div>

    <p class="fd-hint">
      These pages are self-contained: open any of them straight from disk, no server required.
      The companion repo (<code>${otherRepoName}</code>) is expected to sit next to this one for the
      cross-repo links to resolve.
    </p>
  </div>
</section>

<footer>
  <div class="wrap">
    Game Store documentation set · generated front door · press <kbd>⌘K</kbd> to search.
  </div>
</footer>

<!-- GS:INDEX:START -->
<!-- GS:INDEX:END -->
<!-- GS:CHROME-JS:START -->
<!-- GS:CHROME-JS:END -->
</html>
`;
}

for (const repo of REPOS) {
  const out = join(repo.root, "docs", "index.html");
  await writeFile(out, page(repo), "utf8");
  console.log(`wrote ${out}`);
}
console.log(`\nnow run: node docs/_src/stamp.mjs   (to fill the markers)`);
