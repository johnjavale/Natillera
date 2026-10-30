# Natillera Familiar

> Ahorramos juntos, crecemos juntos.

Plataforma web para administrar la natillera familiar: 10 integrantes ahorran durante un ciclo de 12 meses, el fondo hace
préstamos con interés simple, organiza 2 rifas, guarda el dinero en cajitas de Nu (una por integrante, en la cuenta de uno
de los 2 tesoreros) y al cierre reparte las ganancias. Reemplaza el archivo `Natillera_Familiar (1).xlsx` con las mismas
fórmulas.

| Carpeta | Contenido |
|---|---|
| [backend/](backend/) | API Spring Boot 3.5 · Java 21 · PostgreSQL 16 · JWT · Flyway |
| [frontend/](frontend/) | Next.js 15 (App Router, BFF) · TypeScript · Tailwind · TanStack Query |
| [ARQUITECTURA.md](ARQUITECTURA.md) | Arquitectura, modelo de datos, patrones, decisiones y despliegue |
| [test.md](test.md) | Catálogo de pruebas, trazabilidad y evidencia de la última ejecución |

## Ejecutar en local

### Opción A — Docker (recomendada)

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8080 (Swagger UI en http://localhost:8080/swagger-ui.html, solo perfil `dev`)
- PostgreSQL: `localhost:5432` (natillera / natillera)

El perfil `dev` carga el seed y fija la fecha simulada en **2027-03-20** (mes 3 del ciclo 2027) para que haya aportes,
atrasos, un préstamo y rifas que mostrar. Cambie `FECHA_SIMULADA` en `docker-compose.yml` o déjela vacía para usar la
fecha real.

### Opción B — Sin Docker

Requisitos: JDK 21, Node 22 y un PostgreSQL 16 con una base `natillera` (usuario y clave `natillera`).

```bash
# Backend (perfil dev con seed)
cd backend
./mvnw spring-boot:run            # en Windows: mvnw.cmd spring-boot:run

# Frontend
cd frontend
cp .env.example .env.local        # BACKEND_URL, SESSION_SECRET, COOKIE_SECURE=false
npm install
npm run dev                       # http://localhost:3000
```

### Credenciales del seed (solo desarrollo)

Contraseña de todos: **`Natillera2027*`**. También se puede ingresar con el celular.

| Usuario | Roles | Integrante |
|---|---|---|
| `admin@natillera.local` | ADMIN + INTEGRANTE | 1 · John Jairo Valencia |
| `tesorero1@natillera.local` | TESORERO + INTEGRANTE (integrantes 1–5) | 2 · Pascuala Leyes |
| `tesorero2@natillera.local` | TESORERO + INTEGRANTE (integrantes 6–10) | 6 · Arley Leudo |
| `integrante3@natillera.local` | INTEGRANTE | 3 · Samay Moreno (tiene el préstamo de ejemplo) |
| `integrante4@`, `integrante5@`, `integrante7@` … `integrante10@natillera.local` | INTEGRANTE | 4, 5, 7–10 |

Cuotas de la sección 3.9: 100.000, 50.000, 200.000, 100.000, 150.000, 50.000, 300.000, 100.000, 80.000 y 120.000.

## Pruebas

```bash
cd backend && ./mvnw verify        # unitarias (golden tests del Excel) + integración contra PostgreSQL real
cd frontend && npm test            # Vitest
cd frontend && npx playwright test # E2E: requiere el stack arriba (docker compose up)
```

Las pruebas de integración usan **Testcontainers** cuando hay Docker; si no lo hay, levantan automáticamente un
PostgreSQL 16 embebido (zonky), así que también corren en Windows sin Docker. Para forzar el embebido:
`NATILLERA_TEST_DB=embebido`.

## Desplegar

### 1. Backend + PostgreSQL en Railway (plan Hobby)

1. Cree un proyecto en Railway y agregue **PostgreSQL** (Backups → habilitar).
2. Agregue un servicio desde el repositorio con **Root Directory** = `backend`. Railway usa `backend/Dockerfile` y
   `backend/railway.json` (healthcheck `/actuator/health`, *app sleeping* activado).
3. Variables del servicio:

   | Variable | Valor |
   |---|---|
   | `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` (red privada; el backend convierte `postgresql://…` a JDBC) |
   | `JWT_SECRET` | `openssl rand -base64 48` |
   | `CORS_ALLOWED_ORIGIN` | `https://<su-app>.vercel.app` |
   | `FRONTEND_URL` | `https://<su-app>.vercel.app` |
   | `SPRING_PROFILES_ACTIVE` | `prod` |

4. Genere un dominio público para el servicio (lo usará Vercel como `BACKEND_URL`).
5. Primer usuario administrador: el perfil `prod` no carga el seed. Cree el admin con un `INSERT` en la consola de
   PostgreSQL de Railway (ver [ARQUITECTURA.md §9](ARQUITECTURA.md#9-despliegue-y-operación)); el sistema le pedirá
   cambiar la contraseña al ingresar.

### 2. Frontend en Vercel (plan Hobby, uso no comercial)

1. Importe el repositorio con **Root Directory** = `frontend` (framework Next.js).
2. Variables: `BACKEND_URL=https://<backend>.up.railway.app`, `SESSION_SECRET=<≥ 32 caracteres aleatorios>`,
   `COOKIE_SECURE=true`.
3. Despliegue y actualice `CORS_ALLOWED_ORIGIN` en Railway con el dominio final de Vercel.

Checklist completo de despliegue: [test.md §10](test.md#10-checklist-de-despliegue).
