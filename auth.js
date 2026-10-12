/* =========================================================
   Aurea — lógica de login y registro
   ========================================================= */
(function () {
  "use strict";

  const $ = (s, c = document) => c.querySelector(s);

  UI.initTheme();
  UI.bindThemeToggle($("#themeToggle"));
  const y = $("#year"); if (y) y.textContent = String(new Date().getFullYear());

  /* ---------- Redirección si ya hay sesión ---------- */
  const existing = Aurea.currentUser();
  if (existing) {
    const data = Aurea.getUserData(existing.id);
    location.replace(data.onboardingComplete ? "app.html" : "onboarding.html");
    return;
  }

  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  /* ---------- Mostrar / ocultar contraseña ---------- */
  document.querySelectorAll("[data-toggle-pw]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = document.getElementById(btn.dataset.togglePw);
      if (!input) return;
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      btn.textContent = show ? "Ocultar" : "Mostrar";
    });
  });

  /* ---------- Helpers de validación ---------- */
  function setError(form, name, message) {
    const field = form.querySelector('[name="' + name + '"]');
    const wrap = field ? field.closest(".field") : null;
    const err = form.querySelector('[data-error-for="' + name + '"]');
    if (err) err.textContent = message || "";
    if (wrap) wrap.classList.toggle("invalid", Boolean(message));
  }
  function showAlert(el, message, type) {
    if (!el) return;
    el.textContent = message;
    el.className = "form-alert show " + (type || "error");
  }
  function hideAlert(el) { if (el) el.className = "form-alert"; }

  function busy(btn, on, label) {
    if (!btn) return;
    btn.disabled = on;
    if (on) { btn.dataset.original = btn.textContent; btn.textContent = label || "Procesando..."; }
    else if (btn.dataset.original) { btn.textContent = btn.dataset.original; }
  }

  /* =========================================================
     LOGIN
     ========================================================= */
  const loginForm = $("#loginForm");
  if (loginForm) {
    const alertEl = $("#alert");

    loginForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      hideAlert(alertEl);
      let ok = true;

      const email = $("#email").value.trim();
      const password = $("#password").value;

      if (!email) { setError(loginForm, "email", "Escribe tu email."); ok = false; }
      else if (!emailRe.test(email)) { setError(loginForm, "email", "El email no es válido."); ok = false; }
      else setError(loginForm, "email", "");

      if (!password) { setError(loginForm, "password", "Escribe tu contraseña."); ok = false; }
      else setError(loginForm, "password", "");

      if (!ok) return;

      const btn = loginForm.querySelector('button[type="submit"]');
      busy(btn, true, "Entrando...");
      try {
        const user = await Aurea.login(email, password);
        UI.toast("¡Hola de nuevo, " + user.name.split(" ")[0] + "!", "success");
        const data = Aurea.getUserData(user.id);
        setTimeout(() => {
          location.href = data.onboardingComplete ? "app.html" : "onboarding.html";
        }, 500);
      } catch (err) {
        busy(btn, false);
        showAlert(alertEl, err.message || "No se pudo iniciar sesión.", "error");
      }
    });
  }

  /* =========================================================
     REGISTRO
     ========================================================= */
  const registerForm = $("#registerForm");
  if (registerForm) {
    const alertEl = $("#alert");
    const pwInput = $("#password");
    const strengthBar = $("#strengthBar");

    function scorePassword(v) {
      let s = 0;
      if (v.length >= 8) s++;
      if (/[A-Z]/.test(v)) s++;
      if (/[0-9]/.test(v)) s++;
      if (/[^A-Za-z0-9]/.test(v)) s++;
      return Math.min(s, 4);
    }
    pwInput.addEventListener("input", () => {
      const s = scorePassword(pwInput.value);
      const pct = [0, 25, 50, 75, 100][s];
      const colors = ["transparent", "#ef4444", "#f59e0b", "#eab308", "#16a34a"];
      strengthBar.style.width = pct + "%";
      strengthBar.style.background = colors[s];
    });

    registerForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      hideAlert(alertEl);
      let ok = true;

      const name = $("#name").value.trim();
      const email = $("#email").value.trim();
      const password = $("#password").value;
      const confirm = $("#confirm").value;
      const terms = $("#terms").checked;

      if (!name) { setError(registerForm, "name", "Escribe tu nombre."); ok = false; }
      else if (name.length < 2) { setError(registerForm, "name", "El nombre es demasiado corto."); ok = false; }
      else setError(registerForm, "name", "");

      if (!email) { setError(registerForm, "email", "Escribe tu email."); ok = false; }
      else if (!emailRe.test(email)) { setError(registerForm, "email", "El email no es válido."); ok = false; }
      else if (Aurea.findUserByEmail(email)) { setError(registerForm, "email", "Ese email ya está registrado."); ok = false; }
      else setError(registerForm, "email", "");

      if (!password) { setError(registerForm, "password", "Escribe una contraseña."); ok = false; }
      else if (password.length < 8) { setError(registerForm, "password", "Debe tener al menos 8 caracteres."); ok = false; }
      else setError(registerForm, "password", "");

      if (!confirm) { setError(registerForm, "confirm", "Repite la contraseña."); ok = false; }
      else if (confirm !== password) { setError(registerForm, "confirm", "Las contraseñas no coinciden."); ok = false; }
      else setError(registerForm, "confirm", "");

      if (!terms) { setError(registerForm, "terms", "Debes aceptar los términos."); ok = false; }
      else setError(registerForm, "terms", "");

      if (!ok) return;

      const btn = registerForm.querySelector('button[type="submit"]');
      busy(btn, true, "Creando cuenta...");
      try {
        await Aurea.register({ name, email, password });
        UI.toast("Cuenta creada. ¡Vamos a configurarla!", "success");
        setTimeout(() => { location.href = "onboarding.html"; }, 600);
      } catch (err) {
        busy(btn, false);
        showAlert(alertEl, err.message || "No se pudo crear la cuenta.", "error");
      }
    });
  }
})();
