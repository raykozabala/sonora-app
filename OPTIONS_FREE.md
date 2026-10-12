# Opciones gratuitas SIN límite de 30 días (para siempre)

## Comparativa real

| Servicio | Gratis para siempre | yt-dlp+ffmpeg | HTTPS | URL fija | Dormido tras inactividad | Fácil | Notas |
|---|---|---|---|---|---|---|---|
| [Oracle Cloud Always Free](https://www.oracle.com/cloud/free/) | SÍ | Sí (instalas tú) | Sí (con dominio o túnel) | Sí (IP pública fija) | NO (VM siempre encendida) | Medio | 2 VMs ARM gratis + 24GB RAM + 200GB. Mejor relación calidad/gratuito. |
| [Render](https://render.com/) | SÍ (Free tier) | Sí | Sí | Sí | SÍ (se duerme tras 15 min sin tráfico) - primera petición tarda 30-60s | Muy fácil | Bueno, pero se duerme. Al despertar yt-dlp funciona pero hay latencia. |
| [Koyeb](https://www.koyeb.com/) | SÍ (Free tier) | Sí con Docker | Sí | Sí (con custom) | Depende (Free Apps pueden hibernar) | Muy fácil | Similar a Render/Railway. |
| [Netlify Functions](https://www.netlify.com/products/functions/) | SÍ | NO (limita binarios, yt-dlp pesado) | Sí | Sí | NO ideal | Fácil | No apto para yt-dlp/ffmpeg largos. |
| [Vercel](https://vercel.com/) | SÍ | NO (Serverless limita CPU/time) | Sí | Sí | Límite estricto | Fácil | No apto para descargas largas. |
| [OnRender](https://render.com/) | SÍ | Sí | Sí | Sí | Sí | Fácil | Ver Render |
| [Gitpod](https://www.gitpod.io/) | Límite horas gratis | Sí | Sí | Temporal | Se apaga | Fácil | No 24/7 real |
| [Oracle Cloud ARM](https://www.oracle.com/cloud/free/) | SÍ 100% | Sí | Sí | IP fija | SIEMPRE ENCENDIDO | Un poco más setup | RECOMENDADO PARA "PARA SIEMPRE" |

## Mi recomendación directa

Si quieres que **dure toda la vida y sea 24/7 sin límite de 30 días**, ve por **Oracle Cloud Always Free**.

¿Por qué?
- Gratis para siempre (no caduca tras 30 días)
- VM ARM con recursos suficientes (yt-dlp + ffmpeg van bien)
- IP pública fija
- Sin "trial" que se acaba
- Puedes poner dominio propio con HTTPS (Caddy/NGINX)

## Plan automático (yo hago TODO por ti)

Te lo automatizo al máximo:
1. Te dejo instrucciones para crear cuenta Oracle Cloud (solo 1 tarjeta de crédito/debito para verificación, **nunca cobra** en Always Free)
2. Te genero un script para conectar por SSH y desplegar automáticamente (instala Node, yt-dlp, ffmpeg, pm2, clona repo, levanta servidor)
3. Te digo IP pública → la pones en app
4. También subo el repo a GitHub (ya lo tienes link) y lo dejo listo

¿Prefieres Oracle Cloud (para siempre) o seguimos con Koyeb/Render (más fácil pero con "hibernación" tras inactividad)?
