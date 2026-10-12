# Aurea — Sitio web + App de cuentas

Sitio web profesional con **área privada funcional**, hecho con **HTML, CSS y JavaScript puro**
(sin frameworks ni dependencias). Incluye registro, inicio de sesión, asistente de
configuración inicial y un panel completo.

---

## 🚀 Cómo arrancarlo (recomendado)

Necesitas Node.js instalado (ya lo tienes). Desde esta carpeta:

```bash
npm start
```

Luego abre en el navegador: **http://localhost:4321**

> También puedes usar `python3 -m http.server 4321` si lo prefieres.

¿Por qué un servidor? Porque las páginas comparten la sesión mediante `localStorage`,
y algunos navegadores lo bloquean o aíslan al abrir los archivos con doble clic (`file://`).
Con `http://localhost` todo funciona igual en Chrome, Safari, Firefox y Edge.

---

## ✨ Qué puedes hacer

1. **Landing pública** (`index.html`) — página de presentación con secciones,
   animaciones y llamadas a la acción.
2. **Crear cuenta** (`register.html`) — validación, medidor de fuerza de contraseña.
3. **Asistente inicial** (`onboarding.html`) — 3 pasos: negocio, marca y objetivo.
   Personaliza el color y el tema en vivo.
4. **Panel** (`app.html`) — protegido. Incluye:
   - **Resumen**: métricas, gráfico semanal y tareas pendientes.
   - **Tareas**: añadir, completar y borrar (con barra de progreso).
   - **Perfil**: editar nombre, cargo, empresa y descripción.
   - **Ajustes**: color de marca, tema, sector, objetivo y notificaciones.
5. **Iniciar / cerrar sesión** (`login.html`) — con mensajes de error claros.

---

## 📁 Estructura

```
.
├── index.html          # Portada pública
├── login.html          # Iniciar sesión
├── register.html       # Crear cuenta
├── onboarding.html     # Asistente de configuración
├── app.html            # Panel privado
│
├── styles.css          # Estilos de la portada
├── script.js           # Interactividad de la portada
├── auth.js             # Lógica de login y registro
├── onboarding.js       # Lógica del asistente
├── dashboard.js        # Lógica del panel
│
├── assets/
│   ├── app.css         # Estilos de la app (auth + panel)
│   ├── store.js        # Capa de datos (usuarios, sesión, ajustes, tareas)
│   └── ui.js           # Utilidades (toasts, tema, iconos)
│
├── server.js           # Servidor estático mínimo
└── package.json
```

---

## 🔐 Cómo se guardan los datos

Todo se guarda en el **`localStorage`** del navegador:

- `aurea:users` — lista de usuarios (email + hash de contraseña + salt).
- `aurea:session` — quién ha iniciado sesión.
- `aurea:data:<id>` — ajustes, perfil, tareas y estadísticas de cada usuario.

Las contraseñas se derivan con **SHA-256 + salt** (vía Web Crypto) cuando el contexto es
seguro, o con un hash simple como respaldo.

> ⚠️ **Aviso importante:** esto es una **demo**, ideal para prototipos y prácticas.
> No la uses en producción tal cual: `localStorage` no es seguro para datos reales ni
> contraseñas. Para un uso real se necesita un **servidor con base de datos** y
> autenticación del lado del servidor.

---

## 🎨 Personalización rápida

- **Colores y tema:** se cambian desde *Ajustes* dentro del panel.
- **Color base del sitio público:** variable `--accent` en `styles.css`.
- **Textos:** edítalos directamente en los archivos `.html`.
- **Marca:** busca `Aurea` y sustitúyelo por tu nombre.

---

## 🔄 Para convertirlo en una app real (siguiente paso)

Si más adelante quieres cuentas de verdad (multiusuario, seguras y en la nube), habría
que añadir un **backend** (por ejemplo Node.js + Express + base de datos) y cambiar
`assets/store.js` para que hable con una API en lugar de con `localStorage`.
Avísame y lo montamos.
