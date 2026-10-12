/* =========================================================
   Aurea Studio — comportamiento del sitio
   ========================================================= */
(function () {
  "use strict";

  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

  /* ---------- Año dinámico en el pie ---------- */
  const yearEl = $("#year");
  if (yearEl) yearEl.textContent = String(new Date().getFullYear());

  /* ---------- Tema claro / oscuro ---------- */
  const root = document.documentElement;
  const themeToggle = $("#themeToggle");
  const STORAGE_KEY = "aurea-theme";

  function applyTheme(theme) {
    root.setAttribute("data-theme", theme);
    try { localStorage.setItem(STORAGE_KEY, theme); } catch (_) {}
  }

  // Preferencia guardada o la del sistema
  let saved = null;
  try { saved = localStorage.getItem(STORAGE_KEY); } catch (_) {}
  if (saved === "light" || saved === "dark") {
    applyTheme(saved);
  } else if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
    applyTheme("dark");
  }

  if (themeToggle) {
    themeToggle.addEventListener("click", () => {
      const next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
      applyTheme(next);
    });
  }

  /* ---------- Menú móvil ---------- */
  const navToggle = $("#navToggle");
  const nav = $("#primaryNav");

  function closeNav() {
    if (!nav || !navToggle) return;
    nav.classList.remove("open");
    navToggle.setAttribute("aria-expanded", "false");
    navToggle.setAttribute("aria-label", "Abrir menú");
  }

  if (navToggle && nav) {
    navToggle.addEventListener("click", () => {
      const isOpen = nav.classList.toggle("open");
      navToggle.setAttribute("aria-expanded", String(isOpen));
      navToggle.setAttribute("aria-label", isOpen ? "Cerrar menú" : "Abrir menú");
    });
    $$(".nav-link", nav).forEach((link) => link.addEventListener("click", closeNav));
    document.addEventListener("keydown", (e) => { if (e.key === "Escape") closeNav(); });
  }

  /* ---------- Header + progreso + botón arriba ---------- */
  const header = $("#siteHeader");
  const progress = $("#scrollProgress");
  const toTop = $("#toTop");

  function onScroll() {
    const y = window.scrollY || document.documentElement.scrollTop;
    const docH = document.documentElement.scrollHeight - window.innerHeight;
    const pct = docH > 0 ? (y / docH) * 100 : 0;

    if (progress) progress.style.width = pct + "%";
    if (header) header.classList.toggle("scrolled", y > 8);
    if (toTop) toTop.classList.toggle("show", y > 520);
  }

  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  if (toTop) {
    toTop.addEventListener("click", () => {
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }

  /* ---------- Animación de aparición ---------- */
  const revealEls = $$(".reveal");
  if ("IntersectionObserver" in window) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("visible");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });
    revealEls.forEach((el) => io.observe(el));
  } else {
    revealEls.forEach((el) => el.classList.add("visible"));
  }

  /* ---------- Contadores animados ---------- */
  function animateCount(el) {
    const target = parseFloat(el.dataset.count || "0");
    const suffix = el.dataset.suffix || "";
    const duration = 1400;
    const start = performance.now();

    function tick(now) {
      const t = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
      const value = Math.round(target * eased);
      el.textContent = value.toLocaleString("es-ES") + suffix;
      if (t < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  const counters = $$(".stat-value[data-count]");
  if (counters.length && "IntersectionObserver" in window) {
    const co = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          animateCount(entry.target);
          co.unobserve(entry.target);
        }
      });
    }, { threshold: 0.6 });
    counters.forEach((el) => co.observe(el));
  }

  /* ---------- Scroll spy (enlace activo) ---------- */
  const sections = $$("main section[id]");
  const navLinks = $$(".nav-link");
  if (sections.length && navLinks.length && "IntersectionObserver" in window) {
    const spy = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        const id = entry.target.getAttribute("id");
        navLinks.forEach((l) => l.classList.toggle("active", l.getAttribute("href") === "#" + id));
      });
    }, { rootMargin: "-45% 0px -50% 0px", threshold: 0 });
    sections.forEach((s) => spy.observe(s));
  }

  /* ---------- Validación del formulario ---------- */
  const form = $("#contactForm");
  const status = $("#formStatus");

  function setError(field, message) {
    const wrap = field.closest(".field");
    const err = wrap ? $(".error", wrap) : null;
    if (err) err.textContent = message || "";
    if (wrap) wrap.classList.toggle("invalid", Boolean(message));
  }

  function validate() {
    let ok = true;
    const name = $("#name");
    const email = $("#email");
    const message = $("#message");

    if (!name.value.trim()) { setError(name, "Escribe tu nombre."); ok = false; }
    else if (name.value.trim().length < 2) { setError(name, "El nombre es demasiado corto."); ok = false; }
    else setError(name, "");

    const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.value.trim()) { setError(email, "Escribe tu email."); ok = false; }
    else if (!emailRe.test(email.value.trim())) { setError(email, "El email no es válido."); ok = false; }
    else setError(email, "");

    if (!message.value.trim()) { setError(message, "Cuéntanos algo sobre tu proyecto."); ok = false; }
    else if (message.value.trim().length < 10) { setError(message, "El mensaje es demasiado corto."); ok = false; }
    else setError(message, "");

    return ok;
  }

  if (form) {
    // Validación en vivo tras el primer intento
    $$("input, textarea", form).forEach((input) => {
      input.addEventListener("input", () => { if (form.dataset.touched) validate(); });
    });

    form.addEventListener("submit", (e) => {
      e.preventDefault();
      form.dataset.touched = "1";

      if (!validate()) {
        if (status) { status.textContent = "Revisa los campos marcados."; status.classList.add("error"); }
        return;
      }

      // Simulación de envío (front-end). Aquí conectarías tu backend o servicio de email.
      const btn = $("button[type=submit]", form);
      const original = btn.textContent;
      btn.disabled = true;
      btn.textContent = "Enviando...";
      if (status) { status.textContent = ""; status.classList.remove("error"); }

      setTimeout(() => {
        btn.disabled = false;
        btn.textContent = original;
        form.reset();
        form.dataset.touched = "";
        if (status) status.textContent = "¡Gracias! Hemos recibido tu mensaje y te contactaremos pronto.";
      }, 1000);
    });
  }

  /* ---------- Actualiza el año de nuevo por si el DOM cambia ---------- */
  window.addEventListener("load", onScroll, { once: true });
})();
