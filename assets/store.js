/* =========================================================
   Aurea — capa de datos y autenticación (solo navegador)
   Guarda usuarios, sesión, ajustes y tareas en localStorage.
   NOTA: es una demo. Las contraseñas se derivan con hash,
   pero localStorage no es un almacén seguro para producción.
   ========================================================= */
(function () {
  "use strict";

  const KEYS = {
    users: "aurea:users",
    session: "aurea:session",
    data: (id) => "aurea:data:" + id,
  };

  /* ---------- Utilidades de almacenamiento ---------- */
  // Fallback en memoria si localStorage no está disponible (p. ej. file://)
  const memory = new Map();
  const store = {
    get(key) {
      try {
        const v = localStorage.getItem(key);
        return v === null ? null : v;
      } catch (_) {
        return memory.has(key) ? memory.get(key) : null;
      }
    },
    set(key, value) {
      try {
        localStorage.setItem(key, value);
      } catch (_) {
        memory.set(key, value);
      }
    },
    remove(key) {
      try { localStorage.removeItem(key); } catch (_) { memory.delete(key); }
    },
  };

  const read = (key, fallback) => {
    const raw = store.get(key);
    if (!raw) return fallback;
    try { return JSON.parse(raw); } catch (_) { return fallback; }
  };
  const write = (key, value) => store.set(key, JSON.stringify(value));

  const uid = () =>
    Date.now().toString(36) + Math.random().toString(36).slice(2, 8);

  /* ---------- Hash de contraseña ---------- */
  // Usa SHA-256 (Web Crypto) cuando hay contexto seguro; si no, un hash simple.
  function fallbackHash(str) {
    let h = 0x811c9dc5;
    for (let i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 0x01000193);
    }
    return "fv" + (h >>> 0).toString(16);
  }

  async function hashPassword(password, salt) {
    const input = salt + "::" + password;
    if (window.crypto && window.crypto.subtle && window.isSecureContext) {
      try {
        const buf = new TextEncoder().encode(input);
        const digest = await window.crypto.subtle.digest("SHA-256", buf);
        return Array.from(new Uint8Array(digest))
          .map((b) => b.toString(16).padStart(2, "0"))
          .join("");
      } catch (_) { /* cae al fallback */ }
    }
    return fallbackHash(input);
  }

  /* ---------- Usuarios ---------- */
  function getUsers() { return read(KEYS.users, []); }
  function saveUsers(users) { write(KEYS.users, users); }
  function findUserByEmail(email) {
    const needle = String(email || "").trim().toLowerCase();
    return getUsers().find((u) => u.email === needle) || null;
  }
  function findUserById(id) { return getUsers().find((u) => u.id === id) || null; }

  async function register({ name, email, password }) {
    const cleanEmail = String(email || "").trim().toLowerCase();
    if (!name || !cleanEmail || !password) {
      throw new Error("Faltan datos obligatorios.");
    }
    if (findUserByEmail(cleanEmail)) {
      throw new Error("Ya existe una cuenta con ese email.");
    }
    const salt = uid();
    const passwordHash = await hashPassword(password, salt);
    const user = {
      id: uid(),
      name: String(name).trim(),
      email: cleanEmail,
      salt,
      passwordHash,
      createdAt: new Date().toISOString(),
    };
    const users = getUsers();
    users.push(user);
    saveUsers(users);

    // Datos iniciales del usuario
    write(KEYS.data(user.id), {
      onboardingComplete: false,
      profile: { role: "", company: "", bio: "" },
      settings: {
        theme: "light",
        brandColor: "#4f46e5",
        sector: "",
        goal: "crecer",
        notifications: true,
        density: "comfortable",
      },
      tasks: [],
      stats: { visits: 0, logins: 0 },
    });

    setSession(user.id);
    return user;
  }

  async function login(email, password) {
    const user = findUserByEmail(email);
    if (!user) throw new Error("No existe una cuenta con ese email.");
    const hash = await hashPassword(password, user.salt);
    if (hash !== user.passwordHash) throw new Error("La contraseña no es correcta.");
    setSession(user.id);
    const data = getUserData(user.id);
    data.stats.logins = (data.stats.logins || 0) + 1;
    data.stats.lastLogin = new Date().toISOString();
    saveUserData(user.id, data);
    return user;
  }

  /* ---------- Sesión ---------- */
  function setSession(userId) { write(KEYS.session, { userId }); }
  function clearSession() { store.remove(KEYS.session); }
  function currentUser() {
    const s = read(KEYS.session, null);
    if (!s || !s.userId) return null;
    return findUserById(s.userId);
  }
  function logout() { clearSession(); }

  /* ---------- Datos por usuario ---------- */
  function getUserData(userId) {
    return read(KEYS.data(userId), {
      onboardingComplete: false,
      profile: { role: "", company: "", bio: "" },
      settings: {
        theme: "light",
        brandColor: "#4f46e5",
        sector: "",
        goal: "crecer",
        notifications: true,
        density: "comfortable",
      },
      tasks: [],
      stats: { visits: 0, logins: 0 },
    });
  }
  function saveUserData(userId, data) { write(KEYS.data(userId), data); }

  function updateSettings(userId, patch) {
    const data = getUserData(userId);
    data.settings = Object.assign({}, data.settings, patch);
    saveUserData(userId, data);
    return data.settings;
  }
  function updateProfile(userId, patch) {
    const data = getUserData(userId);
    data.profile = Object.assign({}, data.profile, patch);
    saveUserData(userId, data);
    return data.profile;
  }
  function completeOnboarding(userId) {
    const data = getUserData(userId);
    data.onboardingComplete = true;
    saveUserData(userId, data);
  }

  /* ---------- Tareas ---------- */
  function getTasks(userId) { return getUserData(userId).tasks || []; }
  function addTask(userId, text) {
    const data = getUserData(userId);
    const task = {
      id: uid(),
      text: String(text).trim(),
      done: false,
      createdAt: new Date().toISOString(),
    };
    data.tasks.unshift(task);
    saveUserData(userId, data);
    return data.tasks;
  }
  function toggleTask(userId, taskId) {
    const data = getUserData(userId);
    const t = data.tasks.find((x) => x.id === taskId);
    if (t) { t.done = !t.done; saveUserData(userId, data); }
    return data.tasks;
  }
  function deleteTask(userId, taskId) {
    const data = getUserData(userId);
    data.tasks = data.tasks.filter((x) => x.id !== taskId);
    saveUserData(userId, data);
    return data.tasks;
  }

  /* ---------- Cuenta ---------- */
  function updateAccount(userId, patch) {
    const users = getUsers();
    const u = users.find((x) => x.id === userId);
    if (!u) return null;
    // No permitir cambiar el email ni el hash por esta vía
    delete patch.email; delete patch.passwordHash; delete patch.salt;
    Object.assign(u, patch);
    saveUsers(users);
    return u;
  }
  function deleteAccount(userId) {
    saveUsers(getUsers().filter((u) => u.id !== userId));
    store.remove(KEYS.data(userId));
    clearSession();
  }

  /* ---------- Tema y marca ---------- */
  function applyBrand(settings) {
    const root = document.documentElement;
    const theme = settings && settings.theme === "dark" ? "dark" : "light";
    root.setAttribute("data-theme", theme);
    const color = (settings && settings.brandColor) || "#4f46e5";
    root.style.setProperty("--accent", color);
  }

  window.Aurea = {
    register, login, logout, currentUser,
    getUserData, saveUserData, updateSettings, updateProfile, completeOnboarding,
    getTasks, addTask, toggleTask, deleteTask,
    updateAccount, deleteAccount,
    applyBrand, findUserByEmail,
    _keys: KEYS,
  };
})();
