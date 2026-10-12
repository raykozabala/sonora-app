/* =========================================================
   Aurea — panel privado (dashboard)
   ========================================================= */
(function () {
  "use strict";

  const $ = (s, c = document) => c.querySelector(s);
  const $$ = (s, c = document) => Array.from(c.querySelectorAll(s));

  UI.initTheme();

  /* ---------- Sesión obligatoria ---------- */
  let user = Aurea.currentUser();
  if (!user) { location.replace("login.html"); return; }

  let data = Aurea.getUserData(user.id);
  if (!data.onboardingComplete) { location.replace("onboarding.html"); return; }

  /* ---------- Aplicar marca del usuario ---------- */
  function applyBrand() {
    const root = document.documentElement;
    root.setAttribute("data-theme", data.settings.theme === "dark" ? "dark" : "light");
    root.style.setProperty("--accent", data.settings.brandColor || "#4f46e5");
    root.style.setProperty("--accent-soft", hexToSoft(data.settings.brandColor || "#4f46e5"));
  }
  function hexToSoft(hex) {
    const h = hex.replace("#", "");
    const r = parseInt(h.substring(0, 2), 16);
    const g = parseInt(h.substring(2, 4), 16);
    const b = parseInt(h.substring(4, 6), 16);
    return "rgba(" + r + "," + g + "," + b + ",0.12)";
  }
  applyBrand();
  UI.bindThemeToggle($("#themeToggle"));

  /* ---------- Contar visita ---------- */
  data.stats.visits = (data.stats.visits || 0) + 1;
  Aurea.saveUserData(user.id, data);

  const initials = (name) =>
    String(name || "?").trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join("").toUpperCase();

  /* ---------- Datos del usuario en la interfaz ---------- */
  function paintUser() {
    const name = user.name || "Usuario";
    const company = data.profile.company || "Sin empresa";
    $("#sideName").textContent = name;
    $("#sideCompany").textContent = company;
    $("#sideAvatar").textContent = initials(name);
    $("#bigAvatar").textContent = initials(name);
    $("#cardName").textContent = name;
    $("#cardRole").textContent = data.profile.role || "Sin cargo";
    $("#cardCompany").textContent = data.profile.company || "Sin empresa";
    $("#cardBio").textContent = data.profile.bio || "Aún no has escrito una descripción.";
    const since = new Date(user.createdAt || Date.now());
    $("#memberSince").textContent = "Miembro desde " + since.toLocaleDateString("es-ES", { month: "long", year: "numeric" });
  }
  paintUser();

  /* =========================================================
     Navegación entre vistas
     ========================================================= */
  const TITLES = {
    resumen: ["Resumen", "Tu actividad de un vistazo"],
    tareas: ["Tareas", "Organiza y completa tu trabajo"],
    perfil: ["Perfil", "Tu información personal"],
    ajustes: ["Ajustes", "Personaliza tu experiencia"],
  };

  function goTo(view) {
    $$(".view").forEach((v) => v.classList.toggle("active", v.dataset.view === view));
    $$(".side-link").forEach((l) => l.classList.toggle("active", l.dataset.view === view));
    const t = TITLES[view] || TITLES.resumen;
    $("#pageTitle").textContent = t[0];
    $("#pageSub").textContent = t[1];
    closeSidebar();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  $$(".side-link").forEach((link) =>
    link.addEventListener("click", () => goTo(link.dataset.view)));
  $$("[data-goto]").forEach((btn) =>
    btn.addEventListener("click", () => goTo(btn.dataset.goto)));

  /* =========================================================
     Menú móvil
     ========================================================= */
  const sidebar = $("#sidebar");
  const scrim = $("#scrim");
  function closeSidebar() { sidebar.classList.remove("open"); scrim.classList.remove("show"); }
  $("#menuBtn").addEventListener("click", () => {
    sidebar.classList.toggle("open");
    scrim.classList.toggle("show");
  });
  scrim.addEventListener("click", closeSidebar);

  /* =========================================================
     Tareas
     ========================================================= */
  function getTasks() { return data.tasks || []; }

  function saveTasks(tasks) {
    data.tasks = tasks;
    Aurea.saveUserData(user.id, data);
  }

  function taskHTML(t) {
    return (
      '<div class="task' + (t.done ? " done" : "") + '" data-id="' + t.id + '">' +
        '<button class="task-check" type="button" data-action="toggle" aria-label="Marcar tarea">✓</button>' +
        '<span class="task-text"></span>' +
        '<button class="task-del" type="button" data-action="delete" aria-label="Eliminar tarea">✕</button>' +
      "</div>"
    );
  }

  function renderTasks() {
    const tasks = getTasks();
    const list = $("#taskList");
    const recent = $("#recentTasks");

    // Lista completa
    if (tasks.length === 0) {
      list.innerHTML = '<div class="empty">Todavía no tienes tareas. ¡Añade la primera arriba!</div>';
    } else {
      list.innerHTML = tasks.map(taskHTML).join("");
      list.querySelectorAll(".task").forEach((el, i) => {
        el.querySelector(".task-text").textContent = tasks[i].text;
      });
    }

    // Pendientes (resumen)
    const pending = tasks.filter((t) => !t.done).slice(0, 4);
    if (pending.length === 0) {
      recent.innerHTML = '<div class="empty">🎉 ¡No tienes tareas pendientes!</div>';
    } else {
      recent.innerHTML = pending.map(taskHTML).join("");
      recent.querySelectorAll(".task").forEach((el, i) => {
        el.querySelector(".task-text").textContent = pending[i].text;
      });
    }

    // Delegación de eventos
    [list, recent].forEach((host) => {
      host.querySelectorAll(".task").forEach((el) => {
        const id = el.dataset.id;
        el.querySelector('[data-action="toggle"]').addEventListener("click", () => {
          saveTasks(Aurea.toggleTask(user.id, id));
          renderTasks(); renderStats(); renderChart();
        });
        el.querySelector('[data-action="delete"]').addEventListener("click", () => {
          saveTasks(Aurea.deleteTask(user.id, id));
          renderTasks(); renderStats(); renderChart();
          UI.toast("Tarea eliminada", "info");
        });
      });
    });

    renderStats();
  }

  /* Añadir tarea */
  const taskForm = $("#taskForm");
  taskForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const input = $("#taskInput");
    const text = input.value.trim();
    const err = $("#taskError");
    if (text.length < 2) { err.textContent = "Escribe algo un poco más largo."; return; }
    err.textContent = "";
    const updated = Aurea.addTask(user.id, text);
    data.tasks = updated;
    input.value = "";
    renderTasks(); renderChart();
    UI.toast("Tarea añadida", "success");
  });

  /* Borrar completadas */
  $("#clearDone").addEventListener("click", () => {
    const remaining = getTasks().filter((t) => !t.done);
    if (remaining.length === getTasks().length) { UI.toast("No hay tareas completadas", "info"); return; }
    saveTasks(remaining);
    renderTasks(); renderStats(); renderChart();
    UI.toast("Tareas completadas eliminadas", "info");
  });

  /* =========================================================
     Estadísticas y gráfico
     ========================================================= */
  function renderStats() {
    const tasks = getTasks();
    const done = tasks.filter((t) => t.done).length;
    const active = tasks.length - done;
    const pct = tasks.length ? Math.round((done / tasks.length) * 100) : 0;

    $("#statActive").textContent = active;
    $("#statDone").textContent = done;
    $("#statDoneDelta").textContent = pct + "% del total";
    $("#statVisits").textContent = data.stats.visits || 0;
    $("#statLogins").textContent = data.stats.logins || 0;

    $("#tasksSummary").textContent = done + " de " + tasks.length + " completadas";
    $("#tasksBar").style.width = pct + "%";

    $("#goalPct").textContent = pct + "%";
    $("#goalBar").style.width = pct + "%";
  }

  const GOALS = {
    vender: "Vender más — aumentar conversiones",
    crecer: "Crecer — más tráfico y alcance",
    organizar: "Organizarme — orden y productividad",
    lanzar: "Lanzar — sacar algo nuevo al mercado",
  };
  $("#goalLabel").textContent = GOALS[data.settings.goal] || GOALS.crecer;

  function renderChart() {
    const host = $("#chart");
    const days = [];
    const today = new Date();
    for (let i = 6; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(today.getDate() - i);
      days.push(d);
    }
    const tasks = getTasks();
    const counts = days.map((d) => {
      const key = d.toDateString();
      return tasks.filter((t) => new Date(t.createdAt).toDateString() === key).length;
    });
    const max = Math.max(1, ...counts);
    host.innerHTML = days.map((d, i) => {
      const h = Math.round((counts[i] / max) * 100);
      const label = d.toLocaleDateString("es-ES", { weekday: "short" });
      const hi = counts[i] === max && max > 0 ? " hi" : "";
      return (
        '<div class="bar-col' + hi + '">' +
          '<div class="bar" style="height:' + Math.max(h, 6) + '%" title="' + counts[i] + ' tareas"></div>' +
          '<small>' + label + "</small>" +
        "</div>"
      );
    }).join("");
    // Subtítulo del gráfico
    const sub = host.closest(".card").querySelector(".card-sub");
    if (sub) sub.textContent = "Tareas creadas por día";
  }

  /* =========================================================
     Perfil
     ========================================================= */
  const profileForm = $("#profileForm");
  function paintProfile() {
    $("#pName").value = user.name || "";
    $("#pEmail").value = user.email || "";
    $("#pRole").value = data.profile.role || "";
    $("#pCompany").value = data.profile.company || "";
    $("#pBio").value = data.profile.bio || "";
  }
  paintProfile();

  profileForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const name = $("#pName").value.trim();
    if (name.length < 2) { UI.toast("El nombre es demasiado corto", "error"); return; }
    user = Aurea.updateAccount(user.id, { name });
    Aurea.updateProfile(user.id, {
      role: $("#pRole").value.trim(),
      company: $("#pCompany").value.trim(),
      bio: $("#pBio").value.trim(),
    });
    data = Aurea.getUserData(user.id);
    paintUser();
    UI.toast("Perfil actualizado", "success");
  });

  /* =========================================================
     Ajustes
     ========================================================= */
  function paintSettings() {
    // Swatches
    const color = data.settings.brandColor;
    let matched = false;
    $$("#settingsSwatches .swatch").forEach((sw) => {
      const on = sw.dataset.color === color;
      sw.classList.toggle("selected", on);
      if (on) matched = true;
    });
    if (!matched) $("#settingsCustomColor").value = color;

    // Tema
    $$("[data-s-theme]").forEach((c) =>
      c.classList.toggle("selected", c.dataset.sTheme === data.settings.theme));

    // Sector / objetivo / notificaciones
    $("#sSector").value = data.settings.sector || "";
    $$("[data-s-goal]").forEach((c) =>
      c.classList.toggle("selected", c.dataset.sGoal === data.settings.goal));
    $("#sNotifications").checked = data.settings.notifications !== false;
  }
  paintSettings();

  function saveSettings(patch) {
    Aurea.updateSettings(user.id, patch);
    data = Aurea.getUserData(user.id);
    applyBrand();
  }

  $$("#settingsSwatches .swatch").forEach((sw) => {
    if (!sw.dataset.color) return;
    sw.addEventListener("click", () => {
      saveSettings({ brandColor: sw.dataset.color });
      paintSettings();
      UI.toast("Color actualizado", "success");
    });
  });
  $("#settingsCustomColor").addEventListener("input", (e) => {
    saveSettings({ brandColor: e.target.value });
    $$("#settingsSwatches .swatch").forEach((s) => s.classList.remove("selected"));
  });

  $$("[data-s-theme]").forEach((choice) => {
    choice.addEventListener("click", () => {
      saveSettings({ theme: choice.dataset.sTheme });
      paintSettings();
    });
  });

  $("#sSector").addEventListener("change", (e) => {
    saveSettings({ sector: e.target.value });
    UI.toast("Sector guardado", "success");
  });

  $$("[data-s-goal]").forEach((choice) => {
    choice.addEventListener("click", () => {
      saveSettings({ goal: choice.dataset.sGoal });
      $("#goalLabel").textContent = GOALS[choice.dataset.sGoal] || GOALS.crecer;
      paintSettings();
      UI.toast("Objetivo actualizado", "success");
    });
  });

  $("#sNotifications").addEventListener("change", (e) => {
    saveSettings({ notifications: e.target.checked });
    UI.toast(e.target.checked ? "Notificaciones activadas" : "Notificaciones desactivadas", "info");
  });

  /* =========================================================
     Cuenta
     ========================================================= */
  function doLogout() {
    Aurea.logout();
    location.href = "login.html";
  }
  $("#logoutBtn").addEventListener("click", doLogout);
  $("#logoutBtn2").addEventListener("click", doLogout);

  $("#deleteAccount").addEventListener("click", () => {
    const sure = window.confirm("¿Seguro que quieres eliminar tu cuenta? Esta acción no se puede deshacer.");
    if (!sure) return;
    Aurea.deleteAccount(user.id);
    location.href = "index.html";
  });

  /* ---------- Render inicial ---------- */
  renderTasks();
  renderChart();
})();
