# Despliegue del servidor Sonora a la nube

El servidor necesita yt-dlp + ffmpeg. Puedes desplegarlo en Railway, Render, Fly.io u Oracle Cloud.

## 1) Railway (recomendado - más fácil y rápido)

1. Sube todo el repositorio a GitHub (o usa Railway desde CLI)
2. En railway.app → New Project → Deploy from GitHub repo
3. Selecciona este repositorio
4. En "Root Directory" pon: `servidor`
5. Railway detectará automáticamente Node y usará `npm start`
6. Añade variable de entorno si quieres: `NODE_ENV=production`
7. Una vez desplegado, copia la URL pública (ej. https://sonora-server.up.railway.app)

Nota: Railway usa HTTP/HTTPS público. Usa HTTPS en la app si te da HTTPS.

## 2) Render

1. render.com → New → Web Service → Connect repo
2. Root Directory: `servidor`
3. Build Command: `npm install`
4. Start Command: `node server.js`
5. El Dockerfile también funciona si prefieres Docker
6. Copia la URL pública

## 3) Fly.io

```bash
cd servidor
flyctl launch
# sigue instrucciones
flyctl deploy
```

## 4) Oracle Cloud Always Free

Necesitarás una VM Ubuntu. Dentro:
```bash
sudo apt update && sudo apt install -y ffmpeg curl nodejs npm
sudo curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
sudo chmod +x /usr/local/bin/yt-dlp
git clone <tu-repo>
cd <repo>/servidor
npm install
npm start
# usar pm2 para mantener activo: pm2 start server.js --name sonora
```

# Configurar URL en la app

1. Abre Sonora → Ajustes (pronto añadiré interfaz) o edita el valor por defecto
2. Cambia `ApiClient.base` / Store con la URL pública de tu servidor desplegado

Con esto funcionará con datos móviles y sin tener el Mac encendido.
