#!/bin/bash
set -e

echo "=== Despliegue automático Sonora en Oracle Cloud / Ubuntu ==="

# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar dependencias
sudo apt install -y curl wget git ffmpeg nodejs npm unzip

# Instalar yt-dlp
sudo curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
sudo chmod +x /usr/local/bin/yt-dlp
yt-dlp --version

# Instalar pm2 para mantener servidor activo
sudo npm install -g pm2

# Clonar repo
cd /home/ubuntu
if [ ! -d "sonora-app" ]; then
  git clone https://github.com/raykozabala/sonora-app.git
fi
cd sonora-app/servidor

# Instalar dependencias servidor
npm install

# Iniciar con pm2
pm2 start server.js --name sonora-server
pm2 save
pm2 startup

echo "=== ¡LISTO! ==="
echo "Servidor corriendo con pm2"
echo "Comprobar estado: pm2 status"
echo "Logs: pm2 logs sonora-server"
echo "IP pública: $(curl -s https://api.ipify.org)"
