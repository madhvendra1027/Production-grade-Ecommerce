# Render Deployment Guide for Production-Grade E-Commerce

This repository is now fully configured for automatic deployment on [Render](https://render.com).

---

## Architecture Overview on Render

- **Backend Web Service (`sb-ecommerce-backend`)**:
  - Runtime: Docker (`Backend/sb-ecommerce/Dockerfile`)
  - Root Directory: `Backend/sb-ecommerce`
  - Health Check: `/actuator/health`
  - Port: Bound to `${PORT}` dynamically (default: `10000`)
  - JVM Configuration: Memory-optimized for 512 MB Free Tier (`-XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=70.0`)

- **Frontend Static Site (`sb-ecommerce-frontend`)**:
  - Runtime: Static Site
  - Root Directory: `Frontend/ecom-frontend`
  - Build Command: `npm install && npm run build`
  - Publish Directory: `dist`
  - SPA Rewrites: All routes (`/*`) rewrite to `/index.html` (prevents 404 on page refresh)

- **Database**:
  - Managed PostgreSQL (Render PostgreSQL, Neon, or Supabase).

---

## Method 1: Deploy with Render Blueprint (`render.yaml`) (Recommended)

1. **Commit and push** your latest changes to GitHub:
   ```bash
   git add .
   git commit -m "Configure project for Render deployment"
   git push origin main
   ```

2. Log in to [Render Dashboard](https://dashboard.render.com).
3. Click **New +** -> **Blueprint**.
4. Select your GitHub repository: `madhvendra1027/Production-grade-Ecommerce`.
5. Render will automatically detect `render.yaml` and configure both services.
6. Fill in the required environment variables when prompted (see below) and click **Apply**.

---

## Method 2: Manual Service Setup

If setting up services manually in the dashboard:

### 1. Database Setup (Render Postgres or Neon)
If using Render PostgreSQL:
- Click **New +** -> **PostgreSQL Database**.
- Name: `sb-ecom-db`
- Database: `sb-ecom`
- User: `postgres` (or default generated)
- Note the **Internal Database URL** and credentials.

### 2. Backend Web Service
- Click **New +** -> **Web Service**.
- Select your repository: `madhvendra1027/Production-grade-Ecommerce`.
- Name: `sb-ecommerce-backend`
- Region: Same as your database.
- **Root Directory**: `Backend/sb-ecommerce` *(CRUCIAL!)*
- Environment: **Docker**
- Dockerfile Path: `Dockerfile`
- Health Check Path: `/actuator/health`
- **Environment Variables**:
  | Key | Example Value | Description |
  | :--- | :--- | :--- |
  | `PORT` | `10000` | Port assigned by Render |
  | `DB_URL` | `jdbc:postgresql://<db-host>:5432/sb-ecom?sslmode=require` | Must start with `jdbc:postgresql://` |
  | `DB_USERNAME` | `<db-user>` | PostgreSQL username |
  | `DB_PASSWORD` | `<db-password>` | PostgreSQL password |
  | `JWT_SECRET` | *(Generate 32-byte base64 string)* | e.g. run `openssl rand -base64 48` |
  | `STRIPE_SECRET_KEY` | `sk_test_...` | Stripe secret key |
  | `FRONTEND_URL` | `https://sb-ecommerce-frontend.onrender.com` | URL of your deployed frontend |
  | `IMAGE_BASE_URL` | `https://sb-ecommerce-backend.onrender.com/images/` | Public URL for uploaded product images |
  | `APP_JWT_COOKIE_SECURE` | `true` | Required for HTTPS cookie transmission |
  | `APP_JWT_COOKIE_SAME_SITE` | `None` | Required for cross-subdomain cookies |
  | `DDL_AUTO` | `validate` | Hibernate validation against Flyway |
  | `FLYWAY_ENABLED` | `true` | Runs V1 baseline migration |

### 3. Frontend Static Site
- Click **New +** -> **Static Site**.
- Select your repository: `madhvendra1027/Production-grade-Ecommerce`.
- Name: `sb-ecommerce-frontend`
- **Root Directory**: `Frontend/ecom-frontend` *(CRUCIAL!)*
- Build Command: `npm install && npm run build`
- Publish Directory: `dist`
- **Environment Variables**:
  | Key | Value | Description |
  | :--- | :--- | :--- |
  | `VITE_BACK_END_URL` | `https://sb-ecommerce-backend.onrender.com` | URL of your deployed backend |
  | `VITE_STRIPE_PUBLISHABLE_KEY` | `pk_test_...` | Stripe publishable key |
  | `VITE_FRONTEND_URL` | `https://sb-ecommerce-frontend.onrender.com` | Frontend URL |
- **Redirects / Rewrites**:
  - Go to **Redirects/Rewrites** tab in your static site.
  - Add Rule:
    - **Type**: `Rewrite`
    - **Source**: `/*`
    - **Destination**: `/index.html`
