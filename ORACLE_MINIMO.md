# Oracle Cloud Always Free - Pasos mínimos (gratis para siempre)

## 1) Crear cuenta Oracle Cloud Always Free
1. Ve a https://www.oracle.com/cloud/free/
2. Crear cuenta (necesitará tarjeta para verificación pero NUNCA cobra en Always Free)
3. Accede a Oracle Cloud Console

## 2) Crear VM Ubuntu
1. Ir a "Compute" → "Instances" → "Create Instance"
2. Nombre: sonora-vm
3. Image: Ubuntu 22.04 LTS
4. Shape: VM.Standard.A1.Flex (gratis) o VM.Standard.E2.1.Micro
5. SSH keys: genera o sube tu clave pública (si no tienes, genera con ssh-keygen)
6. Crear

## 3) Conectar por SSH y desplegar
Con IP pública de la VM (ej. 140.XX.XX.XX):
```bash
ssh ubuntu@TU_IP_PUBLICA
```

Descarga y ejecuta script automático:
```bash
curl -O https://raw.githubusercontent.com/raykozabala/sonora-app/main/setup-sonora.sh
chmod +x setup-sonora.sh
./setup-sonora.sh
```

## 4) Abrir puerto 8080
En Oracle Cloud:
1. Networking → Virtual Cloud Networks → Selecciona VCN → Security Lists → Default Security List
2. Add Ingress Rules:
   - Source CIDR: 0.0.0.0/0
   - Destination Port Range: 8080
   - Protocol: TCP
3. Aceptar

## 5) Poner URL en app Sonora
URL: http://TU_IP_PUBLICA:8080
(Con Caddy puedes poner HTTPS después si quieres)

## Comprobar
```bash
pm2 status
curl http://TU_IP_PUBLICA:8080/
```
