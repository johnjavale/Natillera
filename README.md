# Natillera Familiar

> Ahorramos juntos, crecemos juntos.

Plataforma web para administrar la natillera familiar: 10 integrantes ahorran durante un ciclo de 12 meses, el fondo hace
préstamos con interés simple, organiza 2 rifas, guarda el dinero en cajitas de Nu (una por integrante, en la cuenta de uno
de los 2 tesoreros) y al cierre reparte las ganancias. Reemplaza el archivo `Natillera_Familiar (1).xlsx` con las mismas
fórmulas.

| Carpeta | Contenido |
|---|---|
| [backend/](backend/) | API Spring Boot 3.5 · Java 21 · MySQL 8 · JWT · Flyway |
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
- MySQL: `localhost:3306` (natillera / natillera)

El perfil `dev` carga el seed y fija la fecha simulada en **2027-03-20** (mes 3 del ciclo 2027) para que haya aportes,
atrasos, un préstamo y rifas que mostrar. Cambie `FECHA_SIMULADA` en `docker-compose.yml` o déjela vacía para usar la
fecha real.

### Opción B — Sin Docker

Requisitos: JDK 21, Node 22 y un MySQL 8.0.16+ con una base `natillera` (usuario y clave `natillera`):

```sql
CREATE DATABASE natillera CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'natillera'@'localhost' IDENTIFIED BY 'natillera';
GRANT ALL PRIVILEGES ON natillera.* TO 'natillera'@'localhost';
```

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

**Base en blanco (perfil `local`):** crea solo el esquema, sin seed, con la fecha real.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run   # PowerShell: $env:SPRING_PROFILES_ACTIVE="local"; .\mvnw.cmd spring-boot:run
```

Para ingresar la primera vez, cree un administrador en MySQL (clave temporal `Natillera2027*`; el sistema pide
cambiarla al ingresar):

```sql
INSERT INTO usuario (nombre, correo, password_hash, debe_cambiar_password, creado_por) VALUES
  ('Administrador', 'admin@natillera.local', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, 'instalacion');
INSERT INTO usuario_rol (usuario_id, rol_id) SELECT id, 1 FROM usuario WHERE correo = 'admin@natillera.local';
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
cd backend && ./mvnw verify        # unitarias (golden tests del Excel) + integración contra MySQL real
cd frontend && npm test            # Vitest
cd frontend && npx playwright test # E2E: requiere el stack arriba (docker compose up)
```

Las pruebas de integración usan **Testcontainers** (`mysql:8.0`) cuando hay Docker. Si no lo hay, usan un MySQL
local: por defecto `jdbc:mysql://localhost:3306/natillera_test` con usuario y clave `natillera`, configurable con
`NATILLERA_TEST_DB_URL`, `NATILLERA_TEST_DB_USER` y `NATILLERA_TEST_DB_PASSWORD`. Esa base **se borra y se recrea en
cada ejecución**, por eso su nombre debe terminar en `_test`. Para forzar el MySQL local aunque haya Docker:
`NATILLERA_TEST_DB=local`.

```sql
GRANT ALL PRIVILEGES ON natillera_test.* TO 'natillera'@'localhost';
```

## Desplegar

### 1. Backend + MySQL en Railway (plan Hobby)

1. Cree un proyecto en Railway y agregue **MySQL** (Backups → habilitar).
2. Agregue un servicio desde el repositorio con **Root Directory** = `backend`. Railway usa `backend/Dockerfile` y
   `backend/railway.json` (healthcheck `/actuator/health`, *app sleeping* activado).
3. Variables del servicio:

   | Variable | Valor |
   |---|---|
   | `DATABASE_URL` | `${{MySQL.MYSQL_URL}}` (red privada; el backend convierte `mysql://…` a JDBC) |
   | `JWT_SECRET` | `openssl rand -base64 48` |
   | `CORS_ALLOWED_ORIGIN` | `https://<su-app>.vercel.app` |
   | `FRONTEND_URL` | `https://<su-app>.vercel.app` |
   | `SPRING_PROFILES_ACTIVE` | `prod` |

4. Genere un dominio público para el servicio (lo usará Vercel como `BACKEND_URL`).
5. Primer usuario administrador: el perfil `prod` no carga el seed. Cree el admin con un `INSERT` en la consola de
   MySQL de Railway (ver [ARQUITECTURA.md §9](ARQUITECTURA.md#9-despliegue-y-operación)); el sistema le pedirá
   cambiar la contraseña al ingresar.

### 2. Frontend en Vercel (plan Hobby, uso no comercial)

1. Importe el repositorio con **Root Directory** = `frontend` (framework Next.js).
2. Variables: `BACKEND_URL=https://<backend>.up.railway.app`, `SESSION_SECRET=<≥ 32 caracteres aleatorios>`,
   `COOKIE_SECURE=true`.
3. Despliegue y actualice `CORS_ALLOWED_ORIGIN` en Railway con el dominio final de Vercel.

Checklist completo de despliegue: [test.md §10](test.md#10-checklist-de-despliegue).
