# Soluciones MÁS FÁCILES (poca intervención)

| Servicio | Gratis | 24/7 | Fácil | yt-dlp+ffmpeg | URL directa | Notas |
|---|---|---|---|---|---|---|
| [Koyeb](https://app.koyeb.com/) | Sí (créditos gratis) | Sí | ★★★★★ | Sí con Docker | Te da https://xxx.koyeb.app al instante | Login con GitHub, 1 click deploy. Muy automatizable. |
| [Render](https://dashboard.render.com/) | Sí | Sí* (hiberna 15min inactivo) | ★★★★★ | Sí | https://xxx.onrender.com | Muy fácil. Al recibir 1ª petición despierta (30s) |
| [Northflank](https://northflank.com/) | Sí (free tier) | Sí | ★★★★★ | Sí | Te da URL | Interfaz clara, parecido a Railway |
| [PikaPage](https://www.pikapage.com/) | Hay alternativas | - | - | - | - | Menos común |
| [Porter.run](https://porter.run/) | Tiene free | - | ★★★★☆ | Sí | Sí | Bueno pero menos conocido |
| [Deno Deploy](https://deno.com/deploy) | NO apto (Node+yt-dlp binario) | - | - | NO | - | No funciona con yt-dlp nativo |
| [Zeabur](https://zeabur.com/) | Free tier | Sí | ★★★★★ | Sí | Sí | Muy sencillo, UI moderna |

# Más "1-click" aún: Gitpod/Codespaces NO 24/7

# Recomendado AHORA MISMO: Koyeb

Koyeb permite:
- Conectar repo GitHub → 1 click deploy
- Detecta Dockerfile automáticamente
- Te da URL pública al instante
- No pide tarjeta para empezar (suele ser suficiente con login)
- Menos clicks que Oracle Cloud

# Render vs Koyeb
- Render: más conocido, duerme tras inactividad
- Koyeb: menos hibernación en free tier, muy directo

Si quieres MÁXIMA automatización: Koyeb. Yo te preparo repo (ya casi listo), tú solo das permisos a Koyeb y le das "Deploy". Eso es TODO.

También Render: tan fácil como conectar repo → root directory servidor → auto-deploy.
