/* =========================================================
   Servidor de descargas — Sonora
   ---------------------------------------------------------
   Usa yt-dlp (con ffmpeg) bajo demanda para obtener información
   de un vídeo y descargarlo en MP3 o MP4.

   NO requiere dependencias externas: solo módulos de Node.

   Arranque:   node server.js
   Escucha en: http://<tu-ip>:8080

   ⚠️ Uso personal con contenido del que tengas derechos
   (tuyos, con licencia CC, público dominio, etc.).
   ========================================================= */
"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");
const os = require("os");
const { spawn } = require("child_process");

const PORT = process.env.PORT || 8080;
const OUT_DIR = path.join(__dirname, "descargas");
fs.mkdirSync(OUT_DIR, { recursive: true });

// Resuelve la ruta absoluta de un binario. Devuelve null si no existe.
function resolveBin(nombre) {
  if (process.env[nombre.toUpperCase()]) return process.env[nombre.toUpperCase()];
  try {
    const out = require("child_process")
      .execSync("command -v " + nombre, { stdio: ["ignore", "pipe", "ignore"] })
      .toString()
      .trim();
    return out || null;
  } catch (_) {
    return null;
  }
}

const YTDLP = resolveBin("yt-dlp") || "yt-dlp";
const FFMPEG = resolveBin("ffmpeg"); // puede ser null; yt-dlp usará el que encuentre

/* ---------- Trabajos en curso ---------- */
const jobs = new Map();
let jobCounter = 0;

/* ---------- Utilidades ---------- */
function run(cmd, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args);
    let out = "";
    let err = "";
    child.stdout.on("data", (d) => (out += d.toString()));
    child.stderr.on("data", (d) => (err += d.toString()));
    child.on("error", (e) => reject(new Error("No se pudo ejecutar " + cmd + ": " + e.message)));
    child.on("close", (code) => {
      if (code === 0) resolve(out);
      else reject(new Error(err.slice(0, 800) || ("El proceso salió con código " + code)));
    });
  });
}

function isValidUrl(u) {
  try {
    const p = new URL(u);
    return p.protocol === "http:" || p.protocol === "https:";
  } catch (_) {
    return false;
  }
}

// Lee un parámetro de la URL de forma segura.
function q_get(u, nombre) {
  return u.searchParams.get(nombre);
}

function sendJSON(res, code, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Cache-Control": "no-store",
  });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve) => {
    let data = "";
    req.on("data", (c) => (data += c));
    req.on("end", () => {
      try { resolve(JSON.parse(data || "{}")); } catch (_) { resolve({}); }
    });
  });
}

/* ---------- Obtener información de un vídeo ---------- */
async function getInfo(url) {
  const raw = await run(YTDLP, ["--dump-json", "--no-playlist", "--no-warnings", url]);
  const line = raw.trim().split("\n").filter(Boolean).pop();
  const j = JSON.parse(line);
  return {
    id: j.id,
    title: j.title || "Sin título",
    uploader: j.uploader || j.channel || "Desconocido",
    duration: j.duration || 0,
    thumbnail: j.thumbnail || "",
    webpage_url: j.webpage_url || url,
    view_count: j.view_count || 0,
  };
}

/* ---------- Buscar en una fuente concreta ---------- */
async function searchSource(prefix, query, limit) {
  try {
    const raw = await run(YTDLP, [
      prefix + limit + ":" + query,
      "--flat-playlist", "--dump-json", "--no-warnings", "--socket-timeout", "20",
    ]);
    return raw
      .trim().split("\n").filter(Boolean)
      .map((l) => { try { return JSON.parse(l); } catch (_) { return null; } })
      .filter(Boolean);
  } catch (_) {
    return []; // si una fuente falla, seguimos con las demás
  }
}

/* ---------- Formatos disponibles (calidades) ---------- */
async function getFormats(url) {
  const raw = await run(YTDLP, ["--dump-json", "--no-playlist", "--no-warnings", url]);
  const line = raw.trim().split("\n").filter(Boolean).pop();
  const j = JSON.parse(line);

  const audio = [];
  const video = [];
  const AUDIO_EXTS = ["m4a", "webm", "mp3", "ogg", "opus", "aac", "flac", "wav"];

  for (const f of j.formats || []) {
    const tam = f.filesize || f.filesize_approx || null;
    if (f.vcodec === "none" && AUDIO_EXTS.includes(f.ext)) {
      audio.push({
        ext: f.ext, abr: f.abr || null, asr: f.asr || null,
        note: f.format_note || null, filesize: tam,
      });
    } else if (f.vcodec && f.vcodec !== "none") {
      video.push({
        ext: f.ext, height: f.height || null, width: f.width || null,
        fps: f.fps || null, vcodec: f.vcodec, note: f.format_note || null, filesize: tam,
      });
    }
  }

  const quitar = (a, k) => a.filter((x, i) => a.findIndex((y) => y[k] === x[k]) === i);

  return {
    title: j.title, uploader: j.uploader || j.channel, duration: j.duration,
    thumbnail: j.thumbnail, webpage_url: j.webpage_url || url,
    audio: quitar(audio, "abr"),
    video: quitar(video, "height").sort((a, b) => (b.height || 0) - (a.height || 0)),
  };
}

/* ---------- Relacionados / recomendaciones ---------- */
function extraerIdYoutube(url) {
  try {
    const u = new URL(url);
    if (u.hostname.includes("youtu.be")) return u.pathname.slice(1);
    if (u.searchParams.get("v")) return u.searchParams.get("v");
    const m = u.pathname.match(/\/(shorts|embed|live)\/([^/?]+)/);
    if (m) return m[2];
  } catch (_) {}
  return null;
}

async function getRelated(url) {
  const id = extraerIdYoutube(url);
  // "Mix" de YouTube: da contenido relacionado automáticamente.
  const mixUrl = id
    ? "https://www.youtube.com/watch?v=" + id + "&list=RD" + id
    : url;
  const raw = await run(YTDLP, [
    mixUrl, "--flat-playlist", "--dump-json", "--no-warnings", "--socket-timeout", "25",
  ]);
  return raw.trim().split("\n").filter(Boolean)
    .map((l) => { try { return JSON.parse(l); } catch (_) { return null; } })
    .filter(Boolean)
    .slice(0, 25);
}

/* ---------- Descarga ---------- */
function startDownload(url, format, quality) {
  const id = "job" + (++jobCounter) + "_" + Date.now();
  const job = {
    id,
    state: "downloading",
    progress: 0,
    error: null,
    file: null,
    filename: null,
    format,
    info: null,
    startedAt: Date.now(),
  };
  jobs.set(id, job);

  const outBase = path.join(OUT_DIR, id);

  (async () => {
    try {
      job.info = await getInfo(url);
      const safeTitle = (job.info.title || "audio").replace(/[^\p{L}\p{N}\s\-_().]/gu, "").slice(0, 80).trim();

      // Construimos los argumentos en orden correcto para yt-dlp.
      const args = ["--newline", "--no-playlist", "--no-warnings"];

      if (format === "mp3") {
        args.push("-x", "--audio-format", "mp3", "--audio-quality", quality || "0");
        // Solo indicamos la ruta de ffmpeg si la hemos encontrado.
        if (FFMPEG) args.push("--ffmpeg-location", FFMPEG);
        args.push("-o", outBase + ".%(ext)s");
      } else {
        args.push("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b", "--merge-output-format", "mp4", "-o", outBase + ".mp4");
      }

      args.push(url);

      const child = spawn(YTDLP, args);
      let errBuf = "";
      child.stderr.on("data", (b) => (errBuf += b.toString()));

      child.stdout.on("data", (buf) => {
        const txt = buf.toString();
        const m = txt.match(/\[download\]\s+([\d.]+)%/);
        if (m) {
          const pct = parseFloat(m[1]);
          if (!Number.isNaN(pct)) job.progress = Math.min(99, Math.round(pct));
        }
      });

      child.on("error", (e) => {
        job.state = "error";
        job.error = e.message;
      });

      child.on("close", (code) => {
        if (code !== 0) {
          job.state = "error";
          const detalle = (errBuf.trim().split("\n").slice(-4).join(" ")).slice(0, 500);
          job.error = job.error || ("yt-dlp terminó con error (" + code + ")" + (detalle ? ": " + detalle : ""));
          return;
        }
        // Localizar el archivo generado
        const dir = fs.readdirSync(OUT_DIR).filter((f) => f.startsWith(id + "."));
        if (!dir.length) {
          job.state = "error";
          job.error = "No se encontró el archivo generado.";
          return;
        }
        job.file = path.join(OUT_DIR, dir[0]);
        job.filename = safeTitle + (format === "mp3" ? ".mp3" : ".mp4");
        job.progress = 100;
        job.state = "done";
      });
    } catch (e) {
      job.state = "error";
      job.error = String(e.message || e).slice(0, 600);
    }
  })();

  return job;
}

/* ---------- Servidor ---------- */
const server = http.createServer(async (req, res) => {
  const u = new URL(req.url, "http://localhost");
  const p = u.pathname;
  const q = u.searchParams;

  // Respuesta a preflight CORS
  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    });
    return res.end();
  }

  try {
    // 1) Información de un enlace
    if (p === "/api/info" && req.method === "GET") {
      const url = q.get("url");
      if (!url || !isValidUrl(url)) return sendJSON(res, 400, { error: "URL no válida." });
      const info = await getInfo(url);
      return sendJSON(res, 200, info);
    }

    // 2) Iniciar descarga
    if (p === "/api/start" && req.method === "POST") {
      const body = await readBody(req);
      if (!body.url || !isValidUrl(body.url)) return sendJSON(res, 400, { error: "URL no válida." });
      const format = body.format === "mp4" ? "mp4" : "mp3";
      const quality = body.quality || "0";
      const job = startDownload(body.url, format, quality);
      return sendJSON(res, 200, { id: job.id });
    }

    // 3) Consultar estado
    if (p.startsWith("/api/status/") && req.method === "GET") {
      const id = p.split("/")[3];
      const job = jobs.get(id);
      if (!job) return sendJSON(res, 404, { error: "Trabajo no encontrado." });
      return sendJSON(res, 200, {
        id: job.id,
        state: job.state,
        progress: job.progress,
        error: job.error,
        format: job.format,
        info: job.info,
        filename: job.filename,
      });
    }

    // 4) Descargar el archivo resultante
    if (p.startsWith("/api/file/") && req.method === "GET") {
      const id = p.split("/")[3];
      const job = jobs.get(id);
      if (!job || !job.file || !fs.existsSync(job.file)) {
        return sendJSON(res, 404, { error: "Archivo no disponible." });
      }
      const stat = fs.statSync(job.file);
      res.writeHead(200, {
        "Content-Type": job.format === "mp3" ? "audio/mpeg" : "video/mp4",
        "Content-Length": stat.size,
        "Content-Disposition": 'attachment; filename="' + encodeURIComponent(job.filename) + '"',
        "Access-Control-Allow-Origin": "*",
      });
      fs.createReadStream(job.file).pipe(res);
      // Limpieza diferida
      setTimeout(() => { try { fs.unlinkSync(job.file); jobs.delete(id); } catch (_) {} }, 60 * 1000);
      return;
    }

    // Búsqueda multi-fuente
    if (p === "/api/search" && req.method === "GET") {
      const q = q_get(u, "q");
      const limit = Math.min(parseInt(q_get(u, "limit") || "8", 10) || 8, 20);
      if (!q || q.trim().length < 2) return sendJSON(res, 400, { error: "Escribe al menos 2 letras." });

      const [yt, sc] = await Promise.all([
        searchSource("ytsearch", q, limit),
        searchSource("scsearch", q, limit),
      ]);

      const normalizar = (arr, fuente) => arr.map((r) => ({
        source: fuente,
        id: r.id,
        title: r.title || "Sin título",
        uploader: r.uploader || r.channel || "Desconocido",
        duration: r.duration || 0,
        thumbnail: r.thumbnail || "",
        url: r.url || r.webpage_url || "",
      }));

      return sendJSON(res, 200, {
        query: q,
        results: normalizar(yt, "YouTube").concat(normalizar(sc, "SoundCloud")),
      });
    }

    // Formatos y calidades disponibles para un enlace concreto
    if (p === "/api/formats" && req.method === "GET") {
      const url = q_get(u, "url");
      if (!url || !isValidUrl(url)) return sendJSON(res, 400, { error: "URL no válida." });
      const info = await getFormats(url);
      return sendJSON(res, 200, info);
    }

    // Contenido relacionado (base de las recomendaciones)
    if (p === "/api/related" && req.method === "GET") {
      const url = q_get(u, "url");
      if (!url || !isValidUrl(url)) return sendJSON(res, 400, { error: "URL no válida." });
      const items = await getRelated(url);
      return sendJSON(res, 200, {
        results: items.map((r) => ({
          source: "YouTube",
          id: r.id,
          title: r.title || "Sin título",
          uploader: r.uploader || r.channel || "Desconocido",
          duration: r.duration || 0,
          thumbnail: r.thumbnail || "",
          url: r.url || r.webpage_url || "",
        })),
      });
    }

    // 5) Página de estado sencilla
    if (p === "/" && req.method === "GET") {
      res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      return res.end(
        "<!doctype html><meta charset='utf-8'><title>Sonora</title>" +
        "<body style='font-family:system-ui;max-width:640px;margin:60px auto;padding:0 20px'>" +
        "<h1>Servidor de Sonora ✅</h1>" +
        "<p>El servidor está funcionando. Conecta la app Android a esta dirección:</p>" +
        "<code style='display:block;background:#111;color:#fff;padding:14px;border-radius:10px'>http://" +
        primeraIp() + ":" + PORT + "</code>" +
        "<p style='margin-top:24px'>Trabajos en curso: <b>" + jobs.size + "</b></p></body>"
      );
    }

    sendJSON(res, 404, { error: "Ruta no encontrada." });
  } catch (e) {
    sendJSON(res, 500, { error: String(e.message || e).slice(0, 600) });
  }
});

/* ---------- IP local para mostrar al usuario ---------- */
function primeraIp() {
  const ifs = os.networkInterfaces();
  for (const name of Object.keys(ifs)) {
    for (const it of ifs[name] || []) {
      if (it.family === "IPv4" && !it.internal) return it.address;
    }
  }
  return "localhost";
}

server.listen(PORT, () => {
  console.log("\n  🎵 Servidor de Sonora activo");
  console.log("  URL para la app:  http://" + primeraIp() + ":" + PORT);
  console.log("  Abrir en navegador: http://localhost:" + PORT + "\n");
});
