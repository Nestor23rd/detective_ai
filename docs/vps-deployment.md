# VPS Deployment

This project can be deployed from the same repository while running the frontend and backend separately.

The recommended pattern is:
- deploy the backend from the `backend/` folder
- deploy the frontend from the `frontend/` folder
- let the frontend proxy `/api` requests to the backend

That solves the common mixed-content problem:
- browser loads the frontend over HTTPS
- browser calls `/api` on the same frontend origin
- the frontend server forwards those requests to the backend over HTTP or HTTPS

The browser never talks directly to an insecure backend URL.

## Backend VPS

### 1. Prepare environment
```bash
cd backend
cp .env.example .env
```

Edit `.env` and set at least:
- `ASI_ONE_API_KEY`
- `APP_ALLOWED_ORIGINS`

If MongoDB runs in the same compose stack, keep:
```env
MONGODB_URI=mongodb://mongodb:27017/ai_detective
```

For the current separate-compose setup, expose the backend on the VPS network so the frontend container can reach it:
```env
BACKEND_BIND_ADDRESS=0.0.0.0
```

### 2. Deploy
```bash
cd backend
docker compose up -d --build
```

The backend will be exposed on:
```text
http://your-server:8080
```

## Frontend VPS

### 1. Prepare environment
```bash
cd frontend
cp .env.example .env
```

Set:
- `SITE_ADDRESS`
- `API_UPSTREAM`

Examples:

If the frontend has a real domain and the backend stays plain HTTP on another VPS:
```env
SITE_ADDRESS=app.example.com
API_UPSTREAM=http://203.0.113.10:8080
```

If the backend later gets its own HTTPS domain:
```env
SITE_ADDRESS=app.example.com
API_UPSTREAM=https://api.example.com
```

### 2. Deploy
```bash
cd frontend
docker compose up -d --build
```

If `SITE_ADDRESS` is a real public domain and ports `80` and `443` are open, Caddy will serve the frontend over HTTPS automatically.

## Same server deployment

You can also deploy both from the same repository on the same VPS:

### Backend
```bash
cd /path/to/repo/backend
docker compose up -d --build
```

### Frontend
```bash
cd /path/to/repo/frontend
cp .env.example .env
```

Then set:
```env
SITE_ADDRESS=app.example.com
API_UPSTREAM=http://your-server-ip:8080
```

And run:
```bash
cd /path/to/repo/frontend
docker compose up -d --build
```

In that setup:
- the frontend is public on HTTPS
- the backend remains on the same VPS on port `8080`
- `/api` is proxied by the frontend server

## Why this is better than hardcoding an API URL in Angular

The frontend build already uses `/api` as its base path.

That means:
- no rebuild is needed just to change API protocol or hostname
- no browser mixed-content issue when the frontend is HTTPS
- the proxy target can be changed only through `frontend/.env`

## Important notes

- Open ports `80` and `443` on the frontend VPS if you want automatic HTTPS through Caddy.
- Open port `8080` on the backend VPS only if the frontend VPS needs to reach it publicly.
- If the backend should stay private, place both stacks on the same server or use a private network/VPN between VPS instances.
- Set `APP_ALLOWED_ORIGINS` on the backend if you still want direct browser access to backend endpoints from specific origins.
