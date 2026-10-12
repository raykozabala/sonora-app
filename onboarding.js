/* =========================================================
   Aurea — asistente de configuración inicial
   ========================================================= */
(function () {
  "use strict";

  const $ = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

  UI.initTheme();

  /* ---------- Requiere sesión ---------- */
  const user = Aurea.currentUser();
  if (!user) { location.replace("login.html"); return; }

  // Si ya completó el asistente, directo al panel
  const data = Aurea.getUserData(user.id);
  if (data.onboardingComplete) { location.replace("app.html"); return; }

  // Preferencias temporales del asistente
  const draft = {
    company: data.profile.company || "",
    role: data.profile.role || "",
    sector: data.settings.sector || "",
    brandColor: data.settings.brandColor || "#4f46e5",
    theme: data.settings.theme || "light",
    goal: data.settings.goal || "crecer",
    notifications: data.settings.notifications !== false,
  };

  const form = $("#onboardingForm");
  const steps = $$(".wizard-step");
  const dots = $$(".step-dot");
  const backBtn = $("#backBtn");
  const nextBtn = $("#nextBtn");
  const hint = $("#stepHint");
  let current = 1;
  const total = steps.length;

  /* ---------- Estado inicial visual ---------- */
  document.documentElement.style.setProperty("--accent", draft.brandColor);
  document.documentElement.setAttribute("data-theme", draft.theme);

  const companyInput = $("#company");
  const roleInput = $("#role");
  const sectorInput = $("#sector");
  companyInput.value = draft.company;
  roleInput.value = draft.role;
  if (draft.sector) sectorInput.value = draft.sector;

  /* Prefill de selecciones guardadas */
  const swatchForColor = $(`.swatch[data-color="${draft.brandColor}"]`);
  if (swatchForColor) {
    $$("#swatches .swatch").forEach((s) => s.classList.remove("selected"));
    swatchForColor.classList.add("selected");
  } else {
    $("#customColor").value = draft.brandColor;
  }
  $$("[data-theme-choice]").forEach((c) =>
    c.classList.toggle("selected", c.dataset.themeChoice === draft.theme));
  $$("[data-goal]").forEach((c) =>
    c.classList.toggle("selected", c.dataset.goal === draft.goal));
  $("#notifications").checked = draft.notifications;
  updatePreview();

  /* ---------- Paso 1: prefill en vivo del nombre en la vista previa ---------- */
  companyInput.addEventListener("input", updatePreview);
  function updatePreview() {
    $("#previewName").textContent = companyInput.value.trim() || "Tu empresa";
  }

  /* ---------- Selector de color ---------- */
  $$("#swatches .swatch").forEach((sw) => {
    if (!sw.dataset.color) return;
    sw.addEventListener("click", () => {
      $$("#swatches .swatch").forEach((s) => s.classList.remove("selected"));
      sw.classList.add("selected");
      draft.brandColor = sw.dataset.color;
      document.documentElement.style.setProperty("--accent", draft.brandColor);
    });
  });
  $("#customColor").addEventListener("input", (e) => {
    draft.brandColor = e.target.value;
    $$("#swatches .swatch").forEach((s) => s.classList.remove("selected"));
    document.documentElement.style.setProperty("--accent", draft.brandColor);
  });

  /* ---------- Selector de tema ---------- */
  $$("[data-theme-choice]").forEach((choice) => {
    choice.addEventListener("click", () => {
      $$("[data-theme-choice]").forEach((c) => c.classList.remove("selected"));
      choice.classList.add("selected");
      draft.theme = choice.dataset.themeChoice;
      document.documentElement.setAttribute("data-theme", draft.theme);
    });
  });

  /* ---------- Selector de objetivo ---------- */
  $$("[data-goal]").forEach((choice) => {
    choice.addEventListener("click", () => {
      $$("[data-goal]").forEach((c) => c.classList.remove("selected"));
      choice.classList.add("selected");
      draft.goal = choice.dataset.goal;
      nextBtn.focus();
    });
  });

  /* ---------- Navegación entre pasos ---------- */
  function render() {
    steps.forEach((s) => s.classList.toggle("active", Number(s.dataset.step) === current));
    dots.forEach((d) => {
      const n = Number(d.dataset.dot);
      d.classList.toggle("active", n === current);
      d.classList.toggle("done", n < current);
    });
    backBtn.disabled = current === 1;
    nextBtn.textContent = current === total ? "Finalizar" : "Continuar";
    hint.textContent = "Paso " + current + " de " + total;
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  nextBtn.addEventListener("click", () => {
    if (current === 1) {
      const name = companyInput.value.trim();
      const err = companyInput.closest(".field").querySelector(".error");
      if (!name) {
        companyInput.closest(".field").classList.add("invalid");
        err.textContent = "Escribe el nombre de tu empresa o proyecto.";
        companyInput.focus();
        return;
      }
      companyInput.closest(".field").classList.remove("invalid");
      err.textContent = "";
      draft.company = name;
      draft.role = roleInput.value.trim();
      draft.sector = sectorInput.value;
    }

    if (current < total) { current++; render(); }
    else { finish(); }
  });

  backBtn.addEventListener("click", () => {
    if (current > 1) { current--; render(); }
  });

  /* ---------- Finalizar ---------- */
  function finish() {
    draft.notifications = $("#notifications").checked;

    Aurea.updateProfile(user.id, {
      company: draft.company,
      role: draft.role,
      bio: data.profile.bio || "",
    });
    Aurea.updateSettings(user.id, {
      brandColor: draft.brandColor,
      theme: draft.theme,
      sector: draft.sector,
      goal: draft.goal,
      notifications: draft.notifications,
    });
    Aurea.completeOnboarding(user.id);

    // Tareas de bienvenida para empezar con buen pie
    if (!data.tasks || data.tasks.length === 0) {
      Aurea.addTask(user.id, "Explorar el panel y las métricas");
      Aurea.addTask(user.id, "Invitar a un compañero de equipo");
      Aurea.addTask(user.id, "Definir tu primer objetivo del mes");
    }

    UI.toast("¡Todo listo! Bienvenido a tu panel.", "success");
    nextBtn.disabled = true;
    nextBtn.textContent = "Entrando...";
    setTimeout(() => { location.href = "app.html"; }, 700);
  }

  render();
})();
