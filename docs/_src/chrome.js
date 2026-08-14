/* ═══════════════════════════════════════════════════════════════════════════
   chrome.js — the behaviour every Game Store document gets
   ═══════════════════════════════════════════════════════════════════════════

   SOURCE FILE. Do not edit the copy inside an HTML document — that copy sits
   between <!-- GS:CHROME-JS:START --> and :END and is overwritten wholesale by

     node docs/_src/stamp.mjs

   ── Why this file exists ─────────────────────────────────────────────────
   All six documents carried their own copy of the theme toggle, reading
   progress bar, back-to-top button, scroll-spy and scroll-reveal — 1,859
   lines of inline JS in total. The copies had drifted: the backend
   architecture brief had a requestAnimationFrame guard on its scroll handler
   and a fail-safe that force-reveals the page after 2.5s; the roadmaps had
   neither, and used a different scroll-spy rootMargin. Same feature, six
   implementations, three behaviours. Where they disagreed, the more careful
   version is the one kept below.

   ── Ground rules ─────────────────────────────────────────────────────────
   · Everything here is progressive enhancement. The documents are readable,
     navigable and printable with this file absent or broken.
   · Every motion effect is gated on prefers-reduced-motion.
   · Every localStorage access is wrapped — private mode throws on write.
   · Nothing here assumes a network. Mermaid is loaded from docs/vendor/.
   ═══════════════════════════════════════════════════════════════════════════ */

(function () {
  "use strict";

  var root = document.documentElement;
  var motionOK = !window.matchMedia || !window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  /* ── Storage ────────────────────────────────────────────────────────────
     One namespace for the whole doc set. The theme is shared across all six;
     everything else is keyed per document so two roadmaps never collide. */
  var DOC_ID = root.getAttribute("data-gs-doc") || "doc";
  var store = {
    get: function (key, fallback) {
      try {
        var raw = localStorage.getItem("gs-docs:" + key);
        return raw === null ? fallback : JSON.parse(raw);
      } catch (e) { return fallback; }
    },
    set: function (key, value) {
      try { localStorage.setItem("gs-docs:" + key, JSON.stringify(value)); } catch (e) {}
    },
    scoped: function (name) { return DOC_ID + ":" + name; }
  };


  /* ═══════════════════════════════════════════════════════════════════════
     THEME
     ═══════════════════════════════════════════════════════════════════════
     The stylesheet answers to :root[data-theme] in both directions, so this
     only flips the attribute and remembers the choice. The rendered scheme is
     read straight out of CSS (--scheme) rather than re-derived here, so the
     data-theme / OS-preference precedence lives in exactly one place.

     Pages with diagrams used to reload on toggle, because Mermaid resolves
     its palette at render time and cannot be re-themed in place. They no
     longer do: renderMermaid() keeps each diagram's source text, so the
     diagrams are simply drawn again in the new palette. */

  function renderedScheme() {
    return getComputedStyle(root).getPropertyValue("--scheme").trim() === "light" ? "light" : "dark";
  }

  var themeListeners = [];
  function onThemeChange(fn) { themeListeners.push(fn); }

  (function themeToggle() {
    var btn = document.getElementById("themeToggle");
    if (!btn) return;

    var label = btn.querySelector(".tt-label");
    var sun = btn.querySelector(".tt-sun");
    var moon = btn.querySelector(".tt-moon");

    function sync() {
      var next = renderedScheme() === "dark" ? "light" : "dark";
      if (label) label.textContent = next === "light" ? "Light" : "Dark";
      // toggleAttribute — the IDL `hidden` property is HTMLElement-only, so
      // assigning to sun.hidden on an <svg> silently no-ops. Set the attribute
      // itself and pair it with the svg[hidden] rule in the stylesheet.
      if (sun) sun.toggleAttribute("hidden", next !== "light");
      if (moon) moon.toggleAttribute("hidden", next !== "dark");
      btn.setAttribute("aria-label", "Switch to " + next + " theme");
      btn.title = "Switch to " + next + " theme";
    }

    btn.addEventListener("click", function () {
      var next = renderedScheme() === "dark" ? "light" : "dark";
      root.dataset.theme = next;
      // Written as a raw string (not JSON) so the pre-paint inline script in
      // every document can read it back with one localStorage.getItem call
      // before any parser is available. This is the one key that does NOT go
      // through store.set() — it is intentionally the exception.
      try { localStorage.setItem("gs-docs-theme", next); } catch (e) {}
      sync();
      themeListeners.forEach(function (fn) { try { fn(next); } catch (e) {} });
    });

    sync();

    // The stylesheet flips live when the OS scheme changes; tell the diagram
    // layer so it can repaint too. An explicit choice outranks the OS.
    if (window.matchMedia) {
      window.matchMedia("(prefers-color-scheme: light)").addEventListener("change", function () {
        if (root.dataset.theme) return;
        sync();
        themeListeners.forEach(function (fn) { try { fn(renderedScheme()); } catch (e) {} });
      });
    }
  })();


  /* ═══════════════════════════════════════════════════════════════════════
     PAGE CHROME — progress, back-to-top, scroll-spy, reveal
     ═══════════════════════════════════════════════════════════════════════ */

  var progress = document.createElement("div");
  progress.id = "readProgress";
  document.body.appendChild(progress);

  var toTop = document.createElement("button");
  toTop.id = "toTop";
  toTop.type = "button";
  toTop.setAttribute("aria-label", "Back to top");
  toTop.textContent = "↑";
  toTop.addEventListener("click", function () {
    window.scrollTo({ top: 0, behavior: motionOK ? "smooth" : "auto" });
  });
  document.body.appendChild(toTop);

  // rAF-guarded: scroll fires far more often than the screen repaints, and
  // this was the one version of the handler that did not do redundant work.
  var ticking = false;
  function onScroll() {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(function () {
      var max = root.scrollHeight - root.clientHeight;
      var frac = max > 0 ? root.scrollTop / max : 0;
      // scaleX(0..1) rather than width % — matches the transform-based CSS,
      // and keeps a scroll-driven update off the layout path.
      progress.style.transform = "scaleX(" + frac + ")";
      toTop.classList.toggle("show", root.scrollTop > 600);
      ticking = false;
    });
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ── Scroll-spy ──
     Tracks which sections are on screen and lights the matching nav link.
     The insets clear the sticky nav at the top and stop the section below
     claiming focus while you are still reading the one above. */
  (function scrollSpy() {
    var navLinks = [].slice.call(document.querySelectorAll(".subnav a, nav.toc a"));
    if (!navLinks.length || !("IntersectionObserver" in window)) return;

    var linkFor = {};
    navLinks.forEach(function (a) {
      var href = a.getAttribute("href") || "";
      if (href.charAt(0) === "#") linkFor[href.slice(1)] = a;
    });

    var spied = [].slice.call(document.querySelectorAll("section[id], [id][data-spy]"))
      .filter(function (s) { return linkFor[s.id]; });
    if (!spied.length) return;

    var visible = {};
    var spy = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) visible[e.target.id] = true;
        else delete visible[e.target.id];
      });
      var current = null;
      for (var i = 0; i < spied.length; i++) {
        if (visible[spied[i].id]) { current = spied[i]; break; }
      }
      navLinks.forEach(function (a) { a.classList.remove("active"); });
      if (current && linkFor[current.id]) linkFor[current.id].classList.add("active");
    }, { rootMargin: "-76px 0px -55% 0px", threshold: 0 });

    spied.forEach(function (s) { spy.observe(s); });
  })();

  /* ── Scroll reveal ──
     .reveal starts at opacity 0, so a failure here would render the document
     blank. Everything is force-revealed after a beat regardless: a doc that
     loses its animation is fine, one that loses its text is not. */
  (function scrollReveal() {
    var targets = [].slice.call(document.querySelectorAll("section, .reveal"));
    if (!targets.length) return;

    if (!motionOK || !("IntersectionObserver" in window)) {
      targets.forEach(function (el) { el.classList.add("in"); });
      return;
    }

    targets.forEach(function (el) { el.classList.add("reveal"); });
    var revealer = new IntersectionObserver(function (entries, obs) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        e.target.classList.add("in");
        obs.unobserve(e.target);
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.04 });

    targets.forEach(function (el) { revealer.observe(el); });

    // Anything already on screen at load should not wait for a scroll event.
    requestAnimationFrame(function () {
      targets.forEach(function (el) {
        if (el.getBoundingClientRect().top < window.innerHeight) el.classList.add("in");
      });
    });
    setTimeout(function () {
      targets.forEach(function (el) { el.classList.add("in"); });
    }, 2500);
  })();

  /* ── Heading anchors ──
     Deep-linking a section is how one document cites another. Added by script
     so the markup stays clean and no document has to hand-write them. */
  (function headingAnchors() {
    var heads = document.querySelectorAll("section[id] > .wrap > h2, section[id] > .wrap > h3");
    Array.prototype.forEach.call(heads, function (h) {
      var sec = h.closest("[id]");
      if (!sec || h.querySelector(".h-anchor")) return;
      var a = document.createElement("a");
      a.className = "h-anchor";
      a.href = "#" + sec.id;
      a.textContent = "#";
      a.setAttribute("aria-label", "Link to this section");
      h.appendChild(a);
    });
  })();

  /* ── Filter chips ──
     Any [data-filter-group] container filters the elements named by its
     data-filter-target selector, matching each element's data-tags against
     the clicked chip's data-filter. Lifted out of the learning guide, where
     it was written once for quizzes and Java cards, so the roadmaps can
     reuse it rather than growing a second copy. */
  (function filters() {
    var groups = document.querySelectorAll("[data-filter-group]");
    Array.prototype.forEach.call(groups, function (group) {
      var sel = group.getAttribute("data-filter-target");
      if (!sel) return;
      group.addEventListener("click", function (ev) {
        var chip = ev.target.closest(".chip");
        if (!chip || !group.contains(chip)) return;
        var tag = chip.getAttribute("data-filter");

        Array.prototype.forEach.call(group.querySelectorAll(".chip"), function (c) {
          var on = c === chip;
          c.classList.toggle("active", on);
          c.setAttribute("aria-pressed", on ? "true" : "false");
        });

        Array.prototype.forEach.call(document.querySelectorAll(sel), function (el) {
          var tags = (el.getAttribute("data-tags") || "").split(/\s+/);
          el.hidden = !(tag === "all" || tags.indexOf(tag) !== -1);
        });

        group.dispatchEvent(new CustomEvent("gs:filter", { detail: { tag: tag }, bubbles: true }));
      });
    });
  })();


  /* ═══════════════════════════════════════════════════════════════════════
     DIAGRAMS
     ═══════════════════════════════════════════════════════════════════════ */

  /* One vocabulary for every diagram in the doc set — both architecture
     briefs share it, so a colour means the same thing in both.
     [stroke, fill, text] per class, per scheme.

     Generated and injected at render time rather than written into each
     diagram. Mermaid resolves colours while laying a diagram out and cannot
     read the CSS variables the rest of the page themes with, so the values
     have to be handed to it either way. Doing it from one table also removes
     a whole bug class: an earlier approach authored dark hexes inline and
     string-replaced them for light mode, and the map was missing three of
     them, so those nodes stayed near-black on a white page. */
  var PALETTE = {
    dark: {
      auth:   ["#f5a623", "#2a2114", "#ffdfa6"],
      spring: ["#7cc44a", "#1a2113", "#cdeeb4"],
      ok:     ["#35d399", "#132420", "#9bf0cf"],
      work:   ["#a094ff", "#1f1c2c", "#d5cfff"],
      data:   ["#22b8cf", "#122326", "#9fe6f2"],
      ext:    ["#d946ef", "#26162c", "#f0bdfa"],
      async:  ["#60a5fa", "#151e2a", "#bcd9ff"],
      fail:   ["#f2687e", "#2a1519", "#ffc2cd"],
      muted:  ["#8b8b97", "#1d1d22", "#e7e7ea"]
    },
    light: {
      auth:   ["#b8770c", "#fdf3e0", "#6b4205"],
      spring: ["#4d8f24", "#eef7e6", "#2c5410"],
      ok:     ["#0f9d70", "#e6f7f0", "#08543a"],
      work:   ["#4c3fd0", "#eeecff", "#33269e"],
      data:   ["#0e7490", "#e3f6fa", "#0b4d5f"],
      ext:    ["#a21caf", "#fbeafd", "#6b127a"],
      async:  ["#2563eb", "#e8f0fe", "#1a3f8f"],
      fail:   ["#be123c", "#fdeaee", "#8a0f2c"],
      muted:  ["#6e6e7a", "#f2f2f4", "#3a3a45"]
    }
  };

  /* What each palette class means, in words. A diagram may relabel a colour
     for its own context via data-legend on the shell; the colour still means
     one thing per diagram. */
  var LEGEND_TEXT = {
    auth:   "auth / trust boundary",
    spring: "Spring machinery",
    ok:     "fast path · already correct",
    work:   "write · added by this design",
    data:   "persistent store",
    ext:    "third party",
    async:  "background · scheduled",
    fail:   "failure path · guard",
    muted:  "unchanged infrastructure"
  };

  function themeVars(light) {
    return light ? {
      background: "#ffffff", primaryColor: "#f2f2f4", primaryBorderColor: "#dcdce1",
      primaryTextColor: "#131316", lineColor: "#8b8b97",
      fontFamily: "'JetBrains Mono', ui-monospace, monospace", fontSize: "14px",
      clusterBkg: "#f7f7f8", clusterBorder: "#c9c9d1",
      actorBkg: "#f2f2f4", actorBorder: "#c6bffa", actorTextColor: "#131316",
      actorLineColor: "#8b8b97", signalColor: "#43434e", signalTextColor: "#131316",
      labelBoxBkgColor: "#f2f2f4", labelBoxBorderColor: "#c6bffa", labelTextColor: "#131316",
      noteBkgColor: "#eeecff", noteBorderColor: "#5546e8", noteTextColor: "#131316"
    } : {
      background: "#16161a", primaryColor: "#1d1d22", primaryBorderColor: "#2a2a31",
      primaryTextColor: "#e7e7ea", lineColor: "#5a5a68",
      fontFamily: "'JetBrains Mono', ui-monospace, monospace", fontSize: "14px",
      clusterBkg: "#131317", clusterBorder: "#33333c",
      actorBkg: "#1d1d22", actorBorder: "#363154", actorTextColor: "#e7e7ea",
      actorLineColor: "#5a5a68", signalColor: "#a2a2ac", signalTextColor: "#e7e7ea",
      labelBoxBkgColor: "#1d1d22", labelBoxBorderColor: "#363154", labelTextColor: "#e7e7ea",
      noteBkgColor: "#1a1826", noteBorderColor: "#7b6cff", noteTextColor: "#e7e7ea"
    };
  }

  // Each diagram's authored source, captured before Mermaid replaces it with
  // SVG. This is what makes an in-place re-theme possible — without it the
  // only way back to the source was to reload the page, which is what the
  // toggle used to do.
  var SRC = new WeakMap();

  function paletteFor(light) {
    var pal = PALETTE[light ? "light" : "dark"];
    return Object.keys(pal).map(function (name) {
      var c = pal[name];
      return "  classDef " + name + " stroke:" + c[0] + ",stroke-width:2px,fill:" + c[1] + ",color:" + c[2] + ";";
    }).join("\n");
  }

  /* classDef is a flowchart/state-diagram feature — a sequence or ER diagram
     rejects it outright, so only those two get the prelude, and it goes after
     the type declaration because nothing may precede that line. */
  function withPalette(source, light) {
    var lines = source.split("\n");
    var head = -1;
    for (var i = 0; i < lines.length; i++) { if (lines[i].trim()) { head = i; break; } }
    if (head === -1) return source;
    if (!/^(flowchart|graph|stateDiagram)/.test(lines[head].trim())) return source;
    lines.splice(head + 1, 0, paletteFor(light));
    return lines.join("\n");
  }

  /* The key lists exactly what the reader can see: classes are read off the
     RENDERED svg, not the source, so a class that was defined but never
     applied never appears. */
  function buildLegend(el) {
    var shell = el.closest(".diagram-shell");
    if (!shell) return;
    var existing = shell.parentNode.querySelector(".dg-legend[data-for='" + (el.id || "") + "']");
    if (existing) existing.remove();

    var local = {};
    try { local = JSON.parse(shell.getAttribute("data-legend") || "{}"); } catch (e) {}

    var used = {};
    Array.prototype.forEach.call(el.querySelectorAll(".node"), function (n) {
      (n.getAttribute("class") || "").split(/\s+/).forEach(function (c) {
        if (LEGEND_TEXT[c]) used[c] = true;
      });
    });
    if (!Object.keys(used).length) return;

    var pal = PALETTE[renderedScheme() === "light" ? "light" : "dark"];
    var ul = document.createElement("ul");
    ul.className = "dg-legend";
    ul.setAttribute("data-for", el.id || "");
    ul.setAttribute("aria-label", "Diagram key — select an entry to isolate it");

    Object.keys(pal).filter(function (c) { return used[c]; }).forEach(function (c) {
      var li = document.createElement("li");
      li.setAttribute("role", "button");
      li.setAttribute("tabindex", "0");
      li.setAttribute("aria-pressed", "false");
      li.setAttribute("data-class", c);

      var sw = document.createElement("span");
      sw.className = "dg-key";
      sw.style.color = pal[c][0];
      sw.style.background = pal[c][1];
      li.appendChild(sw);
      li.appendChild(document.createTextNode(local[c] || LEGEND_TEXT[c]));

      function toggle() {
        var on = li.getAttribute("aria-pressed") !== "true";
        Array.prototype.forEach.call(ul.children, function (other) {
          other.setAttribute("aria-pressed", other === li && on ? "true" : "false");
        });
        Array.prototype.forEach.call(el.querySelectorAll(".node"), function (n) {
          var has = (n.getAttribute("class") || "").split(/\s+/).indexOf(c) !== -1;
          n.classList.toggle("dg-dim", on && !has);
        });
      }
      li.addEventListener("click", toggle);
      li.addEventListener("keydown", function (ev) {
        if (ev.key === "Enter" || ev.key === " ") { ev.preventDefault(); toggle(); }
      });

      ul.appendChild(li);
    });

    shell.after(ul);
  }

  /* Prose ↔ diagram binding. A <span data-node="JwtF"> in the text points at
     a Mermaid node id; hovering it lights the node, and clicking a node that
     has matching prose scrolls to it. Both directions are optional — a
     diagram with no bound prose behaves exactly as before. */
  function bindProse(el) {
    var refs = document.querySelectorAll("[data-node]");
    if (!refs.length) return;

    function nodesFor(id) {
      return el.querySelectorAll('.node[id*="-' + id + '-"], .node#' + CSS.escape(id));
    }

    Array.prototype.forEach.call(refs, function (ref) {
      var id = ref.getAttribute("data-node");
      var nodes = nodesFor(id);
      if (!nodes.length) return;

      function light(on) {
        ref.classList.toggle("dg-active", on);
        Array.prototype.forEach.call(nodes, function (n) { n.classList.toggle("dg-hit", on); });
      }
      ref.addEventListener("mouseenter", function () { light(true); });
      ref.addEventListener("mouseleave", function () { light(false); });
      ref.addEventListener("focus", function () { light(true); });
      ref.addEventListener("blur", function () { light(false); });
      if (!ref.hasAttribute("tabindex")) ref.setAttribute("tabindex", "0");

      Array.prototype.forEach.call(nodes, function (n) {
        n.classList.add("dg-linked");
        n.addEventListener("click", function () {
          ref.scrollIntoView({ behavior: motionOK ? "smooth" : "auto", block: "center" });
          light(true);
          setTimeout(function () { light(false); }, 1600);
        });
      });
    });
  }

  /* Toolbar: copy the diagram's source, or open it fullscreen. Fullscreen is
     the answer to wide flowcharts on a phone, where the shell's horizontal
     scroll is workable but cramped. */
  function addTools(el) {
    var shell = el.closest(".diagram-shell");
    if (!shell || shell.querySelector(".dg-tools")) return;

    var tools = document.createElement("div");
    tools.className = "dg-tools";

    var copy = document.createElement("button");
    copy.type = "button";
    copy.textContent = "copy source";
    copy.addEventListener("click", function () {
      var src = SRC.get(el) || "";
      if (!navigator.clipboard) return;
      navigator.clipboard.writeText(src).then(function () {
        copy.textContent = "copied";
        setTimeout(function () { copy.textContent = "copy source"; }, 1400);
      }, function () {});
    });

    var full = document.createElement("button");
    full.type = "button";
    full.textContent = "fullscreen";
    full.addEventListener("click", function () {
      var dlg = document.createElement("dialog");
      dlg.className = "dg-full";
      var body = document.createElement("div");
      body.className = "dg-full-body";
      body.innerHTML = shell.querySelector(".mermaid").innerHTML;
      var close = document.createElement("button");
      close.type = "button";
      close.className = "dg-full-close";
      close.textContent = "close";
      close.addEventListener("click", function () { dlg.close(); });
      dlg.appendChild(close);
      dlg.appendChild(body);
      dlg.addEventListener("close", function () { dlg.remove(); });
      document.body.appendChild(dlg);
      dlg.showModal();
    });

    tools.appendChild(copy);
    tools.appendChild(full);
    shell.appendChild(tools);
  }

  function renderMermaid(nodes, light) {
    if (!window.mermaid || !nodes.length) return;

    nodes.forEach(function (el) {
      // First pass: remember the authored source. Later passes: put it back,
      // because the element now holds rendered SVG.
      if (!SRC.has(el)) SRC.set(el, el.textContent);
      el.textContent = withPalette(SRC.get(el), light);
      el.removeAttribute("data-processed");
    });

    try {
      window.mermaid.run({ nodes: nodes }).then(function () {
        nodes.forEach(function (el) {
          buildLegend(el);
          bindProse(el);
          addTools(el);
        });
      }, function () {});
    } catch (e) { /* a broken diagram must not take the document with it */ }
  }

  function initMermaid() {
    if (!window.mermaid) return;
    var light = renderedScheme() === "light";

    window.mermaid.initialize({
      // Rendered by hand below, not on load: Mermaid cannot lay out a diagram
      // inside a display:none container — it fails silently and stamps a
      // misleading "Syntax error" bomb — so hidden flow tabs render on reveal.
      startOnLoad: false,
      securityLevel: "loose",
      theme: "base",
      // Rounded edges read as flow rather than circuitry, and the extra rank
      // spacing is what stops edge labels sitting on top of the arrowheads.
      flowchart: { curve: "basis", nodeSpacing: 46, rankSpacing: 62, padding: 14 },
      // mirrorActors doubles every participant along the bottom, which on
      // these diagrams is a second row of names and no extra information.
      sequence: { mirrorActors: false, boxMargin: 8, noteMargin: 12, messageAlign: "center" },
      themeVariables: themeVars(light)
    });

    function visibleDiagrams() {
      return [].slice.call(document.querySelectorAll(".mermaid")).filter(function (n) {
        var panel = n.closest(".flow-panel");
        return !panel || !panel.hidden;
      });
    }

    renderMermaid(visibleDiagrams(), light);

    // A theme change now repaints the diagrams instead of reloading the page.
    onThemeChange(function (scheme) {
      var isLight = scheme === "light";
      window.mermaid.initialize({
        startOnLoad: false, securityLevel: "loose", theme: "base",
        flowchart: { curve: "basis", nodeSpacing: 46, rankSpacing: 62, padding: 14 },
        sequence: { mirrorActors: false, boxMargin: 8, noteMargin: 12, messageAlign: "center" },
        themeVariables: themeVars(isLight)
      });
      renderMermaid([].slice.call(document.querySelectorAll(".mermaid")).filter(function (n) {
        return SRC.has(n);
      }), isLight);
    });

    // Flow tabs: a panel that was hidden at load has never been laid out, so
    // render its diagram the first time it is shown.
    document.addEventListener("gs:panel-shown", function (ev) {
      var panel = ev.detail && ev.detail.panel;
      if (!panel) return;
      var pending = [].slice.call(panel.querySelectorAll(".mermaid")).filter(function (n) {
        return !SRC.has(n);
      });
      renderMermaid(pending, renderedScheme() === "light");
    });
  }

  if (document.querySelector(".mermaid")) {
    var mermaidBooted = false;
    function bootMermaid() {
      if (mermaidBooted || !window.mermaid) return;
      mermaidBooted = true;
      initMermaid();
    }
    // The Mermaid bundle is a deferred external script. Opened over http it is
    // reliably ready by `load`; opened as a file:// double-click the timing can
    // differ, so try at every reasonable moment and poll briefly as a backstop
    // rather than betting on one event firing at the right time.
    bootMermaid();
    document.addEventListener("DOMContentLoaded", bootMermaid);
    window.addEventListener("load", bootMermaid);
    var mermaidTries = 0;
    var mermaidPoll = setInterval(function () {
      bootMermaid();
      if (mermaidBooted || ++mermaidTries > 50) clearInterval(mermaidPoll);
    }, 100);
  }


  /* ═══════════════════════════════════════════════════════════════════════
     SEARCH  (⌘K / Ctrl-K)
     ═══════════════════════════════════════════════════════════════════════
     Cross-document. The index (window.GS_SEARCH) is stamped inline into every
     page from one generated file, so a query on any document finds hits in
     all six. If the index is absent — a document stamped before the index was
     built — the whole feature quietly does not appear. No index, no empty box.

     Zero dependencies, ~60 KB of data, in-memory. Ranking is deliberately
     simple: a title hit outweighs a body hit, an exact phrase beats scattered
     tokens, and same-document hits are grouped so the results read as a map of
     where a term lives across the set. */

  function initSearch() {
    var INDEX = window.GS_SEARCH;
    var META = window.GS_SEARCH_META || {};
    if (!Array.isArray(INDEX) || !INDEX.length) return;

    // A trigger button in the doc-switch, and the search shortcut. The hint
    // names the modifier that matches the viewer's OS — these docs are opened
    // on any platform from GitHub Pages or disk, so a hardcoded ⌘ would be
    // wrong for most readers. The shortcut itself accepts either modifier
    // (see keydown below), so only the label is platform-aware.
    var isMac = /mac|iphone|ipad|ipod/i.test(navigator.platform || navigator.userAgent || "");
    var kbdHint = isMac ? "⌘K" : "Ctrl K";
    var nav = document.querySelector(".doc-switch-inner");
    var btn = document.createElement("button");
    btn.type = "button";
    btn.className = "gs-search-btn";
    btn.setAttribute("aria-label", "Search the documentation (" + kbdHint + ")");
    btn.innerHTML = '<span class="gs-sk-ico" aria-hidden="true">⌕</span><span class="gs-sk-label">Search</span><kbd>' + kbdHint + "</kbd>";
    if (nav) {
      var toggle = nav.querySelector(".theme-toggle");
      nav.insertBefore(btn, toggle || null);
    }

    // Overlay, built once, toggled with the [hidden] attribute.
    var overlay = document.createElement("div");
    overlay.className = "gs-cmdk";
    overlay.hidden = true;
    overlay.innerHTML =
      '<div class="gs-cmdk-box" role="dialog" aria-modal="true" aria-label="Search documentation">' +
      '  <input type="search" class="gs-cmdk-input" placeholder="Search all six documents…" ' +
      '         autocomplete="off" spellcheck="false" aria-label="Search query" />' +
      '  <ul class="gs-cmdk-results" role="listbox"></ul>' +
      '  <div class="gs-cmdk-foot"><kbd>↑</kbd><kbd>↓</kbd> navigate · <kbd>↵</kbd> open · <kbd>esc</kbd> close</div>' +
      "</div>";
    document.body.appendChild(overlay);

    var input = overlay.querySelector(".gs-cmdk-input");
    var list = overlay.querySelector(".gs-cmdk-results");
    var results = [];
    var active = -1;

    function open() {
      overlay.hidden = false;
      input.value = "";
      render("");
      input.focus();
      document.documentElement.style.overflow = "hidden";
    }
    function close() {
      overlay.hidden = true;
      document.documentElement.style.overflow = "";
    }

    // href from THIS document to a result's document + anchor.
    function hrefFor(hit) {
      var m = META[hit.doc];
      if (!m) return "#" + hit.id;
      var here = m.file === (location.pathname.split("/").pop() || "");
      var file = here ? "" : crossHref(hit.doc, m);
      var anchor = hit.id ? "#" + hit.id : "";
      // A task result also carries the card id so we can flash it on arrival.
      var q = hit.ref ? (anchor ? "" : "") : "";
      return (file || "") + anchor + q;
    }
    // Cross-document path. Same repo → bare file; different repo → sibling hop.
    function crossHref(targetDocId, m) {
      var meHere = META[DOC_ID];
      if (meHere && meHere.role === m.role) return m.file;
      var repoDir = m.role === "backend" ? "gameStore-backend" : "gameStore";
      return "../../" + repoDir + "/docs/" + m.file;
    }

    /* Scoring. Tokenise the query; each token contributes by where it lands.
       An exact substring of the whole phrase in the title is the strong
       signal a picker wants to reward. */
    function score(hit, tokens, phrase) {
      var title = hit.title.toLowerCase();
      var kicker = (hit.kicker || "").toLowerCase();
      var text = (hit.text || "").toLowerCase();
      var s = 0;
      for (var i = 0; i < tokens.length; i++) {
        var t = tokens[i];
        if (title.indexOf(t) !== -1) s += 10;
        if (kicker.indexOf(t) !== -1) s += 5;
        if (text.indexOf(t) !== -1) s += 1;
      }
      if (phrase && title.indexOf(phrase) !== -1) s += 15;
      else if (phrase && text.indexOf(phrase) !== -1) s += 3;
      // A section is a better landing place than a single task or quiz when
      // scores are otherwise equal.
      if (hit.kind === "section") s += 0.5;
      return s;
    }

    // Document order is fixed by the manifest, so the results always read
    // front-to-back through the system (frontend docs, then backend) rather
    // than reshuffling on every keystroke.
    var DOC_ORDER = Object.keys(META);

    function search(q) {
      var phrase = q.trim().toLowerCase();
      var tokens = phrase.split(/\s+/).filter(Boolean);

      var scored;
      if (!tokens.length) {
        // Empty query → a table of contents: every section, its own weight.
        scored = INDEX.filter(function (h) { return h.kind === "section"; })
          .map(function (h) { return { hit: h, s: 1 }; });
      } else {
        scored = [];
        for (var i = 0; i < INDEX.length; i++) {
          var s = score(INDEX[i], tokens, tokens.length > 1 ? phrase : "");
          if (s > 0) scored.push({ hit: INDEX[i], s: s });
        }
      }

      // Group by document so each doc's hits stay together — a map of where
      // the term lives, not a reshuffled flat list with a header repeating
      // every few rows. Documents are ordered by their single best hit, and
      // ties fall back to manifest order so the layout is stable.
      var byDoc = {};
      scored.forEach(function (r) {
        (byDoc[r.hit.doc] || (byDoc[r.hit.doc] = [])).push(r);
      });
      var docs = Object.keys(byDoc).sort(function (a, b) {
        var ba = Math.max.apply(null, byDoc[a].map(function (r) { return r.s; }));
        var bb = Math.max.apply(null, byDoc[b].map(function (r) { return r.s; }));
        if (bb !== ba) return bb - ba;
        return DOC_ORDER.indexOf(a) - DOC_ORDER.indexOf(b);
      });

      var out = [];
      docs.forEach(function (d) {
        byDoc[d].sort(function (a, b) { return b.s - a.s; });
        byDoc[d].slice(0, 8).forEach(function (r) { out.push(r); });
      });
      return out.slice(0, 40);
    }

    function highlight(text, tokens) {
      if (!tokens.length) return esc(text);
      var out = esc(text);
      tokens.forEach(function (t) {
        if (!t) return;
        out = out.replace(new RegExp("(" + t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + ")", "ig"), "<mark>$1</mark>");
      });
      return out;
    }
    function esc(s) {
      return String(s).replace(/[&<>"]/g, function (c) {
        return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
      });
    }

    function render(q) {
      var tokens = q.trim().toLowerCase().split(/\s+/).filter(Boolean);
      results = search(q);
      active = results.length ? 0 : -1;
      list.innerHTML = "";

      if (!results.length) {
        var empty = document.createElement("li");
        empty.className = "gs-cmdk-empty";
        empty.textContent = "No matches for “" + q + "”.";
        list.appendChild(empty);
        return;
      }

      var lastDoc = null;
      results.forEach(function (r, i) {
        var hit = r.hit;
        if (hit.doc !== lastDoc) {
          lastDoc = hit.doc;
          var head = document.createElement("li");
          head.className = "gs-cmdk-group";
          var m = META[hit.doc] || {};
          head.innerHTML = '<span class="gs-g-role gs-g-' + (m.role || "") + '">' + esc(m.role || "") + "</span>" + esc(m.title || hit.doc);
          list.appendChild(head);
        }
        var li = document.createElement("li");
        li.className = "gs-cmdk-item";
        li.setAttribute("role", "option");
        li.dataset.idx = String(i);
        li.innerHTML =
          '<span class="gs-i-kind gs-k-' + hit.kind + '">' + hit.kind + "</span>" +
          '<span class="gs-i-body"><span class="gs-i-title">' + highlight(hit.title, tokens) + "</span>" +
          (hit.kicker ? '<span class="gs-i-kicker">' + highlight(hit.kicker, tokens) + "</span>" : "") +
          "</span>";
        li.addEventListener("click", function () { go(i); });
        li.addEventListener("mousemove", function () { setActive(i); });
        list.appendChild(li);
      });
      paintActive();
    }

    function itemEls() { return [].slice.call(list.querySelectorAll(".gs-cmdk-item")); }
    function setActive(i) { active = i; paintActive(); }
    function paintActive() {
      itemEls().forEach(function (el) {
        var on = Number(el.dataset.idx) === active;
        el.classList.toggle("active", on);
        if (on) el.scrollIntoView({ block: "nearest" });
      });
    }
    function move(delta) {
      if (!results.length) return;
      active = (active + delta + results.length) % results.length;
      paintActive();
    }
    function go(i) {
      var r = results[i];
      if (!r) return;
      var hit = r.hit;
      var href = hrefFor(hit);
      close();
      // Same-document jump: set the hash and, for a task, flash the card.
      var m = META[hit.doc] || {};
      var sameDoc = m.file === (location.pathname.split("/").pop() || "");
      if (sameDoc) {
        if (hit.id) location.hash = hit.id;
        if (hit.ref) flashCard(hit.ref);
      } else {
        location.href = href;
      }
    }
    function flashCard(ref) {
      var box = document.querySelector('[data-ck="' + ref + '"]');
      var card = box && box.closest(".rm-card");
      if (!card) return;
      card.scrollIntoView({ behavior: motionOK ? "smooth" : "auto", block: "center" });
      card.classList.add("gs-flash");
      setTimeout(function () { card.classList.remove("gs-flash"); }, 1500);
    }

    input.addEventListener("input", function () { render(input.value); });
    input.addEventListener("keydown", function (e) {
      if (e.key === "ArrowDown") { e.preventDefault(); move(1); }
      else if (e.key === "ArrowUp") { e.preventDefault(); move(-1); }
      else if (e.key === "Enter") { e.preventDefault(); if (active >= 0) go(active); }
      else if (e.key === "Escape") { e.preventDefault(); close(); }
    });
    overlay.addEventListener("click", function (e) { if (e.target === overlay) close(); });
    btn.addEventListener("click", open);

    document.addEventListener("keydown", function (e) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") { e.preventDefault(); open(); }
      else if (e.key === "/" && !/^(input|textarea|select)$/i.test((e.target.tagName || "")) && overlay.hidden) {
        e.preventDefault(); open();
      }
    });
  }

  initSearch();


  /* ── Public surface ──
     Documents keep their own page-specific script (quizzes, roadmap state,
     layer explorers). This is the small amount of chrome they need to reach. */
  window.GS = {
    store: store,
    docId: DOC_ID,
    scheme: renderedScheme,
    onThemeChange: onThemeChange,
    motionOK: motionOK,
    renderMermaid: function (nodes) { renderMermaid(nodes, renderedScheme() === "light"); }
  };
})();
