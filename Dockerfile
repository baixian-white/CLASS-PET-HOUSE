# syntax=docker/dockerfile:1
FROM node:20-bookworm-slim AS build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential python3 pkg-config \
    libcairo2-dev libpango1.0-dev libjpeg-dev libgif-dev librsvg2-dev libpng-dev \
    sqlite3 libsqlite3-dev \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Backend deps
COPY backend/package*.json backend/
RUN cd backend && npm ci --omit=dev

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
