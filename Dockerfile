# Usar Node 16 para mejor compatibilidad con Electron 9 y dependencias antiguas
FROM node:16-bullseye

# Instalamos dependencias del sistema y entorno gráfico mínimo
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    libnss3 libatk-bridge2.0-0 libcups2 libgtk-3-0 libgbm-dev libasound2 \
    xvfb x11vnc fluxbox novnc websockify curl wget default-jre \
    apksigner zipalign \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache buster para forzar rebuild
ARG CACHEBUST=3

# Copiamos solo la parte del servidor
COPY AhMyth-Server/app /app

# Instalamos dependencias de node
RUN cd /app && npm install

# Instalamos electron de forma global para la ejecución headless
RUN npm install -g electron@9.2.0

# Preparamos el script de inicio
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Puerto 9000: Para ver el panel en el navegador (noVNC)
# Puerto 42474: Para recibir conexiones de los celulares
EXPOSE 9000 42474

CMD ["/entrypoint.sh"]
