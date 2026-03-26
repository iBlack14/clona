#!/bin/bash
# 1. Iniciar pantalla virtual
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99

# 2. Iniciar administrador de ventanas
fluxbox &

# 3. Iniciar servidor VNC (Contraseña: clona123)
x11vnc -display :99 -forever -passwd "clona123" -listen localhost &

# 4. Iniciar bridge para ver en el navegador (Puerto 9000)
/usr/share/novnc/utils/launch.sh --vnc localhost:5900 --listen 9000 &

# 5. Iniciar el servidor de AhMyth
echo "Iniciando AhMyth Server..."
electron /app --no-sandbox
