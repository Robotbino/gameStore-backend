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
    // Read data-theme directly — the pre-paint <script> in <head> guarantees
    // it is always set (from localStorage or OS detection) before any other JS
    // runs.  This is the same signal the CSS [data-theme] selectors key on, so
    // the Mermaid palette and the CSS tokens can never disagree.
    return document.documentElement.dataset.theme === "light" ? "light" : "dark";
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

    function apply(next) {
      root.dataset.theme = next;
      root.dataset.themePinned = "1";
      // Written as a raw string (not JSON) so the pre-paint inline script in
      // every document can read it back with one localStorage.getItem call
      // before any parser is available. This is the one key that does NOT go
      // through store.set() — it is intentionally the exception.
      try { localStorage.setItem("gs-docs-theme", next); } catch (e) {}
      sync();
      themeListeners.forEach(function (fn) { try { fn(next); } catch (e) {} });
    }

    btn.addEventListener("click", function () {
      var next = renderedScheme() === "dark" ? "light" : "dark";

      /* Where the browser has view transitions, the flip is a circular wipe
         out of the button rather than an instant repaint: the same change,
         but the eye is told where it came from. startViewTransition() calls
         its argument synchronously and takes the before/after snapshots
         itself, so the work above is untouched — only its framing is. The
         stylesheet cancels the default cross-fade so this clip is the
         whole animation. */
      if (!motionOK || !document.startViewTransition) return apply(next);

      var r = btn.getBoundingClientRect();
      var x = r.left + r.width / 2;
      var y = r.top + r.height / 2;
      var far = Math.max(
        Math.hypot(x, y), Math.hypot(window.innerWidth - x, y),
        Math.hypot(x, window.innerHeight - y), Math.hypot(window.innerWidth - x, window.innerHeight - y)
      );
      var run = document.startViewTransition(function () { apply(next); });
      run.ready.then(function () {
        root.animate(
          { clipPath: ["circle(0px at " + x + "px " + y + "px)",
                       "circle(" + far + "px at " + x + "px " + y + "px)"] },
          { duration: 480, easing: "cubic-bezier(.2,.7,.3,1)",
            pseudoElement: "::view-transition-new(root)" }
        );
      }, function () {});
    });

    sync();

    // The stylesheet flips live when the OS scheme changes; tell the diagram
    // layer so it can repaint too. An explicit viewer choice outranks the OS,
    // but the pre-paint script also sets data-theme (to avoid the ambiguous
    // "light dark" state), so we distinguish via data-theme-pinned.
    if (window.matchMedia) {
      window.matchMedia("(prefers-color-scheme: light)").addEventListener("change", function () {
        if (root.dataset.themePinned) return;
        var next = this.matches ? "light" : "dark";
        root.dataset.theme = next;
        sync();
        themeListeners.forEach(function (fn) { try { fn(next); } catch (e) {} });
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
      // Published once, read by everything that draws progress: the rail
      // spine scales from it and the back-to-top ring is a conic gradient
      // swept by it. One number, three pictures of it.
      root.style.setProperty("--read", frac.toFixed(4));
      toTop.classList.toggle("show", root.scrollTop > 600);
      ticking = false;
    });
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ═══════════════════════════════════════════════════════════════════════
     OUTLINE RAIL
     ═══════════════════════════════════════════════════════════════════════
     A fixed list of the document's own sections, in the track the shell grid
     holds open for it on wide screens. It replaces the horizontal strip up
     there rather than joining it — two controls for one job is one too many —
     and it answers the question the strip could not: how much of this is
     left. The spine beside it is the reading position.

     Built from <h2>s, so a document that gains a section gains a rail entry
     with no other edit. Only sections carrying an id are listed, because an
     entry that cannot be linked to is decoration. */

  /* The rail is fixed, so it needs to know where the centred shell actually
     starts. `50vw` is the obvious answer and the wrong one: `vw` counts the
     scrollbar and a centred block does not, so the rail would sit a few
     pixels off on every platform that reserves gutter. Measure the real
     element instead, and again whenever the viewport changes. */
  var shellRef = document.querySelector("section > .wrap") || document.querySelector(".wrap");
  function measureShell() {
    if (!shellRef) return;
    var box = shellRef.getBoundingClientRect();
    var pad = parseFloat(getComputedStyle(shellRef).paddingLeft) || 0;
    root.style.setProperty("--rail-left", Math.round(box.left + pad) + "px");
  }
  measureShell();
  window.addEventListener("resize", measureShell, { passive: true });

  (function outlineRail() {
    if (root.dataset.gsRail === "off") return;
    var secs = [].slice.call(document.querySelectorAll("section[id]")).filter(function (s) {
      return s.querySelector(":scope > .wrap > h2") || s.querySelector("h2");
    });
    // Under three sections there is nothing to navigate and the rail is just
    // furniture; the strip at the top already covers that case.
    if (secs.length < 3) return;

    var rail = document.createElement("nav");
    rail.className = "doc-rail";
    rail.setAttribute("aria-label", "Sections in this document");

    var list = document.createElement("ol");
    secs.forEach(function (s, i) {
      var h2 = s.querySelector(":scope > .wrap > h2") || s.querySelector("h2");
      var li = document.createElement("li");
      li.className = "r-sec";
      li.dataset.for = s.id;
      var a = document.createElement("a");
      a.href = "#" + s.id;
      a.innerHTML = '<span class="r-n">' + (i + 1 < 10 ? "0" : "") + (i + 1) + "</span>";
      a.appendChild(document.createTextNode(h2.textContent.replace(/#\s*$/, "").trim()));
      li.appendChild(a);
      list.appendChild(li);
    });

    rail.innerHTML = '<span class="rail-spine" aria-hidden="true"><i></i></span>' +
                     '<p class="rail-h">On this page</p>';
    rail.appendChild(list);

    var meta = document.createElement("p");
    meta.className = "rail-meta";
    rail.appendChild(meta);

    document.body.appendChild(rail);
    rail.classList.add("ready");
    measureShell();
  })();


  /* ── Scroll-spy ──
     Tracks which sections are on screen and lights the matching nav link, in
     the horizontal strip and the outline rail alike — one observer, both
     controls, so they can never disagree about where the reader is.
     The insets clear the sticky nav at the top and stop the section below
     claiming focus while you are still reading the one above. */
  (function scrollSpy() {
    var navLinks = [].slice.call(document.querySelectorAll('.subnav a, nav.toc a, .doc-rail a[href^="#"]'));
    if (!navLinks.length || !("IntersectionObserver" in window)) return;

    // A section can be pointed at from both controls, so this maps to a list.
    var linkFor = {};
    navLinks.forEach(function (a) {
      var href = a.getAttribute("href") || "";
      if (href.charAt(0) !== "#") return;
      var id = href.slice(1);
      (linkFor[id] = linkFor[id] || []).push(a);
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
      paint();
    }, { rootMargin: "-76px 0px -55% 0px", threshold: 0 });

    function paint() {
      /* Of the sections on screen, the one being read is the LAST whose top
         has passed under the sticky bar — not the first that happens to be
         intersecting. Those differ constantly: scroll to a heading and the
         previous section is still showing a sliver at the top of the
         viewport, which is enough to keep it "first" and leave the rail
         pointing one section behind the reader for the whole of the next
         screenful. Falls back to the first visible section for the case
         where the reader is above all of them. */
      var line = (parseFloat(getComputedStyle(root).getPropertyValue("--sticky-h")) || 46) + 30;
      var current = null;
      for (var i = 0; i < spied.length; i++) {
        if (!visible[spied[i].id]) continue;
        if (current === null) current = spied[i];
        if (spied[i].getBoundingClientRect().top <= line) current = spied[i];
      }
      navLinks.forEach(function (a) {
        a.classList.remove("active");
        var li = a.closest(".r-sec");
        if (li) li.classList.remove("is-active");
      });
      if (current && linkFor[current.id]) {
        linkFor[current.id].forEach(function (a) {
          a.classList.add("active");
          var li = a.closest(".r-sec");
          if (li) li.classList.add("is-active");
          keepInView(a);
        });
      }
      // Everything above the section being read is marked as read, which is
      // what turns the rail from a list of links into a position in a
      // document.
      var seen = current === null;
      railItems.forEach(function (li) {
        if (current && li.dataset.for === current.id) seen = true;
        li.classList.toggle("is-read", !seen);
      });
    }

    /* The bar is a horizontal scroller with sixteen entries, so the section you
       are actually in is regularly the one clipped at an edge — reading as a
       truncated "CAC" rather than "CACHING". Nudge it back into the strip.

       Deliberately not scrollIntoView(): that walks every scrollable ancestor,
       so it would also scroll the document vertically and fight the anchor the
       reader just clicked. Only scrollLeft on the strip itself moves here. */
    function keepInView(link) {
      var strip = link.parentNode;
      // The rail is a vertical list; only the horizontal strip needs nudging.
      if (!strip || strip.classList.contains("r-sec")) return;
      if (strip.scrollWidth <= strip.clientWidth) return;
      var pad = 24;
      var left = link.offsetLeft - strip.offsetLeft;
      var right = left + link.offsetWidth;
      var to = null;
      if (left - pad < strip.scrollLeft) to = left - pad;
      else if (right + pad > strip.scrollLeft + strip.clientWidth) to = right + pad - strip.clientWidth;
      if (to === null) return;
      strip.scrollTo({ left: Math.max(0, to), behavior: motionOK ? "smooth" : "auto" });
    }

    var railItems = [].slice.call(document.querySelectorAll(".doc-rail .r-sec"));
    spied.forEach(function (s) { spy.observe(s); });

    // The observer fires on boundary crossings only, and these sections run to
    // several screenfuls — so scrolling within one produces no callback and
    // the answer above would go stale. Re-run it on scroll as well; it reads
    // no layout beyond the sections already known to be on screen.
    var spyTick = false;
    window.addEventListener("scroll", function () {
      if (spyTick) return;
      spyTick = true;
      requestAnimationFrame(function () { paint(); spyTick = false; });
    }, { passive: true });
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
      body.className = "dg-full-body mermaid";
      body.innerHTML = shell.querySelector(".mermaid").innerHTML;
      var close = document.createElement("button");
      close.type = "button";
      close.className = "dg-full-close";
      close.textContent = "close";
      close.addEventListener("click", function () { dlg.close(); });
      dlg.appendChild(close);
      dlg.appendChild(body);
      dlg.addEventListener("close", function () { dlg.remove(); });
      // Close the fullscreen on theme change so stale-palette SVG is never
      // left sitting on a freshly re-themed background.
      var closeFull = function () { if (dlg.open) dlg.close(); };
      onThemeChange(closeFull);
      dlg.addEventListener("close", function () {
        var idx = themeListeners.indexOf(closeFull);
        if (idx !== -1) themeListeners.splice(idx, 1);
        dlg.remove();
      });
      document.body.appendChild(dlg);
      dlg.showModal();
    });

    tools.appendChild(copy);
    tools.appendChild(full);
    shell.appendChild(tools);
  }

  /* ── Fit floor ──
     With useMaxWidth:false a diagram carries its intrinsic width and the shell
     scrolls whatever will not fit (diagram.css). That is the right trade for a
     diagram far wider than its column and the wrong one for a diagram a few
     percent over, which then scrolls for a difference no reader would have
     noticed had it simply scaled.

     So the SVG is allowed to scale down to its column — but only to a floor,
     because past a point scaling is just a smaller illegible diagram. The
     floor is a share of THIS diagram's own natural width, not a fixed pixel
     value: diagram.css already notes that a fixed floor stretches a SMALL
     diagram UP to meet it, and a per-diagram floor is the answer it points to.

     Published as a custom property on the <pre> rather than as an inline style
     on the <svg>, for two reasons. The browser re-resolves it on every resize
     with no listener of ours; and the fullscreen view copies the <svg> markup
     but not the <pre>, so the clone inherits no floor and still scales freely
     to its dialog — which is exactly what the button is for. */
  /* 0.75 is where scaling stops being the better half of the trade: the
     sequence type is set at 13px, so the floor renders it at ~9.8px — small,
     but still read at a glance. Below that a scaled diagram is just an
     illegible diagram that also lost its scrollbar, so the floor binds and the
     shell scrolls instead. It buys the two flow diagrams sitting at 1282 and
     1338px a silent fit in a ~1016px column; the ones past ~1355px are wide
     enough that no scale short of unreadable would have saved them. */
  var FIT_FLOOR = 0.75;
  function fitDiagram(el) {
    var svg = el.querySelector("svg");
    if (!svg) return;
    var natural = parseFloat(svg.getAttribute("width"));
    if (!natural) { el.style.removeProperty("--dg-floor"); return; }
    el.style.setProperty("--dg-floor", Math.round(natural * FIT_FLOOR) + "px");
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
          fitDiagram(el);
          buildLegend(el);
          bindProse(el);
          addTools(el);
        });
      }, function () {});
    } catch (e) { /* a broken diagram must not take the document with it */ }
  }

  /* One config, read by the first render and by every re-theme after it. It
     used to be written out twice, which is two places for a setting to be
     added to and one place for it to be forgotten.

     ── useMaxWidth: false ──
     Mermaid defaults it to true, which emits width="100%" and leaves the
     browser to scale the SVG down to whatever box it lands in. In the flow
     tabs that box is a ~574px grid column, and a seven-participant sequence
     diagram is ~1450px wide — so it rendered at 44% and the 14px type came out
     at about 6px. Off, the diagram keeps its authored size and the shell
     scrolls instead (see diagram.css), which trades a scrollbar for text you
     can actually read. Set per diagram type, because there is no global. */
  function mermaidConfig(light) {
    return {
      // Rendered by hand, not on load: Mermaid cannot lay out a diagram inside
      // a display:none container — it fails silently and stamps a misleading
      // "Syntax error" bomb — so hidden flow tabs render on reveal instead.
      startOnLoad: false,
      securityLevel: "loose",
      theme: "base",
      // Rounded edges read as flow rather than circuitry, and the extra rank
      // spacing is what stops edge labels sitting on top of the arrowheads.
      flowchart: { curve: "basis", nodeSpacing: 46, rankSpacing: 62, padding: 14, useMaxWidth: false },
      /* mirrorActors doubles every participant along the bottom, which on
         these diagrams is a second row of names and no extra information.

         actorMargin/width/font sizes are tuned down from Mermaid's defaults
         (50 / 150 / 14 / 16) because these are seven-to-nine participant
         diagrams: at the defaults the widest measured 2569px, which no column
         on the page can hold. At these values it is ~2365px and most of the
         set lands between 640 and 1750 — see fitDiagram() for what happens to
         whatever is still too wide.

         actorMargin does NOT go below 32. Mermaid sizes an actor box from
         `width`, not from the name inside it, so a name longer than the box
         overhangs both edges and the margin is the only thing keeping it off
         its neighbour. At 12 the saving looked far better — 2205px — because
         the labels had quietly started overlapping: `AuthenticationController`
         ran 11px into `AuthenticationService`, and four of the nine diagrams
         had at least one collision. 32 is the first value that clears every
         one of them, with 10px to spare.

         `wrap` stays OFF deliberately. Turning it on does cut the widest to
         ~1100px, but Mermaid breaks mid-word to do it: `GamesContro-ller`,
         `PagedResponse<GameRespon-se>`, `/games/all?q=wit-cher`. Hyphenating
         identifiers in an architecture diagram costs more than the scroll it
         saves, and the wrapped lines then collide with the autonumber badges. */
      sequence: {
        mirrorActors: false, boxMargin: 8, noteMargin: 12, messageAlign: "center", useMaxWidth: false,
        actorMargin: 32, width: 110, actorFontSize: 13, messageFontSize: 13
      },
      er: { useMaxWidth: false },
      state: { useMaxWidth: false },
      themeVariables: themeVars(light)
    };
  }

  function initMermaid() {
    if (!window.mermaid) return;
    var light = renderedScheme() === "light";

    window.mermaid.initialize(mermaidConfig(light));

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
      window.mermaid.initialize(mermaidConfig(isLight));
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
      var toggle = nav.querySelector(".gs-keys-btn") || nav.querySelector(".theme-toggle");
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
    // Frontend lives at gameStore/docs/, backend at gameStore-backend/docs/.
    function crossHref(targetDocId, m) {
      // The two front doors are not themselves indexed, so they have no
      // entry to read a role out of — they declare one on <html> instead.
      // Without this they resolved every link as cross-repo and sent the
      // reader to ../../gameStore/docs/ from inside that very folder.
      var meHere = META[DOC_ID] || (root.dataset.gsRole ? { role: root.dataset.gsRole } : null);
      if (meHere && meHere.role === m.role) return m.file;
      if (m.role === "backend") return "../../gameStore-backend/docs/" + m.file;
      return "../../gameStore/docs/" + m.file;
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
    // Anything in the page can ask for the palette — the front door's hero
    // offers it as a call to action rather than hiding it behind a shortcut
    // most readers will never try.
    [].slice.call(document.querySelectorAll("[data-gs-open-search]")).forEach(function (el) {
      el.addEventListener("click", function (ev) { ev.preventDefault(); open(); });
    });

    document.addEventListener("keydown", function (e) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") { e.preventDefault(); open(); }
      else if (e.key === "/" && !/^(input|textarea|select)$/i.test((e.target.tagName || "")) && overlay.hidden) {
        e.preventDefault(); open();
      }
    });
  }

  initSearch();


  /* ═══════════════════════════════════════════════════════════════════════
     AFFORDANCES
     ═══════════════════════════════════════════════════════════════════════
     Small behaviours that share one rule: the document is complete without
     any of them. Each one checks for what it needs and returns quietly if it
     is not there, so a document that never grows a <pre>, a KPI or a hero
     plate pays nothing for the code that would have decorated it. */

  /* ── Toast ──
     One line of feedback for the actions that otherwise happen invisibly.
     A copy that says nothing is indistinguishable from a copy that failed. */
  var toastEl = null, toastTimer = 0;
  function say(html) {
    if (!toastEl) {
      toastEl = document.createElement("div");
      toastEl.className = "gs-toast";
      toastEl.setAttribute("role", "status");
      toastEl.setAttribute("aria-live", "polite");
      document.body.appendChild(toastEl);
    }
    toastEl.innerHTML = html;
    toastEl.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.classList.remove("show"); }, 1900);
  }

  function copyText(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) return navigator.clipboard.writeText(text);
    // execCommand is deprecated, but file:// pages are not a secure context in
    // every browser and navigator.clipboard is undefined there. These documents
    // are explicitly meant to be opened from disk, so the fallback stays.
    return new Promise(function (resolve, reject) {
      try {
        var ta = document.createElement("textarea");
        ta.value = text;
        ta.style.cssText = "position:fixed;opacity:0;pointer-events:none";
        document.body.appendChild(ta);
        ta.select();
        document.execCommand("copy");
        document.body.removeChild(ta);
        resolve();
      } catch (e) { reject(e); }
    });
  }

  /* ── Document menu ──
     A native <details>, so it opens, closes and takes focus with no script at
     all. This adds only the two things the element does not do by itself. */
  (function docMenu() {
    var menu = document.querySelector(".ds-menu");
    if (!menu) return;
    document.addEventListener("click", function (e) {
      if (menu.open && !menu.contains(e.target)) menu.open = false;
    });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && menu.open) {
        menu.open = false;
        var s = menu.querySelector("summary");
        if (s) s.focus();
      }
    });
  })();

  /* ── Copy affordances ──
     Anything a reader might otherwise retype: the link to a section, and the
     contents of a code panel. */
  (function copyables() {
    document.addEventListener("click", function (e) {
      var anchor = e.target.closest && e.target.closest(".h-anchor");
      if (!anchor) return;
      var url = location.href.split("#")[0] + anchor.getAttribute("href");
      copyText(url).then(function () { say("Link copied · <b>" + anchor.getAttribute("href") + "</b>"); },
                         function () {});
    });

    /* `pre.mermaid` is deliberately excluded. Mermaid reads its diagram source
       straight off the element's textContent, so a button appended in here is
       read as a trailing `copy` line of diagram source: sequence diagrams then
       fail outright with a "Syntax error" bomb, and flowcharts quietly grow a
       stray node named "copy". Diagrams get their own copy + fullscreen tools
       from addTools(), which appends to the .diagram-shell around the <pre>
       rather than into it. */
    [].slice.call(document.querySelectorAll("pre:not(.mermaid)")).forEach(function (pre) {
      if (pre.querySelector(".gs-copy")) return;
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "gs-copy";
      btn.textContent = "copy";
      btn.setAttribute("aria-label", "Copy this snippet");
      btn.addEventListener("click", function () {
        // The button lives inside the <pre>, so its own label would be copied
        // along with the snippet if it were read straight off textContent.
        var clone = pre.cloneNode(true);
        var b = clone.querySelector(".gs-copy");
        if (b) b.remove();
        copyText(clone.textContent.replace(/\s+$/, "")).then(function () {
          btn.textContent = "copied";
          btn.classList.add("done");
          setTimeout(function () { btn.textContent = "copy"; btn.classList.remove("done"); }, 1400);
        }, function () { btn.textContent = "failed"; });
      });
      pre.appendChild(btn);
    });
  })();

  /* ── Pointer spotlight ──
     One listener on the document rather than one per card: these pages carry
     forty-odd cards between them, and the handler is identical for all of
     them. The custom properties are written on the card the pointer is over
     and cleared when it leaves, so a card that is never touched never gets
     an inline style at all. */
  (function spotlight() {
    var SEL = ".kpi, .fd-card, .rm-card, .layer-btn, .pair, .quiz, .callout, .s-card";
    [].slice.call(document.querySelectorAll(SEL)).forEach(function (el) { el.classList.add("spot"); });
    if (!window.matchMedia || !window.matchMedia("(hover: hover)").matches) return;
    document.addEventListener("pointermove", function (e) {
      var el = e.target.closest && e.target.closest(SEL);
      if (!el) return;
      var r = el.getBoundingClientRect();
      el.style.setProperty("--mx", (((e.clientX - r.left) / r.width) * 100).toFixed(1) + "%");
      el.style.setProperty("--my", (((e.clientY - r.top) / r.height) * 100).toFixed(1) + "%");
    }, { passive: true });
    document.addEventListener("pointerleave", function (e) {
      var el = e.target.closest && e.target.closest(SEL);
      if (el) { el.style.removeProperty("--mx"); el.style.removeProperty("--my"); }
    }, true);
  })();

  /* ── Counting numbers ──
     The KPI values animate from zero the first time they are scrolled into
     view. The markup keeps the real number — this reads it, counts to it and
     writes it back — so a reader with JS off, reduced motion on, or no
     IntersectionObserver sees the true value immediately and always. */
  (function counters() {
    if (!motionOK || !("IntersectionObserver" in window)) return;
    var nums = [].slice.call(document.querySelectorAll(".kpi .v"));
    if (!nums.length) return;

    var obs = new IntersectionObserver(function (entries, o) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        o.unobserve(e.target);
        var node = firstTextNode(e.target);
        if (!node) return;
        var m = /^\s*(\d[\d,]*)/.exec(node.nodeValue);
        if (!m) return;
        var target = parseInt(m[1].replace(/,/g, ""), 10);
        if (!(target > 0) || target > 100000) return;
        var rest = node.nodeValue.slice(m[0].length);
        var lead = /^\s*/.exec(node.nodeValue)[0];
        var start = performance.now();
        var dur = 620;
        (function step(now) {
          var t = Math.min(1, (now - start) / dur);
          var eased = 1 - Math.pow(1 - t, 3);
          node.nodeValue = lead + Math.round(target * eased).toLocaleString() + rest;
          if (t < 1) requestAnimationFrame(step);
        })(start);
      });
    }, { threshold: 0.5 });

    function firstTextNode(el) {
      for (var i = 0; i < el.childNodes.length; i++) {
        if (el.childNodes[i].nodeType === 3 && /\d/.test(el.childNodes[i].nodeValue)) return el.childNodes[i];
      }
      return null;
    }
    nums.forEach(function (n) { obs.observe(n); });
  })();

  /* ── Document metrics ──
     How many sections, how many words, how long. Measured off the rendered
     page rather than written into it, because a hand-maintained "~20 min
     read" is wrong the first time the document is edited and nobody ever
     notices. Feeds both the hero plate and the foot of the rail. */
  (function docMetrics() {
    var secs = document.querySelectorAll("section[id]");
    if (!secs.length) return;
    var words = 0;
    [].slice.call(secs).forEach(function (s) {
      words += (s.textContent.trim().match(/\S+/g) || []).length;
    });
    // 220 wpm is the usual estimate for technical prose read attentively; the
    // diagrams and tables in these documents pull it lower, not higher, so
    // this rounds up rather than down.
    var mins = Math.max(1, Math.ceil(words / 220));
    var kw = words >= 1000 ? (words / 1000).toFixed(1).replace(/\.0$/, "") + "k" : String(words);

    var plate = document.querySelector(".plate-meta");
    if (plate) {
      plate.innerHTML =
        "<div><dt>Sections</dt><dd>" + secs.length + "</dd></div>" +
        "<div><dt>Words</dt><dd>" + kw + "</dd></div>" +
        "<div><dt>Read</dt><dd>" + mins + "<small> min</small></dd></div>";
    }
    /* The front door describes documents it is not itself inside, so it can
       only count what the search index holds. The index stores an ABRIDGED
       extract per section, not the section — so section counts off it are
       exact and word counts off it are not. Only the exact number is shown.
       An earlier pass did print a read estimate here and it disagreed with
       the same document's own hero by a factor of five, which is worse than
       saying nothing. */
    var setMeta = document.querySelector("[data-gs-set-meta]");
    if (setMeta && Array.isArray(window.GS_SEARCH) && window.GS_SEARCH_META) {
      var docs = Object.keys(window.GS_SEARCH_META).length;
      var sections = window.GS_SEARCH.filter(function (h) { return h.kind === "section"; }).length;
      var repos = {};
      Object.keys(window.GS_SEARCH_META).forEach(function (k) { repos[window.GS_SEARCH_META[k].role] = 1; });
      setMeta.innerHTML =
        "<div><dt>Documents</dt><dd>" + docs + "</dd></div>" +
        "<div><dt>Sections</dt><dd>" + sections + "</dd></div>" +
        "<div><dt>Repos</dt><dd>" + Object.keys(repos).length + "</dd></div>";
    }

    var cards = document.querySelectorAll(".fd-card[data-doc]");
    if (cards.length && Array.isArray(window.GS_SEARCH)) {
      Array.prototype.forEach.call(cards, function (card) {
        var id = card.getAttribute("data-doc");
        var n = window.GS_SEARCH.filter(function (h) {
          return h.doc === id && h.kind === "section";
        }).length;
        var slot = card.querySelector(".fd-meta");
        if (slot && n) slot.textContent = n + (n === 1 ? " section" : " sections");
      });
    }

    var railMeta = document.querySelector(".rail-meta");
    if (railMeta) railMeta.innerHTML = secs.length + " sections · " + kw + " words" +
      "<b>~" + mins + " min read</b>";

    // The hero pulse travels the height of the stack it is drawn beside, and
    // that height depends on how many nodes this document's plate carries.
    var schema = document.querySelector(".schema");
    if (schema) {
      var setH = function () {
        root.style.setProperty("--spine-h", Math.max(40, schema.offsetHeight - 20) + "px");
      };
      setH();
      window.addEventListener("resize", setH, { passive: true });
    }
  })();

  /* ── Cross-document previews ──
     Every document carries the whole search index, so a link into another
     document can be answered without leaving this one: what that section is
     called, and how it opens. The map from href back to document id is built
     by running crossHref() over the manifest rather than by parsing paths, so
     it cannot disagree with the links search itself generates.

     Pointer only. On a touch screen there is no hover to preview from, and
     the tap should just follow the link. */
  (function peek() {
    var INDEX = window.GS_SEARCH, META = window.GS_SEARCH_META;
    if (!Array.isArray(INDEX) || !META) return;
    if (!window.matchMedia || !window.matchMedia("(hover: hover) and (pointer: fine)").matches) return;

    var byHref = {};
    Object.keys(META).forEach(function (id) { byHref[docHref(id, META[id])] = id; });

    function docHref(id, m) {
      var mine = META[DOC_ID] || { role: root.dataset.gsRole };
      if (mine && mine.role === m.role) return m.file;
      if (m.role === "backend") return "../../gameStore-backend/docs/" + m.file;
      return "../../gameStore/docs/" + m.file;
    }

    var card = null, timer = 0;
    function build() {
      card = document.createElement("div");
      card.className = "gs-peek";
      card.setAttribute("role", "tooltip");
      document.body.appendChild(card);
      return card;
    }

    function lookup(href) {
      var hash = href.indexOf("#");
      var file = hash === -1 ? href : href.slice(0, hash);
      var id = byHref[file];
      if (!id) return null;
      var anchor = hash === -1 ? "" : href.slice(hash + 1);
      var hit = null;
      for (var i = 0; i < INDEX.length; i++) {
        if (INDEX[i].doc !== id) continue;
        if (!anchor) { hit = INDEX[i]; break; }        // no anchor → the document's first section
        if (INDEX[i].id === anchor) { hit = INDEX[i]; break; }
      }
      return hit ? { hit: hit, meta: META[id] } : null;
    }

    function show(a) {
      var found = lookup(a.getAttribute("href") || "");
      if (!found) return;
      if (!card) build();
      card.className = "gs-peek role-" + found.meta.role;
      card.innerHTML =
        '<span class="p-doc"><i></i>' + esc(found.meta.title) + "</span>" +
        '<span class="p-title">' + esc(found.hit.title) + "</span>" +
        '<span class="p-text">' + esc(trim(found.hit.text, 150)) + "</span>";

      var r = a.getBoundingClientRect();
      var top = r.bottom + window.scrollY + 10;
      var left = Math.min(Math.max(12, r.left + window.scrollX), window.innerWidth - 332);
      card.style.top = top + "px";
      card.style.left = left + "px";
      card.classList.add("show");
    }
    function hide() { if (card) card.classList.remove("show"); }
    function esc(s) { var d = document.createElement("div"); d.textContent = s || ""; return d.innerHTML; }
    function trim(s, n) {
      s = (s || "").replace(/\s+/g, " ").trim();
      return s.length > n ? s.slice(0, n).replace(/\s+\S*$/, "") + "…" : s;
    }

    document.addEventListener("pointerover", function (e) {
      var a = e.target.closest && e.target.closest('a[href*=".html"]');
      if (!a || a.closest(".ds-pop") || a.closest(".gs-cmdk")) return hide();
      clearTimeout(timer);
      timer = setTimeout(function () { show(a); }, 240);
    });
    document.addEventListener("pointerout", function (e) {
      if (e.target.closest && e.target.closest('a[href*=".html"]')) { clearTimeout(timer); hide(); }
    });
    window.addEventListener("scroll", hide, { passive: true });
  })();

  /* ── Keyboard shortcuts ──
     Controls that only answer to a key press, and never say so, are controls
     that do not exist. `?` lists all of them. */
  (function shortcuts() {
    var isMac = /mac|iphone|ipad|ipod/i.test(navigator.platform || navigator.userAgent || "");
    var mod = isMac ? "⌘" : "Ctrl";
    var overlay = document.createElement("div");
    overlay.className = "gs-keys";
    overlay.hidden = true;
    overlay.innerHTML =
      '<div class="gs-keys-box" role="dialog" aria-modal="true" aria-label="Keyboard shortcuts">' +
      '<h2>Keyboard shortcuts<button type="button" class="gs-keys-close">esc</button></h2>' +
      '<p class="k-sub">Every key this document answers to.</p>' +
      '<dl class="gs-keys-grid">' +
      "<dt>Finding things</dt>" +
      "<dd>Search all documents<kbd>" + mod + " K</kbd></dd>" +
      "<dd>Search, without the modifier<kbd>/</kbd></dd>" +
      "<dt>Moving around</dt>" +
      "<dd>Next section<kbd>j</kbd></dd>" +
      "<dd>Previous section<kbd>k</kbd></dd>" +
      "<dd>Top of document<kbd>g g</kbd></dd>" +
      "<dt>The page itself</dt>" +
      "<dd>Light / dark<kbd>t</kbd></dd>" +
      "<dd>This list<kbd>?</kbd></dd>" +
      "<dd>Close anything open<kbd>esc</kbd></dd>" +
      "</dl></div>";
    document.body.appendChild(overlay);

    var lastFocus = null;
    function open() {
      lastFocus = document.activeElement;
      overlay.hidden = false;
      overlay.querySelector(".gs-keys-close").focus();
    }
    function close() {
      overlay.hidden = true;
      if (lastFocus && lastFocus.focus) lastFocus.focus();
    }
    overlay.addEventListener("click", function (e) {
      if (e.target === overlay || e.target.closest(".gs-keys-close")) close();
    });
    // A modal that lets focus wander behind it is not modal. Two stops here
    // (the close button and the box), so the cycle is short and explicit.
    overlay.addEventListener("keydown", function (e) {
      if (e.key === "Escape") { e.preventDefault(); close(); }
      if (e.key === "Tab") { e.preventDefault(); overlay.querySelector(".gs-keys-close").focus(); }
    });

    var btn = document.querySelector(".gs-keys-btn");
    if (btn) btn.addEventListener("click", open);

    var secs = [].slice.call(document.querySelectorAll("section[id]"));
    function step(dir) {
      if (!secs.length) return;
      var y = window.scrollY + (parseFloat(getComputedStyle(root).getPropertyValue("--sticky-h")) || 46) + 30;
      var i = 0;
      for (var n = 0; n < secs.length; n++) if (secs[n].offsetTop <= y) i = n;
      var next = Math.min(secs.length - 1, Math.max(0, i + dir));
      secs[next].scrollIntoView({ behavior: motionOK ? "smooth" : "auto", block: "start" });
    }

    var lastG = 0;
    document.addEventListener("keydown", function (e) {
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      var t = e.target;
      if (t && (/^(input|textarea|select)$/i.test(t.tagName) || t.isContentEditable)) return;
      var openOverlay = document.querySelector(".gs-cmdk:not([hidden]), .gs-keys:not([hidden])");
      if (openOverlay && openOverlay !== overlay) return;

      if (e.key === "?") { e.preventDefault(); overlay.hidden ? open() : close(); }
      else if (overlay.hidden && e.key === "j") { e.preventDefault(); step(1); }
      else if (overlay.hidden && e.key === "k") { e.preventDefault(); step(-1); }
      else if (overlay.hidden && e.key === "t") {
        e.preventDefault();
        var tt = document.getElementById("themeToggle");
        if (tt) tt.click();
      } else if (overlay.hidden && e.key === "g") {
        var now = Date.now();
        if (now - lastG < 600) { window.scrollTo({ top: 0, behavior: motionOK ? "smooth" : "auto" }); lastG = 0; }
        else lastG = now;
      }
    });
  })();

  /* ── Resume reading ──
     Which section you were last in, per document. The front door reads it
     back and offers the way in; everything else just records it. Stored
     through the same namespaced wrapper as every other preference, so
     private mode throwing on write costs nothing. */
  (function resume() {
    var secs = [].slice.call(document.querySelectorAll("section[id]"));
    if (secs.length) {
      var save = 0;
      window.addEventListener("scroll", function () {
        clearTimeout(save);
        save = setTimeout(function () {
          if (window.scrollY < 300) return;
          var y = window.scrollY + 120, cur = null;
          for (var i = 0; i < secs.length; i++) if (secs[i].offsetTop <= y) cur = secs[i];
          if (!cur) return;
          var h2 = cur.querySelector("h2");
          store.set("last", {
            doc: DOC_ID,
            id: cur.id,
            title: h2 ? h2.textContent.replace(/#\s*$/, "").trim() : cur.id,
            at: Date.now()
          });
        }, 700);
      }, { passive: true });
    }

    var slot = document.querySelector("[data-gs-resume]");
    if (!slot) return;
    var last = store.get("last", null);
    var META = window.GS_SEARCH_META || {};
    if (!last || !last.id || !META[last.doc]) return;
    var m = META[last.doc];
    var mine = META[DOC_ID] || { role: root.dataset.gsRole };
    var href = (mine && mine.role === m.role) ? m.file
             : (m.role === "backend" ? "../../gameStore-backend/docs/" + m.file
                                     : "../../gameStore/docs/" + m.file);
    var a = document.createElement("a");
    a.className = "fd-resume";
    a.href = href + "#" + last.id;
    a.innerHTML = '<span class="fr-k">Pick up where you left off</span>' +
                  '<span class="fr-t">' + m.title + " · " + last.title.replace(/</g, "&lt;") + "</span>" +
                  '<span class="fr-go" aria-hidden="true">→</span>';
    slot.appendChild(a);
    slot.hidden = false;
  })();

  /* ── One easter egg ──
     The product these documents describe is a near-black storefront with a
     single gold light over the door (DESIGN.md calls it the Midnight
     Marquee). The docs deliberately do not wear that identity — they are
     documentation, and the semantic diagram hues need the room. But the
     konami code lends them the marquee for as long as you want it, and it
     costs a dozen tokens to offer. It is listed in the shortcut sheet, so it
     is findable rather than merely hidden. */
  (function marquee() {
    var SEQ = "ArrowUp ArrowUp ArrowDown ArrowDown ArrowLeft ArrowRight ArrowLeft ArrowRight b a".split(" ");
    var at = 0, wasTheme = null;
    document.addEventListener("keydown", function (e) {
      at = (e.key === SEQ[at] || e.key === (SEQ[at] || "").toLowerCase()) ? at + 1 : 0;
      if (at < SEQ.length) return;
      at = 0;
      if (root.dataset.skin === "marquee") {
        delete root.dataset.skin;
        if (wasTheme) { root.dataset.theme = wasTheme; wasTheme = null; }
        say("Marquee off");
      } else {
        // The marquee is a dark identity — there is no light half of it — and
        // Mermaid resolves its palette from the rendered theme. Pin dark for
        // the duration so the diagrams do not end up light-on-black, and put
        // the reader's own choice back on the way out. Never persisted: the
        // egg is a mood, not a preference.
        wasTheme = renderedScheme();
        root.dataset.theme = "dark";
        root.dataset.skin = "marquee";
        say("<b>Midnight Marquee</b> · konami again to undo");
      }
      themeListeners.forEach(function (fn) { try { fn(renderedScheme()); } catch (err) {} });
    });
  })();


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

