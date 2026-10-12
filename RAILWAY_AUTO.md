# Despliegue automático Sonora (Railway) - Sin complicaciones

Railway es la opción más fácil y rápida para tener el servidor 24/7 gratis.

## Opción 1: Conectar desde Railway (recomendada - 1 click)

1. Ve a https://railway.app/
2. Crea cuenta (GitHub/Gmail)
3. Pulsa "New Project" → "Deploy from GitHub repo"
4. Conecta GitHub y selecciona el repo `sonora-app` (o el que creaste)
5. Pulsa el proyecto → "Settings" → "Root Directory" → pon `servidor`
6. Guarda. Railway compilará con Dockerfile automáticamente y levantará `node server.js`
7. Pulsa "Deployments" → espera ~1-2 minutos
8. Pulsa "Generate Domain" (Settings > Networking) → obtendrás URL tipo `https://sonora-server-xxxx.up.railway.app`
9. Copia esa URL y pónla en Ajustes de la app Sonora

## Opción 2: Railway CLI (rápido)

Instala CLI:
```bash
npm i -g @railway/cli
railway login
railway init
railway up --service=sonora
```

## Notas

- Gratis: suficiente para búsquedas/descargas personales
- Dockerfile ya está preparado (instala yt-dlp + ffmpeg)
- El servidor escucha en $PORT (Railway lo asigna automáticamente)
