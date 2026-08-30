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

/* `data-doc` is what lets chrome.js count that document's sections out of the
   search index, so a card's "13 sections" cannot drift from the document it
   points at. The slot stays empty — and collapses — until that runs. */
function card(from, doc) {
  const href = hrefBetween(from, doc);
  const roleClass = doc.role === "backend" ? "role-be" : "role-fe";
  return `      <a class="fd-card ${roleClass}" data-doc="${doc.id}" href="${href}">
        <span class="fd-role">${doc.role}</span>
        <span class="fd-title">${doc.title}</span>
        <span class="fd-blurb">${doc.blurb}</span>
        <span class="fd-meta"></span>
      </a>`;
}

/* The order a first-time reader is actually best served by, which is not the
   order the cards happen to sit in: the half that holds the rules first, then
   the half that consumes it. */
const PATH = [
  ["be-arch", "Start with the API", "The backend holds the rules, so it is the half that explains the other one. Filter chain, then controllers, then persistence."],
  ["fe-arch", "Then the SPA", "How the browser half consumes that API — one axios instance, one auth context, two route guards."],
  ["be-learn", "Read the code with a guide", "Patterns and annotations named as you meet them, with 21 quizzes to check yourself."],
  ["fe-roadmap", "Finish on what is next", "Both roadmaps rank the remaining work and say what was deliberately dropped."],
];

function steps(from) {
  return PATH.map(([id, head, why], i) => {
    const d = DOCS.find((x) => x.id === id);
    return `      <li class="fd-step">
        <span class="fs-n">${String(i + 1).padStart(2, "0")}</span>
        <div>
          <a class="fs-h" href="${hrefBetween(from, d)}">${head}</a>
          <p class="fs-why">${why}</p>
        </div>
      </li>`;
  }).join("\n");
}

function page(repo) {
  const from = frontDoc(repo.id);
  const frontendCards = DOCS.filter((d) => d.role === "frontend").map((d) => card(from, d)).join("\n");
  const backendCards = DOCS.filter((d) => d.role === "backend").map((d) => card(from, d)).join("\n");
  const otherRepoName = repo.id === "backend" ? "gameStore" : "gameStore-backend";
  const role = repo.id === "backend" ? "backend" : "frontend";
  const start = hrefBetween(from, DOCS.find((d) => d.id === "be-arch"));

  /* data-gs-role: the front doors are not themselves in the search index, so
     crossHref() has no entry to read a role out of. Without it every link
     resolved as cross-repo and search sent the reader to
     ../../../GameStore/docs/ from inside that very folder.
     data-gs-rail: two sections is not an outline, so the shell holds no
     track open for one. Declared here rather than discovered from the
     section count, so the first paint is already the right shape. */
  return `<!DOCTYPE html>
<html lang="en" data-gs-doc="index" data-gs-role="${role}" data-gs-rail="off">
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Game Store — Documentation</title>

<script>
  /* Pre-paint theme apply — see any document for the rationale.
     Always pins data-theme so light-dark() and the Mermaid palette agree. */
  try {
    var t = localStorage.getItem("gs-docs-theme");
    if (t !== "light" && t !== "dark") {
      t = window.matchMedia && window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
    }
    document.documentElement.dataset.theme = t;
  } catch (e) {}
</script>

<!-- GS:CORE-CSS:START -->
<!-- GS:CORE-CSS:END -->

<style>
  /* ── Front door ───────────────────────────────────────────────────────────
     The only page in the set that is a landing page rather than a document,
     and the only one that has to answer "what is all this" in one screen.
     Everything visual still comes from the shared tokens; what is local is
     the composition. */
  .fd-hero { padding: 76px 0 40px; }

  .fd-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 30px; }
  .fd-cta {
    display: inline-flex; align-items: center; gap: 9px;
    font-family: var(--mono); font-size: 12.5px;
    padding: 10px 18px; border-radius: 999px; cursor: pointer;
    background: var(--accent); color: var(--on-accent);
    border: 1px solid var(--accent); text-decoration: none;
    transition: transform .18s var(--ease), box-shadow .18s var(--ease),
                background .18s var(--ease), color .18s var(--ease);
  }
  .fd-cta:hover { text-decoration: none; transform: translateY(-2px); box-shadow: var(--shadow-md); }
  .fd-cta.ghost { background: transparent; color: var(--ink-soft); border-color: var(--line-2); }
  .fd-cta.ghost:hover { color: var(--ink); border-color: var(--accent); }
  .fd-cta kbd {
    font-family: var(--mono); font-size: 10px; color: var(--ink-faint);
    background: var(--panel-2); border: 1px solid var(--line);
    border-radius: 4px; padding: 1px 5px;
  }

  .fd-col-h {
    font-family: var(--mono); font-size: 11px; letter-spacing: .14em;
    text-transform: uppercase; color: var(--ink-faint); margin: 34px 0 12px;
    display: flex; align-items: center; gap: 10px;
  }
  .fd-col-h:first-of-type { margin-top: 6px; }
  .fd-col-h::after { content: ""; flex: 1; height: 1px; background: var(--line); }

  .fd-grid {
    display: grid; gap: 14px;
    grid-template-columns: repeat(auto-fit, minmax(268px, 1fr));
  }
  .fd-card {
    display: flex; flex-direction: column; gap: 7px;
    background: var(--panel); border: 1px solid var(--line);
    border-radius: var(--radius); padding: 18px 20px 16px;
    text-decoration: none; color: inherit;
    box-shadow: var(--shadow-sm);
    transition: transform .22s var(--ease), box-shadow .22s var(--ease), border-color .22s var(--ease);
  }
  .fd-card:hover {
    transform: translateY(-3px); box-shadow: var(--shadow-md);
    border-color: var(--line-2); text-decoration: none;
  }
  .fd-card .fd-role {
    font-family: var(--mono); font-size: 10px; letter-spacing: .1em;
    text-transform: uppercase; align-self: flex-start;
    padding: 2px 8px; border-radius: 999px; border: 1px solid var(--line-2);
  }
  .fd-card.role-be .fd-role { color: var(--spring); border-color: var(--spring-dim); background: var(--spring-wash); }
  .fd-card.role-fe .fd-role { color: var(--info);   border-color: var(--info-dim);   background: var(--info-wash); }
  .fd-card .fd-title {
    font-family: var(--display); font-weight: 700; font-size: 1.15rem;
    display: flex; align-items: baseline; gap: 8px;
  }
  .fd-card .fd-title::after {
    content: "→"; margin-left: auto; color: var(--ink-faint);
    transition: transform .22s var(--ease), color .22s var(--ease);
  }
  .fd-card:hover .fd-title::after { transform: translateX(3px); color: var(--accent); }
  .fd-card .fd-blurb { color: var(--ink-soft); font-size: .9rem; line-height: 1.5; flex: 1; }
  /* Filled by chrome.js from the search index, so it cannot drift from the
     document it describes. Empty until then, and collapsed if it stays that way. */
  .fd-card .fd-meta {
    font-family: var(--mono); font-size: 10px; letter-spacing: .06em;
    color: var(--ink-faint); padding-top: 10px; margin-top: 2px;
    border-top: 1px solid var(--line-soft);
  }
  .fd-card .fd-meta:empty { display: none; }

  .fd-hint {
    font-family: var(--mono); font-size: 12px; color: var(--ink-faint);
    border: 1px solid var(--line); border-radius: var(--radius-sm);
    padding: 13px 16px; margin: 26px 0 0; line-height: 1.75;
  }

  /* ── The suggested order ──
     Numbered, because the whole point is that it is a sequence. */
  .fd-path { list-style: none; margin: 0; padding: 0; }
  .fd-step {
    display: flex; gap: 18px; padding: 16px 0;
    border-top: 1px solid var(--line);
  }
  .fd-step:first-child { border-top: 0; padding-top: 0; }
  .fd-step .fs-n {
    font-family: var(--mono); font-size: 11px; color: var(--accent);
    flex: none; width: 22px; padding-top: 4px; font-feature-settings: "tnum";
  }
  .fd-step .fs-h {
    font-family: var(--display); font-weight: 700; font-size: 1.02rem;
    color: var(--ink); text-decoration: none;
  }
  .fd-step .fs-h:hover { color: var(--accent-2); text-decoration: underline; }
  .fd-step .fs-why { margin: 4px 0 0; color: var(--ink-soft); font-size: .9rem; line-height: 1.55; }

  @media (max-width: 620px) { .fd-actions .fd-cta { width: 100%; justify-content: center; } }
</style>

<!-- GS:NAV:START -->
<!-- GS:NAV:END -->

<main id="gs-main" tabindex="-1">
<header class="hero fd-hero">
  <div class="wrap">
    <div class="hero-main">
      <p class="eyebrow">Game Store · Documentation</p>
      <h1>One system, six views</h1>
      <p class="lede">
        A full-stack Game Store: a React&nbsp;19 SPA and a Spring&nbsp;Boot API, documented as a single
        cross-linked set. Start anywhere — the briefs explain how each half is built, the roadmaps rank
        what's next, and the learning guide reads the codebase with you.
      </p>
      <div class="fd-actions">
        <a class="fd-cta" href="${start}">Start with the architecture <span aria-hidden="true">→</span></a>
        <button type="button" class="fd-cta ghost" data-gs-open-search>
          Search all six <kbd>⌘K</kbd>
        </button>
      </div>
    </div>
    <aside class="hero-plate">
      <p class="plate-k">What the set is</p>
      <div class="schema">
        <div class="s-node is-accent"><em>Two briefs</em>how each half is actually built</div>
        <div class="s-node is-auth"><em>One pivot</em>a target architecture, not the code today</div>
        <div class="s-node"><em>Two roadmaps</em>ranked work, paired across the stack</div>
        <div class="s-node is-new"><em>One learning guide</em>how to read the code, with quizzes</div>
      </div>
      <dl class="plate-meta" data-gs-set-meta></dl>
    </aside>
  </div>
</header>

<section id="map">
  <div class="wrap">
    <p class="kicker">The set</p>
    <h2>Six documents, two repositories</h2>
    <p class="section-lede">
      Every page is self-contained and cross-linked to the others. Hover a link into another
      document to see what is on the other side of it before you go.
    </p>

    <div class="fd-resume-slot" data-gs-resume hidden></div>

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
      cross-repo links to resolve. Press <kbd>⌘K</kbd> to search all six at once, or
      <kbd>?</kbd> for everything else the keyboard does.
    </p>
  </div>
</section>

<section id="start">
  <div class="wrap">
    <p class="kicker">Suggested order</p>
    <h2>If this is your first time here</h2>
    <p class="section-lede">
      The six are written to be read in any order, but this one wastes the least time: the half
      that holds the rules first, then the half that consumes it.
    </p>
    <ol class="fd-path">
${steps(from)}
    </ol>
  </div>
</section>
</main>

<footer>
  <div class="wrap">
    <div class="f-row">
      <span>Game Store documentation set · six documents, two repositories.</span>
      <a class="f-next" href="${start}">Start reading <span aria-hidden="true">→</span></a>
    </div>
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
