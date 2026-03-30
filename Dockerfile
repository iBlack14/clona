# Usar Node 16 para mejor compatibilidad con Electron 9 y dependencias antiguas
FROM node:16-bullseye

# Instalamos dependencias del sistema y entorno gráfico mínimo
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    libnss3 libatk-bridge2.0-0 libcups2 libgtk-3-0 libgbm-dev libasound2 \
    xvfb x11vnc fluxbox websockify curl wget default-jdk \
    apksigner zipalign zlib1g-dev \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /usr/share/novnc \
    && wget -qO- https://github.com/novnc/noVNC/archive/v1.2.0.tar.gz | tar xz --strip-components=1 -C /usr/share/novnc

WORKDIR /app

# Cache buster para forzar rebuild
ARG CACHEBUST=14

# Copiamos solo la parte del servidor
COPY AhMyth-Server/app /app
RUN chmod -R 777 /app

# Generar keystore de firma para el APK
RUN keytool -genkey -v -keystore /app/app/Factory/debug.keystore \
    -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Debug, OU=Debug, O=Debug, L=Unknown, ST=Unknown, C=US"

# Instalamos dependencias de node
RUN cd /app && npm install

# Instalamos electron de forma global para la ejecución headless
RUN npm install -g electron@9.2.0

# Creamos el entrypoint INLINE para evitar cache viejo
RUN printf '#!/bin/bash\n\
# 1. Iniciar pantalla virtual FULL HD\n\
Xvfb :99 -screen 0 1920x1080x24 &\n\
export DISPLAY=:99\n\
\n\
# 2. Iniciar administrador de ventanas\n\
fluxbox &\n\
\n\
# 3. Iniciar servidor VNC exclusivo para acceso RealVNC Directo (Puerto 5900)
# Usamos -rfbport 5900, -shared, -noxdamage y -repeat
x11vnc -display :99 -forever -passwd "clona123" -rfbport 5900 -shared -noxdamage -repeat &\n\
\n\
# 4. Iniciar el servidor de AhMyth (Sin websockify)
echo "Iniciando Clona Server..."\n\
electron /app --no-sandbox\n' > /entrypoint.sh && chmod +x /entrypoint.sh

# Puertos: 42474 (Celulares) y 5900 (RealVNC)
EXPOSE 42474 5900

CMD ["/entrypoint.sh"]
