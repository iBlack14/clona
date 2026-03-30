#!/bin/bash
# 1. Iniciar pantalla virtual
Xvfb :99 -screen 0 1920x1080x24 &
export DISPLAY=:99
sleep 2

# 2. Iniciar administrador de ventanas
fluxbox &
sleep 1

# 3. Iniciar servidor VNC exclusivo para acceso RealVNC Directo (Puerto 5900)
# Usamos -shared, -noxdamage y -repeat para máxima fluidez
x11vnc -display :99 -forever -passwd "clona123" -rfbport 5900 -shared -noxdamage -repeat &

# 4. Iniciar el servidor de AhMyth
echo "Iniciando Clona Server..."
electron /app --no-sandbox
