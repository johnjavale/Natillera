# PROMPT — Plataforma web "Natillera Familiar"

> Copia este documento completo como instrucción para un agente de código (Claude Code u otro).
> Está escrito para que el agente lo ejecute de principio a fin sin pedir más contexto.

---

## 0. Rol y objetivo

Actúa como **arquitecto de software y desarrollador full-stack senior**. Construye una aplicación web para administrar una **natillera familiar**: un fondo de ahorro colectivo de 10 integrantes durante un ciclo de 12 meses. El fondo hace préstamos con interés, organiza 2 rifas al año, guarda el dinero en **cajitas de Nu** (una cajita por integrante, en la cuenta de uno de los 2 tesoreros) y al cierre reparte las ganancias.

Hoy toda la lógica vive en un archivo de Excel. **Toda esa lógica debe llevarse al sistema** tal como se especifica en la sección 3, con las fórmulas exactas.

**Entregables obligatorios** (sección 9):
1. Código fuente completo: monorepo con `backend/` y `frontend/`.
2. `test.md`: plan y catálogo de pruebas que cubra todo el sitio.
3. `ARQUITECTURA.md`: documento de explicación de lo desarrollado, la arquitectura y los patrones usados.
4. `README.md` con instrucciones de ejecución local y de despliegue.

---

## 1. Stack técnico (obligatorio)

| Capa | Tecnología |
|---|---|
| Backend | **Java 21**, **Spring Boot 3.x** (Web, Security, Data JPA, Validation), **JWT** (jjwt o Nimbus), Flyway, MapStruct, springdoc-openapi, Apache POI (exportar a Excel) |
| Frontend | **Next.js** (App Router, TypeScript, Tailwind CSS), React Hook Form + Zod, TanStack Query |
| Base de datos | **PostgreSQL 16** |
| Pruebas | JUnit 5, Mockito, AssertJ, **Testcontainers** (PostgreSQL), Spring Security Test, Vitest + Testing Library (frontend), **Playwright** (E2E) |
| Despliegue | Backend y PostgreSQL en **Railway** (plan Hobby de US$5/mes). Frontend en **Vercel** (plan Hobby, uso no comercial) |
| Local | `docker-compose.yml` con postgres, backend y frontend |

Reglas generales:
- Maneja el dinero **siempre con `BigDecimal`** (Java) y `NUMERIC(14,2)` (PostgreSQL). Nunca uses `double`.
- La moneda es el peso colombiano (COP). Se muestra sin decimales (`$1.250.000`) y se redondea HALF_UP solo al presentar o al persistir cuotas.
- Zona horaria `America/Bogota`. Idioma de la interfaz: **español**.

---

## 2. Roles, autenticación y autorización

### 2.1 Roles
| Rol | Descripción |
|---|---|
| `ADMIN` | Administrador de la natillera. Ve y edita todo. |
| `TESORERO` | Hay 2. Cada uno guarda en su cuenta Nu las cajitas de los integrantes que tiene asignados. |
| `INTEGRANTE` | Ahorrador. Solo ve la información de su propia cuenta. |

Un usuario puede tener **varios roles**: un tesorero o el administrador también suelen ser integrantes y ahorrar. Modela la relación usuario↔rol como N:M y la relación usuario↔integrante como 1:0..1.

### 2.2 Matriz de permisos (se aplica en el BACKEND, no solo en la UI)

| Recurso / acción | INTEGRANTE | TESORERO | ADMIN |
|---|---|---|---|
| Ver su perfil, aportes, cajita, préstamos, boletas y repartición propia | ✅ | ✅ | ✅ |
| Ver aportes y saldos de cajitas de **otros** integrantes | ❌ | ✅ solo los integrantes **asignados a él** | ✅ todos |
| Registrar movimientos de tesorería (aportes, pagos, rendimientos, desembolsos) | ❌ | ✅ solo en cajitas que administra | ✅ |
| Ver préstamos | ✅ solo los suyos | ✅ **todos** (lectura) *(ver supuesto)* | ✅ |
| Crear préstamos y registrar pagos de cuotas | ❌ | ✅ si el dinero sale o entra de una cajita suya | ✅ |
| Rifas: ver | ✅ sus boletas y el resultado general | ✅ todas | ✅ |
| Rifas: crear y editar | ❌ | ❌ | ✅ |
| Rifas: registrar pago de boletas | ❌ | ✅ de sus integrantes | ✅ |
| Conciliación con la app Nu (saldo reportado) | ❌ | ✅ sus cajitas | ✅ |
| Repartición: ver | ✅ solo su fila | ✅ filas de sus integrantes + totales | ✅ todo |
| Reportes consolidados | ✅ solo su estado de cuenta | ✅ alcance: sus integrantes (+ todos los préstamos) | ✅ todo |
| Configuración del ciclo, usuarios, roles, asignación de tesoreros | ❌ | ❌ | ✅ |
| Cerrar el ciclo / auditoría | ❌ | ❌ | ✅ |

> **Supuesto a confirmar**: el tesorero ve **todos** los préstamos (para gestionar el cobro), pero solo ve saldos y aportes de los integrantes que tiene asignados. Deja esta regla aislada en una sola política (`PrestamoAccessPolicy`) para poder cambiarla fácilmente.

**Prevención de IDOR**: todo endpoint que reciba un `integranteId`, `prestamoId`, `cajitaId`, etc. debe validar el alcance del usuario autenticado. Implementa un `AccessScopeService` que resuelva el alcance (`OWN`, `ASSIGNED`, `ALL`) y úsalo en cada caso de uso, por ejemplo con `@PreAuthorize("@access.canViewIntegrante(#id)")`. Si un recurso existe pero está fuera del alcance, responde **404** (no 403) para no filtrar que existe.

### 2.3 JWT y sesión
- Login con usuario (correo o celular) + contraseña. Contraseñas con **BCrypt** (cost 12).
- **Access token** JWT firmado (HS256 con secreto ≥ 256 bits desde una variable de entorno, o RS256) que dura **15 min**. Claims: `sub` (userId), `roles`, `integranteId`, `tesoreroId`.
- **Refresh token** opaco y aleatorio que dura **7 días**. Se guarda **hasheado** en la base de datos, se **rota** en cada uso y, si se detecta un refresh reutilizado, se invalida toda la familia de tokens.
- **Patrón BFF en Next.js**: el navegador nunca ve el JWT. Los route handlers de Next.js (`/api/auth/*` y un proxy `/api/backend/*`) guardan access y refresh en **cookies httpOnly, Secure, SameSite=Lax** del dominio de Vercel y reenvían las peticiones al backend con `Authorization: Bearer`. Así se evitan los problemas de cookies cross-site entre Vercel y Railway.
- Logout que revoca el refresh token. Cambio de contraseña obligatorio en el primer ingreso. Rate limit en login (por ejemplo, 5 intentos cada 15 min por IP y usuario; con Bucket4j o un filtro simple en memoria).
- CORS: solo se acepta el origen del frontend (variable de entorno).

---

## 3. Lógica de negocio (migrada del Excel — exacta)

Implementa toda esta lógica como **servicios de dominio puros** (sin Spring ni JPA), para poder probarla unitariamente. Las fórmulas se escriben en pseudo-notación.

### 3.1 Configuración del ciclo (`Ciclo` / natillera)
| Campo | Default | Regla |
|---|---|---|
| nombre | "Natillera Familiar" | |
| fechaInicio | 2027-01-01 | Primer mes de ahorro |
| numeroMeses | 12 | 1..12 |
| tasaInteresMensualPrestamos | 0.03 (3 %) | Interés **simple** sobre el monto prestado |
| gastosAdministrativos | 0 | Se descuentan de los intereses antes de repartir |
| maxCuotasPrestamo | 12 | Ningún préstamo a más de un año |
| tasaEACajitasNu | 0.09 (ejemplo) | El admin la actualiza; Nu la cambia con frecuencia |
| tasaMensualNu (derivada) | — | `(1 + tasaEA)^(1/12) − 1` (usar `Math.pow` sobre double solo para esta tasa y convertir a BigDecimal con MathContext.DECIMAL64) |
| fechaCierre (derivada) | — | `fechaInicio + (numeroMeses − 1) meses` |
| numeroIntegrantes (derivado) | — | Integrantes activos |

Funciones auxiliares:
- `mesDelCiclo(fecha) = (año(fecha) − año(inicio))·12 + mes(fecha) − mes(inicio) + 1`
- `mesesTranscurridos(hoy) = max(0, min(numeroMeses, mesDelCiclo(hoy)))`

### 3.2 Integrantes
Campos: nombre, teléfono, **cuotaMensual**, **tesoreroAsignado**, **nombreCajitaNu** (default `"Natillera - " + nombre`), activo.
- `ahorroEsperado = cuotaMensual × numeroMeses`
- `% del total = ahorroEsperado / Σ ahorroEsperado` (0 si el total es 0)
- `rendimientoProyectadoCajita = cuotaMensual × meses × (meses − 1) / 2 × tasaMensualNu`. Cada cuota gana interés los meses que le quedan al ciclo. Debes advertir en la UI que, si se saca plata de la cajita para préstamos, el rendimiento real baja.

**Funcionalidad clave**: cada integrante puede **proponer o editar su cuota mensual** desde su cuenta mientras el ciclo está en estado `INSCRIPCION`. Después de eso solo el ADMIN la cambia (queda en auditoría).

### 3.3 Aportes (ahorro mensual)
Los aportes **NO se guardan en una tabla aparte**: se derivan del libro de movimientos de tipo `AHORRO` (sección 3.6), que es la fuente única de verdad.
- `aporteReal(integrante, mes) = Σ entradas AHORRO de su cajita con mesDelCiclo(fecha) = mes`
- `totalAportado = Σ aporteReal`
- `diferencia = totalAportado − ahorroEsperado`
- `% cumplimiento = totalAportado / ahorroEsperado`
- `esperadoALaFecha = cuotaMensual × mesesTranscurridos`
- `atraso = max(0, esperadoALaFecha − totalAportado)` → si es > 0 se marca **en rojo**
- Totales por mes: `totalReal(mes)`, `totalEsperado(mes) = Σ cuotas`, y sus acumulados.
- Vista matriz integrantes × 12 meses, igual a la hoja "Ahorros Mensuales".

### 3.4 Préstamos
Campos de entrada: integrante, fechaDesembolso, **monto**, **mesDelPrestamo** (1..12, derivable de la fecha), **numeroCuotas** (1..`maxCuotasPrestamo`), **tasaMensual** (default la del ciclo, editable por préstamo), **cajitaOrigen** (de qué cajita sale el dinero).

Cálculos (interés simple):
- `interesPorCuota = monto × tasaMensual`
- `abonoCapitalPorCuota = monto / numeroCuotas`. **Mejora sobre el Excel**: redondea cada cuota al peso y ajusta el residuo en la **última cuota**, para que Σ capital = monto exacto.
- `valorCuota = interesPorCuota + abonoCapitalPorCuota`
- `totalIntereses = interesPorCuota × numeroCuotas`
- `totalAPagar = monto + totalIntereses`
- Plan de pagos: la cuota k (1..n) vence en el mes `mesDelPrestamo + k`. **La primera cuota se paga el mes siguiente a la entrega.**
- `cuotasPagadas` = número de cuotas con pago registrado. Los pagos son una entidad `PagoCuota` que **genera** un movimiento `PAGO_CUOTA_PRESTAMO`.
- `interesesRecibidos = min(cuotasPagadas, n) × interesPorCuota`
- `saldoPendiente = max(0, totalAPagar − min(cuotasPagadas, n) × valorCuota)`
- `cuotasVencidas = cuotas con mes de vencimiento ≤ mesesTranscurridos y sin pago` → **mora**
- Estado: `PAGADO` si cuotasPagadas ≥ n; `EN_MORA` si cuotasVencidas > 0; si no, `VIGENTE`.

Validaciones (rechazar con 422 y un mensaje claro en español):
- `numeroCuotas > maxCuotasPrestamo` → "Los préstamos no pueden ser a más de 12 cuotas (un año)".
- `mesDelPrestamo + numeroCuotas > numeroMeses` → "El préstamo termina después del cierre de la natillera". El Excel solo advertía; aquí se **bloquea** por defecto, con un flag de configuración `permitirPrestamoDespuesDelCierre=false`.
- `monto > saldo disponible de la cajita origen` → rechazar. El Excel marcaba en rojo la caja negativa; aquí se previene.
- `monto > 0`, integrante activo.

Al crear el préstamo se genera automáticamente el movimiento `DESEMBOLSO_PRESTAMO` (salida) en la cajita origen.

Proyección de intereses por mes (como la grilla del Excel): para cada préstamo y mes m, `interés(m) = interesPorCuota` si `mesDelPrestamo < m ≤ mesDelPrestamo + n`; si no, 0.

### 3.5 Actividades (rifas) — 2 por ciclo
Campos: nombre (ej. "Rifa Televisor 55\"", "Rifa PS5"), fechaSorteo, **valorBoleta**, **boletasPorIntegrante** (default), **costoPremio**, **otrosGastos**, ganador (nombre/número).
- `mesDeLaActividad = mesDelCiclo(fechaSorteo)`
- Por integrante: `boletasAsignadas` (default = boletasPorIntegrante, editable por integrante), `valorAPagar = boletas × valorBoleta`, `pagado` (suma de movimientos `PAGO_BOLETAS_RIFA` asociados a esa actividad e integrante), `pendiente = valorAPagar − pagado`.
- `recaudoEsperado = Σ valorAPagar`; `recaudoReal = Σ pagado`
- `costos = costoPremio + otrosGastos`
- `utilidadEsperada = recaudoEsperado − costos`; `utilidadReal = recaudoReal − costos`
- `utilidadPorIntegrante = utilidad / numeroIntegrantes` → **se reparte en partes IGUALES**.
- Restricción: máximo 2 actividades por ciclo (configurable).

### 3.6 Tesorería — cajitas Nu y libro de movimientos (fuente única de verdad del dinero)
- Hay **una cajita por integrante**, y cada cajita pertenece a la cuenta Nu de **un tesorero**.
- `Movimiento`: fecha, cajita, tipo, descripción, **entrada**, **salida** (exactamente una de las dos > 0), referencia opcional (préstamo, pagoCuota, actividad), creadoPor, creadoEn.
- Tipos (enum): `AHORRO`, `PAGO_CUOTA_PRESTAMO`, `PAGO_BOLETAS_RIFA`, `RENDIMIENTO_NU`, `OTRO_INGRESO` (entradas) · `DESEMBOLSO_PRESTAMO`, `COMPRA_PREMIO`, `GASTO`, `RETIRO_ENTREGA_FINAL` (salidas).
- `tesorero` del movimiento = tesorero asignado a la cajita (derivado, no se digita).
- `saldoCajita = Σ entradas − Σ salidas`
- `saldoTesorero = Σ saldo de sus cajitas`; `nºCajitas por tesorero`.
- **Conciliación**: entidad `SaldoReportadoNu` (cajita, fecha, saldoApp). `diferencia = saldoApp − saldoSegunRegistro` (0 si no hay saldo reportado) → **en rojo** si ≠ 0. Conciliación también por tesorero.
- Resúmenes: por tipo de movimiento (Σ entradas, Σ salidas); por mes (entradas, salidas, saldo acumulado total y por tesorero, al cierre de cada mes).
- Los movimientos **no se borran**: se anulan con un contramovimiento y motivo (auditoría).

### 3.7 Repartición (cierre)
Se calcula en dos versiones: **Proyectada** y **Real a la fecha**.

| Concepto | Proyectado | Real |
|---|---|---|
| Intereses de préstamos | Σ totalIntereses de todos los préstamos | Σ interesesRecibidos |
| (−) Gastos administrativos | config | config |
| **Intereses netos** | `max(0, intereses − gastos)` | ídem |
| Utilidad de actividades | Σ utilidadEsperada | Σ utilidadReal |
| Utilidad de actividades por integrante | `/ numeroIntegrantes` | ídem |

Por integrante:
- `% participación = ahorro / Σ ahorro`. Proyectado usa ahorroEsperado; real usa totalAportado.
- `interesesPréstamos = % participación × interesesNetos` → **quien más ahorra, más recibe** (estrategia proporcional).
- `actividades = utilidadPorIntegrante` → **partes iguales** (estrategia igualitaria).
- `rendimientoCajitaNu`: proyectado = rendimientoProyectadoCajita (3.2); real = Σ movimientos `RENDIMIENTO_NU` de **su** cajita. Cada quien recibe lo que generó su propia cajita.
- `TOTAL A RECIBIR = ahorro + interesesPréstamos + actividades + rendimientoCajitaNu`
- `puesto` = ranking por ahorro (RANK descendente; los empates comparten puesto).
- La UI explica la regla: "Ej.: quien ahorra el 20 % del total recibe el 20 % de los intereses".

### 3.8 Resumen mensual y anual (dashboard)
Para cada mes m = 1..12:
- `ahorroEsperado(m) = Σ cuotas mensuales`
- `ahorroReal(m) = Σ aportes del mes`
- `prestamosEntregados(m) = Σ monto de préstamos con mesDelPrestamo = m`
- `capitalRecuperado(m) = Σ abonoCapitalPorCuota de préstamos con mesDelPrestamo < m ≤ mesDelPrestamo + n` (proyectado; mostrar también el real por pagos)
- `intereses(m)` = proyección de 3.4
- `utilidadActividades(m) = Σ utilidadEsperada de actividades con mesDeLaActividad = m`
- `flujoNeto(m) = ahorroEsperado − prestamosEntregados + capitalRecuperado + intereses + utilidadActividades`
- `cajaDisponibleAcumulada(m) = Σ flujoNeto(1..m)` → **alerta roja si < 0** ("se está prestando más dinero del que hay disponible")
- `fondoTotalAcumulado(m) = Σ(ahorroEsperado + intereses + utilidadActividades)(1..m)`

KPIs anuales: ahorro esperado del año, ahorro real a la fecha, total prestado, intereses proyectados, intereses recibidos, saldo por cobrar de préstamos, utilidad esperada de actividades, rendimiento proyectado de cajitas Nu, total a repartir al cierre (proyectado), **rentabilidad proyectada = (intereses + actividades + rendimiento Nu) / ahorro esperado**, saldo total en cajitas (registro) y diferencia contra la app Nu.

Gráficas: ahorro esperado vs real por mes, caja acumulada, intereses por mes, distribución del reparto.

### 3.9 Casos de referencia (golden tests — deben pasar EXACTOS)
Datos semilla: 10 integrantes con cuotas `[100000, 50000, 200000, 100000, 150000, 50000, 300000, 100000, 80000, 120000]`, 12 meses, tasa 3 %, EA Nu 9 %; los integrantes 1–5 con el Tesorero 1 y los 6–10 con el Tesorero 2.
- Σ cuotas = **1.250.000**/mes; ahorro esperado total = **15.000.000**.
- Préstamo: monto 1.000.000, mes 2, 6 cuotas, 3 % → interés/cuota **30.000**; abono capital **166.666,67**; cuota **196.666,67**; total intereses **180.000**; total a pagar **1.180.000**; con 1 cuota pagada → intereses recibidos **30.000**, saldo **983.333,33**; estado VIGENTE. Intereses proyectados en los meses 3..8.
- Repartición proyectada: integrante 1 (ahorro 1.200.000) → 8,0 % → intereses **14.400**; integrante 7 (3.600.000) → 24 % → **43.200**.
- Rendimiento proyectado de la cajita del integrante 1 (cuota 100.000, 12 meses, EA 9 %) ≈ **47.568,33**; total del grupo ≈ **594.604,17**.
- Rifa 1: boleta 10.000 × 20 × 10 = 2.000.000 − (1.800.000 + 50.000) = **150.000**. Rifa 2: boleta 15.000 × 20 × 10 = 3.000.000 − (2.400.000 + 50.000) = **550.000**. Total **700.000** → **70.000** por integrante.
- Resumen: caja acumulada mes 1 = 1.250.000; mes 2 = 1.500.000 (entra 1.250.000 de ahorro y salen 1.000.000 del préstamo).

---

## 4. Módulo de reportes

Endpoints bajo `/api/reportes/**`. Todos respetan el **alcance del rol** (un integrante solo obtiene su propio estado de cuenta).

1. **Consolidado por integrante**: nombre, tesorero, cuota, ahorro esperado, total aportado, esperado a la fecha, atraso, % cumplimiento, saldo de su cajita, préstamos vigentes (nº), saldo de la deuda de préstamos, cuotas vencidas, boletas pendientes ($), intereses proyectados a recibir, total a recibir (proyectado y real).
2. **¿Quién debe y cuánto?** (deudores): lista de integrantes con deuda > 0, desglosada en (a) **atraso de aportes**, (b) **saldo de préstamos** (y cuánto está vencido), (c) **boletas sin pagar**; con columna total y orden descendente. Filtros: tesorero, tipo de deuda, solo en mora.
3. **Estado de cuenta individual**: detalle de un integrante con todos sus movimientos, plan de pagos de sus préstamos y su proyección de repartición.
4. **Tesorería**: saldos por cajita y por tesorero, conciliación con Nu y movimientos por rango de fechas y tipo.
5. **Resumen mensual/anual** (3.8).

Exportación de cada reporte a **Excel (.xlsx con Apache POI)** y **CSV**. Opcional: versión imprimible en el frontend (CSS print).

**Rendimiento**: los reportes se calculan con **consultas SQL agregadas** (vistas o consultas nativas con `GROUP BY` y proyecciones DTO), no cargando entidades en memoria. Agrega índices en `movimiento(cajita_id, fecha)`, `movimiento(tipo)`, `prestamo(integrante_id)`, `pago_cuota(prestamo_id)`.

---

## 5. Arquitectura del backend

**Monolito modular** con **arquitectura hexagonal (puertos y adaptadores)** por módulo:

```
backend/src/main/java/co/natillera/
├── shared/            (dinero, errores, auditoría, utilidades de fechas del ciclo)
├── auth/              (usuarios, roles, JWT, refresh tokens, AccessScopeService)
├── ciclo/             (configuración)
├── integrantes/
├── tesoreria/         (cajitas, movimientos, conciliación)
├── prestamos/         (préstamos, plan de pagos, pagos de cuota)
├── actividades/       (rifas, boletas)
├── reparticion/       (estrategias de reparto)
├── resumen/           (dashboard mensual/anual)
└── reportes/
    cada módulo:
    ├── domain/        (entidades y servicios de dominio PUROS, sin Spring)
    ├── application/   (casos de uso, puertos in/out, DTOs)
    └── infrastructure/(controladores REST, adaptadores JPA, mappers)
```

Patrones que **deben** aplicarse (y documentarse en ARQUITECTURA.md con el archivo donde se usan):
- **Strategy**: `EstrategiaReparto` → `RepartoProporcionalAlAhorro` (intereses) y `RepartoIgualitario` (rifas).
- **Repository / Ports & Adapters**: puertos de salida en `application`, adaptadores JPA en `infrastructure`.
- **Factory**: `MovimientoFactory` crea los movimientos derivados (desembolso, pago de cuota, pago de boletas).
- **Domain Events** (con `ApplicationEventPublisher`, en memoria): `PrestamoCreado` → movimiento de desembolso; `CuotaPagada` → movimiento de entrada.
- **Specification** o **Policy**: `AccessScopeService`, `PrestamoAccessPolicy` y validaciones de préstamo (`ReglaPrestamo` encadenables: máximo de cuotas, fin del ciclo, saldo de la cajita).
- **DTO + Mapper** (MapStruct) y **Value Object** `Dinero` (envuelve BigDecimal, con suma, resta, multiplicación y redondeo).
- **Template Method** o **Builder** para la generación de reportes en Excel y CSV.
- Manejo de errores centralizado con `@RestControllerAdvice` y respuestas **RFC 7807 (ProblemDetail)** en español.

API REST versionada `/api/v1`, documentada con OpenAPI (Swagger UI solo en el perfil `dev`).
Auditoría: `creadoPor`, `creadoEn`, `modificadoPor`, `modificadoEn` en todas las entidades (Spring Data Auditing) y una tabla `auditoria` para los cambios sensibles (cuotas, tasas, anulaciones).
Migraciones con **Flyway** (`V1__esquema.sql`, `V2__seed_dev.sql` solo para dev).
Seed de desarrollo: 1 admin, 2 tesoreros y 10 integrantes con los datos de la sección 3.9, más el préstamo y las 2 rifas de ejemplo.

---

## 6. Frontend (Next.js)

- **App Router**, TypeScript estricto, Tailwind. Componentes de servidor donde sea posible; componentes de cliente solo para formularios y gráficas (Recharts).
- **BFF**: route handlers en `/app/api/**` (auth y proxy), con `middleware.ts` que protege rutas por rol leyendo un claim no sensible (el rol) de una cookie firmada. **La autorización real siempre está en el backend.**
- Layout responsive (**mobile-first**: la familia lo usará desde el celular).
- Pantallas:
  - Públicas: login, cambio de contraseña.
  - **Integrante**: "Mi cuenta": mi cuota, mi ahorro (esperado vs real por mes), mi atraso, el saldo de mi cajita, mis préstamos con su plan de pagos, mis boletas, mi repartición proyectada y real, y mi posición.
  - **Tesorero**: mis cajitas (saldos y conciliación con Nu), registrar movimiento, aportes del mes (registro rápido en lote para sus integrantes), todos los préstamos, registrar pago de cuota, deudores de sus integrantes.
  - **Admin**: dashboard (3.8), configuración del ciclo, usuarios y roles, integrantes y asignación de tesorero, préstamos, actividades, tesorería global, repartición, reportes, auditoría.
- Estados vacíos, carga (skeletons) y errores con mensajes del ProblemDetail. Formato de dinero `Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })`.
- Alertas visuales iguales a las del Excel: atraso en rojo, diferencia de conciliación en rojo, caja acumulada negativa en rojo, préstamo PAGADO en verde.
- Accesibilidad básica (labels, contraste, navegación con teclado).
- Usa la ilustración `natillera_familiar.svg` (la incluyo en el repo) en el login y en el encabezado. Lema: "Ahorramos juntos, crecemos juntos".

---

## 7. Eficiencia de recursos en el despliegue (prioridad alta)

El backend y PostgreSQL deben caber cómodamente en los **US$5/mes de Railway Hobby**.

**Backend / JVM**
- Dockerfile **multi-stage**: build con Maven y JDK 21 → runtime `eclipse-temurin:21-jre-alpine`, jar en **capas** (layertools), usuario no root.
- `JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -XX:TieredStopAtLevel=1"` (documentar el trade-off de `TieredStopAtLevel`). Evaluar **CDS** (`-XX:SharedArchiveFile`) para acelerar el arranque.
- `spring.threads.virtual.enabled=true`; `server.tomcat.threads.max=20`.
- HikariCP: `maximum-pool-size=5`, `minimum-idle=1`, `idle-timeout=60s`.
- `spring.jpa.open-in-view=false`, `spring.main.lazy-initialization=true` en prod (medir el arranque), `ddl-auto=validate`, consultas con proyecciones, sin N+1 (verificar con `@EntityGraph` o fetch joins).
- Actuator: solo `health` (para el healthcheck de Railway). Logs en formato JSON y nivel INFO.
- Compatible con **Railway Serverless (app sleeping)**: sin tareas `@Scheduled` que mantengan despierto el servicio; el arranque en frío debe ser < 10 s (medirlo y documentarlo).
- Conexión al PostgreSQL de Railway por la **red privada** (`DATABASE_URL` interna). Backups habilitados.

**Frontend (Vercel Hobby, uso no comercial)**
- Páginas estáticas donde aplique; cache de TanStack Query; imágenes en SVG; sin dependencias pesadas innecesarias.
- No incluir pagos en línea ni publicidad (el plan Hobby es para uso no comercial).

**Base de datos**
- Tipos ajustados, índices de la sección 4 y `VACUUM`/estadísticas por defecto. Sin tablas de log enormes: la auditoría guarda solo los cambios sensibles.

**Configuración por variables de entorno** (12-factor): `DATABASE_URL`, `JWT_SECRET`, `CORS_ALLOWED_ORIGIN`, `FRONTEND_URL`, `SPRING_PROFILES_ACTIVE`, `BACKEND_URL` (para Next.js). Incluye `.env.example` en ambos proyectos.

---

## 8. Seguridad adicional
- Validación de entrada con Bean Validation y Zod en ambos lados.
- Headers de seguridad (HSTS, X-Content-Type-Options, frame-ancestors) en Spring y en `next.config`.
- Nunca registrar en logs contraseñas, tokens ni saldos completos asociados a nombres.
- Dependencias sin vulnerabilidades críticas (OWASP Dependency-Check o `npm audit` en CI).
- CI con GitHub Actions: build, pruebas (incluye Testcontainers) y lint.

---

## 9. Documentos a generar

### 9.1 `test.md` — catálogo completo de pruebas
Debe contener, en tablas con columnas **ID | Módulo | Caso | Precondición | Pasos | Resultado esperado | Tipo (Unit/Integración/E2E/Manual) | Automatizada (sí/no + ruta del test)**:
1. **Lógica de negocio (unitarias)**: cada fórmula de la sección 3, incluidos los golden tests de 3.9, bordes (0 integrantes, ahorro total 0, cuota 0, préstamo de 1 y de 12 cuotas, residuo de redondeo, préstamo que termina en el mes 12, empates en el ranking, EA 0 %).
2. **Validaciones**: más de 12 cuotas, fin después del cierre, monto mayor al saldo de la cajita, monto ≤ 0, más de 2 rifas, movimiento con entrada y salida a la vez.
3. **Seguridad y autorización**: **matriz completa rol × endpoint** (permitido/denegado), IDOR (un integrante pide los datos de otro → 404), tesorero que intenta ver la cajita de un integrante no asignado → 404, token expirado, refresh rotado y reutilizado (se revoca la familia), rate limit de login, CORS, contraseña débil, cambio de contraseña obligatorio.
4. **Integración (API + PostgreSQL con Testcontainers)**: flujos completos — crear préstamo → movimiento de desembolso → pagar cuotas → estado PAGADO; registrar aportes → atraso; rifas → utilidad; repartición proyectada vs real; conciliación con Nu.
5. **Reportes**: consolidado por integrante, deudores (los tres tipos de deuda), estado de cuenta, exportación a Excel y CSV (contenido y formato), alcance por rol.
6. **E2E (Playwright)**: login por rol y navegación visible por rol; el integrante solo ve "Mi cuenta"; el tesorero registra un aporte en lote; el admin crea un préstamo y ve la alerta de caja negativa.
7. **Frontend (unitarias)**: formateo de COP, formularios con Zod, guardas de rutas.
8. **No funcionales**: arranque en frío < 10 s, memoria del contenedor < 400 MB en reposo, p95 de reportes < 500 ms con datos semilla, revisión de N+1.
9. **Regresión contra el Excel**: procedimiento manual para cargar los mismos datos en el Excel y en la web y comparar cada hoja (Integrantes, Ahorros Mensuales, Tesorería, Préstamos, Actividades, Repartición, Resumen).
10. **Checklist de despliegue**: variables de entorno, migraciones, healthcheck, backups, dominio y CORS.

Al final de `test.md`, una tabla de **trazabilidad** requisito → IDs de prueba, y la evidencia de la última ejecución (resumen de `mvn verify`, `npm test` y Playwright).

### 9.2 `ARQUITECTURA.md` — documento de explicación
Debe explicar **lo que se desarrolló**:
1. Resumen funcional (qué hace el sistema, por rol).
2. **Arquitectura**: diagrama de contexto y de contenedores (Vercel/Next.js BFF → Spring Boot en Railway → PostgreSQL) y diagrama de componentes por módulo, en **Mermaid**. Explica por qué se eligió un monolito modular hexagonal y no microservicios (equipo de 1 persona, 10 usuarios, costo).
3. **Modelo de datos**: diagrama ER en Mermaid y descripción de cada tabla.
4. **Flujo de autenticación** (diagrama de secuencia: login, refresh con rotación, proxy BFF) y modelo de autorización (alcances OWN/ASSIGNED/ALL).
5. **Patrones de diseño** usados: para cada uno, qué problema resuelve, dónde está (ruta del archivo) y un fragmento de código breve.
6. **Lógica de negocio**: tabla de equivalencia **hoja/columna del Excel → clase/método** del sistema.
7. **Decisiones técnicas (ADRs breves)**: libro de movimientos como fuente única de verdad, BigDecimal y redondeo, BFF con cookies httpOnly, bloqueo de préstamos posteriores al cierre, ajuste de la última cuota.
8. **Optimización de recursos**: configuración de JVM, pool, Docker y sleeping, con las mediciones reales (memoria, arranque).
9. Despliegue paso a paso (Railway + Vercel) y operación (backups, rotación del JWT_SECRET, cierre de ciclo e inicio de uno nuevo).
10. Limitaciones conocidas y mejoras futuras (notificaciones por WhatsApp de cuotas vencidas, varios ciclos, PWA).

---

## 10. Forma de trabajo esperada
1. Antes de escribir código, presenta un **plan corto** (módulos, esquema de BD y endpoints) y la lista de supuestos.
2. Implementa **primero el dominio con los golden tests de 3.9 en verde**, luego persistencia, API, seguridad, frontend y reportes.
3. Ejecuta todas las pruebas y corrige hasta que pasen. No marques nada como terminado con pruebas fallando.
4. Genera `test.md`, `ARQUITECTURA.md` y `README.md` **a partir de lo realmente implementado** (rutas y nombres reales, no genéricos).
5. Al terminar, entrega un resumen con: qué quedó listo, cómo correrlo localmente (`docker compose up`), credenciales de seed de dev y pasos para desplegar.
