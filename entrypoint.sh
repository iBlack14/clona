#!/bin/bash
# 1. Iniciar pantalla virtual
Xvfb :99 -screen 0 1920x1080x24 &
export DISPLAY=:99

# 2. Iniciar administrador de ventanas
fluxbox &

# 3. Iniciar servidor VNC (Contraseña: clona123)
x11vnc -display :99 -forever -passwd "clona123" &

# 4. Iniciar bridge para ver en el navegador (Puerto 9000)
# Usamos websockify directo para evitar errores de rutas de novnc_proxy
websockify --web /usr/share/novnc 9000 localhost:5900 &

# 5. Iniciar el servidor de AhMyth
echo "Iniciando AhMyth Server..."
electron /app --no-sandbox
