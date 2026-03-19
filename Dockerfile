# syntax=docker/dockerfile:1
FROM node:20-bookworm-slim AS build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential python3 pkg-config \
    libcairo2-dev libpango1.0-dev libjpeg-dev libgif-dev librsvg2-dev libpng-dev \
    sqlite3 libsqlite3-dev \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# esbuild install script can fail with ETXTBSY on some overlayfs setups.
# Pin a stable binary and point the installer to it.
ARG ESBUILD_VERSION=0.27.3
RUN npm i -g esbuild@${ESBUILD_VERSION}
ENV ESBUILD_BINARY_PATH=/usr/local/bin/esbuild

# Backend deps (force sqlite3 to build from source to match runtime glibc)
COPY backend/package*.json backend/
RUN cd backend && npm ci --omit=dev --build-from-source=sqlite3

# Frontend deps + build
COPY frontend/package*.json frontend/
RUN cd frontend && npm ci
COPY frontend frontend
RUN cd frontend && npm run build

# Backend source
COPY backend backend
COPY assets assets

FROM node:20-bookworm-slim AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
    libcairo2 libpango-1.0-0 libjpeg62-turbo libgif7 librsvg2-2 libpng16-16 \
    sqlite3 \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app/backend

COPY --from=build /app/backend /app/backend
COPY --from=build /app/frontend/dist /app/frontend/dist
COPY --from=build /app/assets /app/assets

ENV NODE_ENV=production \
    PORT=3000

EXPOSE 3000

CMD ["node", "src/server.js"]
