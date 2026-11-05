# Natillera Familiar — Plan y catálogo de pruebas

Rutas de prueba relativas al repositorio. Abreviaturas de ruta: `BT` = `backend/src/test/java/co/natillera`,
`FE` = `frontend`. Datos de las pruebas de integración y E2E: seed dev (`V2__seed_dev.sql`) con fecha simulada
2027-03-20 (mes 3 del ciclo).

| Tipo | Herramienta | Cómo correr |
|---|---|---|
| Unit (dominio y aplicación) | JUnit 5, AssertJ, Mockito | `cd backend && ./mvnw test` |
| Integración (API + PostgreSQL 16 real) | Spring Boot Test, MockMvc, Spring Security Test, Testcontainers (o PostgreSQL embebido sin Docker) | `cd backend && ./mvnw verify` |
| Frontend (unit) | Vitest + Testing Library | `cd frontend && npm test` |
| E2E | Playwright (Pixel 7 y Chrome escritorio) | `docker compose up -d && cd frontend && npx playwright test` |

---

## 1. Lógica de negocio (unitarias)

| ID | Módulo | Caso | Precondición | Pasos | Resultado esperado | Tipo | Automatizada |
|---|---|---|---|---|---|---|---|
| UN-01 | integrantes | Σ cuotas y ahorro esperado total (golden) | 10 cuotas de la §3.9 | Sumar cuotas y `ahorroEsperado(c,12)` | 1.250.000 / 15.000.000 | Unit | Sí · `BT/golden/GoldenExcelTest#sumaDeCuotasYAhorroEsperadoTotal` |
| UN-02 | prestamos | Columnas del préstamo (golden) | 1.000.000, mes 2, 6 cuotas, 3 % | Calcular | 30.000; 166.666,67; 196.666,67; 180.000; 1.180.000 | Unit | Sí · `GoldenExcelTest$PrestamoGolden#columnasCalculadas` |
| UN-03 | prestamos | 1 cuota pagada (golden) | Ídem | `resumir(p,[1],3)` | Intereses 30.000, saldo 983.333,33, VIGENTE | Unit | Sí · `GoldenExcelTest$PrestamoGolden#conUnaCuotaPagada` |
| UN-04 | prestamos | Proyección de intereses meses 3..8 (golden) | Ídem | `proyeccionIntereses(p,12)` | 30.000 en 3..8, 0 en el resto | Unit | Sí · `GoldenExcelTest$PrestamoGolden#interesesProyectadosEnLosMeses3a8` |
| UN-05 | reparticion | Repartición proyectada (golden) | Intereses 180.000, rifas 700.000 | `calcular` | Int. 1: 8 % → 14.400; int. 7: 24 % → 43.200; 70.000 c/u; puesto 1 = int. 7 | Unit | Sí · `GoldenExcelTest#reparticionProyectada` |
| UN-06 | integrantes | Rendimiento Nu (golden) | EA 9 % | `rendimientoProyectadoCajita` | 47.568,33 y total 594.604,17 | Unit | Sí · `GoldenExcelTest#rendimientoCajitasNu` |
| UN-07 | actividades | Rifas (golden) | Rifas del Excel | `resumir` | 150.000 + 550.000 = 700.000; 70.000 c/u; meses 6 y 11 | Unit | Sí · `GoldenExcelTest#rifas` |
| UN-08 | resumen | Caja acumulada (golden) | Préstamo golden | `CalculadoraResumen.calcular` | Mes 1 = 1.250.000; mes 2 = 1.500.000; sin alerta | Unit | Sí · `GoldenExcelTest#resumenCajaAcumulada` |
| UN-09 | shared | mesDelCiclo / mesesTranscurridos (antes, durante y después del ciclo) | Inicio 2027-01-01 | Varias fechas | 2, 13, 0, 3, 12 | Unit | Sí · `BT/domain/BordesDominioTest#mesDelCicloYMesesTranscurridos` |
| UN-10 | ciclo | Fecha de cierre y tasa mensual Nu | Ciclo por defecto | Calcular | 2027-12-01; 0,0072073233 | Unit | Sí · `BordesDominioTest#fechaDeCierreYTasaMensualNu` |
| UN-11 | ciclo | Más de 12 meses | — | `new Ciclo(…13…)` | ReglaNegocioException | Unit | Sí · `BordesDominioTest#cicloRechazaMasDe12Meses` |
| UN-12 | integrantes | EA 0 % | Tasa 0 | Rendimiento | 0 | Unit | Sí · `BordesDominioTest#tasaEACeroDaRendimientoCero` |
| UN-13 | integrantes | Ahorro total 0 | Total 0 | `porcentajeDelTotal` | 0 (sin división por cero) | Unit | Sí · `BordesDominioTest#porcentajeConTotalCeroEsCero` |
| UN-14 | integrantes | Cuota 0 | Cuota 0, 5 meses | `estado` | Atraso 0, cumplimiento 0 | Unit | Sí · `BordesDominioTest#cuotaCeroNoGeneraAtrasoNiDivisionPorCero` |
| UN-15 | integrantes | Atraso, diferencia y % cumplimiento | Cuota 100.000, aportes 100.000 + 50.000, mes 3 | `estado` | Total 150.000, esperado a la fecha 300.000, atraso 150.000, dif. −1.050.000, 12,5 % | Unit | Sí · `BordesDominioTest#atrasoAportes` |
| UN-16 | integrantes | Totales por mes y acumulados | 2 integrantes | `totalesPorMes` | Reales, esperados y acumulados correctos | Unit | Sí · `BordesDominioTest#totalesPorMesYAcumulados` |
| UN-17 | reparticion | 0 integrantes | Lista vacía | `calcular` | Sin filas; por integrante 0 | Unit | Sí · `BordesDominioTest#reparticionSinIntegrantes` |
| UN-18 | reparticion | Ahorro total 0 | Un integrante con ahorro 0 | `calcular` | Intereses 0 | Unit | Sí · `BordesDominioTest#reparticionConAhorroTotalCeroNoReparteIntereses` |
| UN-19 | reparticion | Gastos > intereses | Gastos 5.000 | `calcular` | Intereses netos 0 (`max(0,…)`) | Unit | Sí · `BordesDominioTest#gastosMayoresQueInteresesDanInteresesNetosCero` |
| UN-20 | reparticion | Empates en el ranking | Ahorros 100, 300, 100, 50 | `calcular` | Puestos 2, 1, 2, 4 | Unit | Sí · `BordesDominioTest#empatesEnElRankingCompartenPuesto` |
| UN-21 | prestamos | Plan suma exacto el monto (1, 3, 6, 7, 11 cuotas) | 1.000.000 | `planDePagos` | Σ capital = 1.000.000; cuotas al peso | Unit | Sí · `BT/prestamos/domain/CalculadoraPrestamoTest#planDePagosSumaExactamenteElMontoConResiduoEnLaUltimaCuota` |
| UN-22 | prestamos | Residuo de redondeo en la última cuota | 1.000.000 / 6 | `planDePagos` | 166.667 × 5 y 166.665; vencen meses 3..8 | Unit | Sí · `CalculadoraPrestamoTest#residuoDeRedondeoVaEnLaUltimaCuota` |
| UN-23 | prestamos | Préstamo de 1 cuota | 500.000, mes 4 | `resumir` | Cuota 515.000, VIGENTE | Unit | Sí · `CalculadoraPrestamoTest#prestamoDeUnaCuota` |
| UN-24 | prestamos | Préstamo que termina en el mes 12 | Mes 6, 6 cuotas | Validar y planear | Válido; última cuota mes 12 | Unit | Sí · `CalculadoraPrestamoTest#prestamoQueTerminaJustoEnElMes12EsValido` |
| UN-25 | prestamos | Préstamo de 12 cuotas desde el mes 1 | Mes 1, 12 cuotas | Validar | "termina después del cierre" | Unit | Sí · `CalculadoraPrestamoTest#prestamoDeDoceCuotasDesdeElMes1TerminaDespuesDelCierre` |
| UN-26 | prestamos | Mora y saldo vencido | 1 de 6 pagada, mes 5 | `resumir` | 2 vencidas, EN_MORA, 393.334 vencido | Unit | Sí · `CalculadoraPrestamoTest#enMoraCuandoHayCuotasVencidasSinPago` |
| UN-27 | prestamos | Pagado | 6 de 6 | `resumir` | PAGADO, saldo 0, intereses 180.000 | Unit | Sí · `CalculadoraPrestamoTest#pagadoCuandoTodasLasCuotasTienenPago` |
| UN-28 | tesoreria | Conciliación sin saldo reportado / con diferencia | — | `diferencia` | 0 / −50 | Unit | Sí · `BordesDominioTest#conciliacionSinSaldoReportadoEsCero` |
| UN-29 | tesoreria | Saldos por tesorero y por mes acumulado | 3 cajitas, 2 tesoreros | `porTesorero`, `porMes` | 2 cajitas / 250 / −50; acumulados 270 y 200 | Unit | Sí · `BordesDominioTest#saldosPorTesoreroYPorMes` |
| UN-30 | shared | Redondeo HALF_UP e igualdad de Dinero | — | Redondear | 0,005→0,01; 1.250.000,5→1.250.001; 10 = 10,00 | Unit | Sí · `BordesDominioTest#dineroRedondeaHalfUp` |
| UN-31 | reportes | Deudores: orden, filtros por tipo/tesorero/mora | 3 deudores | `filtrarYOrdenar` | B, A; filtros correctos | Unit | Sí · `BT/infraestructura/InfraestructuraTest#deudoresSeOrdenanPorTotalYFiltranPorTipoTesoreroYMora` |

## 2. Validaciones

| ID | Módulo | Caso | Precondición | Pasos | Resultado esperado | Tipo | Automatizada |
|---|---|---|---|---|---|---|---|
| VA-01 | prestamos | Más de 12 cuotas | — | Validar 13 cuotas (dominio) y POST /prestamos | 422 "Los préstamos no pueden ser a más de 12 cuotas (un año)" | Unit + Integración + E2E | Sí · `CalculadoraPrestamoTest#masDeDoceCuotasSeRechaza`, `BT/flujos/FlujosNegocioIT#validacionesDePrestamo`, `FE/e2e/natillera.spec.ts` "validación de préstamos" |
| VA-02 | prestamos | Fin después del cierre | Mes 3, 10 cuotas | POST | 422 "El préstamo termina después del cierre de la natillera" | Integración | Sí · `FlujosNegocioIT#validacionesDePrestamo` |
| VA-03 | prestamos | Flag `permitirPrestamoDespuesDelCierre` | Flag activo | Validar mes 10 + 6 | Válido | Unit | Sí · `CalculadoraPrestamoTest#flagPermiteTerminarDespuesDelCierre` |
| VA-04 | prestamos | Monto mayor al saldo de la cajita | Cajita 3 ≈ 801.441 | POST 50.000.000 | 422 "El monto supera el saldo disponible…" | Unit + Integración | Sí · `CalculadoraPrestamoTest#montoMayorAlSaldoDeLaCajitaSeRechaza`, `FlujosNegocioIT#validacionesDePrestamo` |
| VA-05 | prestamos | Monto ≤ 0 | — | POST 0 y −5 | 422 "El monto del préstamo debe ser mayor que cero" | Unit + Integración | Sí · `CalculadoraPrestamoTest#montoNoPositivoSeRechaza`, `FlujosNegocioIT#validacionesDePrestamo` |
| VA-06 | prestamos | Integrante inactivo | — | Validar | "El integrante no está activo" | Unit | Sí · `CalculadoraPrestamoTest#integranteInactivoSeRechaza` |
| VA-07 | actividades | Más de 2 rifas | 2 rifas en el seed | POST /actividades | 422 "El ciclo admite máximo 2 actividades" | Unit + Integración | Sí · `BordesDominioTest#masDeDosActividadesSeRechaza`, `FlujosNegocioIT#rifas` |
| VA-08 | tesoreria | Movimiento con entrada y salida a la vez | — | `new Movimiento(…1,1…)` | ReglaNegocioException "no ambas" | Unit | Sí · `BordesDominioTest#movimientoConEntradaYSalidaALaVezSeRechaza` |
| VA-09 | tesoreria | Lado contrario al tipo | AHORRO como salida | Construir | "es de entrada" | Unit | Sí · `BordesDominioTest#movimientoDeSalidaConTipoDeEntradaSeRechaza` |
| VA-10 | tesoreria | Tipos derivados no se digitan | DESEMBOLSO a mano | Factory / POST | 422 | Unit + Integración | Sí · `BordesDominioTest#tiposDerivadosNoSeRegistranAMano`, `FlujosNegocioIT#anulacionDeMovimiento` |
| VA-11 | tesoreria | Salida mayor que el saldo | GASTO 99.000.000 | POST | 422 "La salida supera el saldo de la cajita" | Integración | Sí · `FlujosNegocioIT#anulacionDeMovimiento` |
| VA-12 | tesoreria | Anulación sin motivo / doble anulación / anular una anulación | — | Factory y POST | Rechazo | Unit + Integración | Sí · `BordesDominioTest#contramovimientoInvierteElLadoYExigeMotivo`, `FlujosNegocioIT#anulacionDeMovimiento` |
| VA-13 | integrantes | Cuota propia fuera de inscripción | Ciclo ACTIVO | PUT /integrantes/me/cuota | 422 | Integración | Sí · `FlujosNegocioIT#cuotaPropia` |
| VA-14 | frontend | Validación Zod de formularios (cuotas > máx., montos, aportes, contraseñas, usuario) | — | `safeParse` | Mensajes en español | Unit FE | Sí · `FE/src/lib/esquemas.test.ts` |
| VA-15 | api | Formato de exportación inválido | — | `?formato=pdf` | 422 | Integración | Sí · `BT/reportes/ReportesIT#exportacionDelResumenYTesoreria` |

## 3. Seguridad y autorización

| ID | Módulo | Caso | Precondición | Pasos | Resultado esperado | Tipo | Automatizada |
|---|---|---|---|---|---|---|---|
| SE-01 | todos | **Matriz rol × endpoint** (30 endpoints × INTEGRANTE/TESORERO/ADMIN) | Seed | Llamar cada endpoint con cada rol | Estado esperado por celda (ver tabla 3.1) | Integración | Sí · `BT/seguridad/SeguridadIT#matrizRolPorEndpoint` (30 casos) |
| SE-02 | todos | Sin token | — | Los 30 endpoints sin Authorization | 401 | Integración | Sí · `SeguridadIT#sinTokenEs401` (30 casos) |
| SE-03 | auth | IDOR: integrante pide datos de otro | integrante 3 | GET /integrantes/7, /integrantes/7/aportes, /reportes/estado-cuenta/7 | 404 | Integración + E2E | Sí · `SeguridadIT$Idor#integranteNoVeDatosDeOtro`, e2e "IDOR → 404" |
| SE-04 | prestamos | Integrante solo ve sus préstamos | integrante 7 | GET /prestamos/1 | 404; lista vacía | Integración | Sí · `SeguridadIT$Idor#integranteSoloVeSusPrestamos` |
| SE-05 | tesoreria | Tesorero pide cajita/integrante no asignado | tesorero 1 | GET /integrantes/7, movimientos?cajitaId=7, POST movimiento y saldo en cajita 7 | 404 | Integración | Sí · `SeguridadIT$Idor#tesoreroNoVeIntegrantesNoAsignados` |
| SE-06 | prestamos | Tesorero ve todos los préstamos pero no cobra en cajita ajena | tesorero 2 | GET /prestamos/1; pagar a cajita 3 | 200 / 404 | Integración | Sí · `SeguridadIT$Idor#tesoreroVeTodosLosPrestamosPeroNoCobraEnCajitasAjenas` |
| SE-07 | auth | Listados filtrados por alcance | — | GET /integrantes, /tesoreria/cajitas, /reparticion | 1 fila / cajitas 1–5 / fila propia sin conceptos | Integración | Sí · `SeguridadIT$Idor#listadosFiltradosPorAlcance` |
| SE-08 | auth | Alcances OWN/ASSIGNED/ALL y 404 vs 403 | Mocks | `AccessScopeService` | Correctos | Unit | Sí · `BT/auth/application/AccessScopeServiceTest` (5) |
| SE-09 | auth | Token expirado | JWT emitido hace 1 h | GET /ciclo | 401 tipo `token-expirado` | Integración | Sí · `SeguridadIT$Tokens#tokenExpiradoResponde401ConTipoTokenExpirado` |
| SE-10 | auth | Token firmado con otra clave / malformado | — | GET /ciclo | 401 | Integración | Sí · `SeguridadIT$Tokens#tokenAlteradoOFirmadoConOtraClaveEs401` |
| SE-11 | auth | Refresh rotado y reutilizado | Login | refresh(r1) → r2; refresh(r1); refresh(r2) | 200; 401; 401 (familia revocada) | Unit + Integración | Sí · `SeguridadIT$Tokens#refreshRotaYLaReutilizacionRevocaLaFamilia`, `AuthServiceTest#refreshReutilizadoRevocaLaFamilia` |
| SE-12 | auth | Refresh hasheado, expirado, rotación en la misma familia | Mocks | — | Solo se guarda SHA-256; expirado rechazado | Unit | Sí · `AuthServiceTest` |
| SE-13 | auth | Logout revoca el refresh | Login | POST /logout; refresh | 204; 401 | Integración + E2E | Sí · `SeguridadIT$Tokens#logoutRevocaElRefreshToken`, e2e "cerrar sesión" |
| SE-14 | auth | Mensaje de login no revela si el usuario existe | — | Login con usuario real e inexistente | 401 mismo mensaje | Integración + E2E | Sí · `SeguridadIT$Tokens#loginIncorrectoNoRevelaSiElUsuarioExiste` |
| SE-15 | auth | Rate limit de login por IP y usuario | — | 5 fallos y 6.º intento; luego desde otra IP | 429 + Retry-After en ambos | Unit + Integración | Sí · `SeguridadIT$Protecciones#rateLimitDeLoginPorIpYUsuario`, `AuthServiceTest#rateLimitTrasCincoFallos`, `#ventanaDelRateLimitExpira` |
| SE-16 | auth | CORS | — | Preflight desde localhost:3000 y desde otro origen | 200 con ACAO / 403 | Integración | Sí · `SeguridadIT$Protecciones#corsSoloAceptaElOrigenDelFrontend` |
| SE-17 | auth | Headers de seguridad | HTTPS | GET /actuator/health | nosniff, DENY, HSTS, frame-ancestors | Integración | Sí · `SeguridadIT$Protecciones#headersDeSeguridad` |
| SE-18 | auth | Contraseña débil y cambio obligatorio en el primer ingreso | Usuario creado por admin | Login, GET protegido, cambiar con claves débiles y válida | 403 `cambio-password-obligatorio`; 422 ×3; 200; luego 200 | Integración | Sí · `SeguridadIT$Protecciones#cambioDePasswordObligatorioEnPrimerIngresoYPoliticaDeContrasena` |
| SE-19 | frontend | Guardas de rutas por rol | — | `rutaPermitida`, `inicioPara`, `menuPara` | Integrante solo /mi-cuenta y /actividades | Unit FE + E2E | Sí · `FE/src/lib/rutas.test.ts`, e2e "el integrante solo ve Mi cuenta" |
| SE-20 | frontend | Cookie de sesión firmada no falsificable | — | Alterar payload / otra clave | `null` | Unit FE | Sí · `FE/src/lib/sesion.test.ts` |
| SE-21 | frontend | Cookies httpOnly + SameSite=Lax | Login | Inspeccionar cookies | `nat_at` httpOnly, Lax | E2E | Sí · e2e "cerrar sesión borra las cookies" |
| SE-22 | reportes | CSV injection | Celda `=HYPERLINK(...)` | Exportar CSV | Celda prefijada con `'` | Unit | Sí · `InfraestructuraTest#csvFormateaDineroPorcentajeYNeutralizaFormulas` |
| SE-23 | frontend | Redirección abierta en `?volver=` | — | `/login?volver=https://otro` | Ignorado, va al inicio del rol | Manual | No |
| SE-24 | auth | Recuperar contraseña por correo | Integrante 3 | POST /auth/recuperar-password, abrir enlace, POST /auth/restablecer-password | 202; 204; login con la nueva, la anterior 401; el enlace no sirve dos veces (422) | Integración | Sí · `BT/seguridad/RecuperacionPasswordIT#elEnlaceDelCorreoPermiteCambiarLaContrasenaUnaSolaVez` |
| SE-25 | auth | Recuperación no revela cuentas | Correo inexistente | POST /auth/recuperar-password | 202 igual, sin correo enviado | Integración | Sí · `RecuperacionPasswordIT#correoInexistenteRespondeIgualYNoEnviaNada` |
| SE-26 | auth | Enlace anterior, inventado o clave débil | — | Pedir dos enlaces; token falso; clave "corta1" | 422; la clave actual sigue sirviendo | Integración | Sí · `RecuperacionPasswordIT#pedirUnEnlaceNuevoInvalidaElAnterior`, `#tokenInventadoYContrasenaDebilSeRechazan` |
| SE-27 | auth | Rate limit de solicitudes de recuperación | — | 6 solicitudes en 15 min | La 6.ª responde 429 | Integración | Sí · `RecuperacionPasswordIT#demasiadasSolicitudesResponden429` |

### 3.1 Matriz rol × endpoint (automatizada en `SeguridadIT#matrizRolPorEndpoint`)

| Endpoint | INTEGRANTE | TESORERO | ADMIN |
|---|---|---|---|
| GET /ciclo | 200 | 200 | 200 |
| PUT /ciclo · PUT /ciclo/estado | 403 | 403 | 200 |
| GET /usuarios | 403 | 403 | 200 |
| GET /integrantes · GET /integrantes/aportes | 200 (propio) | 200 (asignados) | 200 |
| POST /integrantes | 403 | 403 | 201 |
| PUT /integrantes/me/cuota (ciclo ACTIVO) | 422 | 422 | 422 |
| GET /tesoreria/cajitas · /tesoreria/resumen | 200 (propia) | 200 | 200 |
| POST /tesoreria/movimientos · /aportes-lote | 403 | 201 | 201 |
| POST /tesoreria/cajitas/{id}/saldo-reportado | 403 | 204 | 204 |
| GET /prestamos · /prestamos/proyeccion-intereses | 200 (suyos) | 200 (todos) | 200 |
| POST /prestamos/simular | 403 | 200 | 200 |
| POST /prestamos · /prestamos/{id}/pagos | 403 | 201 | 201 |
| GET /actividades | 200 (sus boletas) | 200 | 200 |
| POST /actividades (cupo lleno) | 403 | 403 | 422 |
| PUT /actividades/{id}/boletas/{i} | 403 | 403 | 200 |
| POST /actividades/{id}/pagos | 403 | 201 | 201 |
| GET /reparticion | 200 (su fila) | 200 | 200 |
| GET /resumen · /reportes/resumen | 403 | 403 | 200 |
| GET /reportes/consolidado · /deudores · /tesoreria | 403 | 200 | 200 |
| GET /reportes/estado-cuenta/3 | 200 | 200 | 200 |
| GET /auditoria | 403 | 403 | 200 |

## 4. Integración (API + PostgreSQL)

| ID | Módulo | Caso | Precondición | Pasos | Resultado esperado | Tipo | Automatizada |
|---|---|---|---|---|---|---|---|
| IN-01 | prestamos | Golden desde la base de datos | Seed | GET /prestamos/1 | Valores del golden; VIGENTE | Integración | Sí · `FlujosNegocioIT#prestamoSeedCoincideConElExcel` |
| IN-02 | prestamos | Crear → desembolso → pagar todas → PAGADO | Cajita 3 con saldo | POST préstamo, 3 pagos, 4.º pago | Movimiento de desembolso; saldo cajita −300.000 y luego +27.000; PAGADO; 422 "ya está pagado" | Integración | Sí · `FlujosNegocioIT#cicloDeVidaDelPrestamo` |
| IN-03 | prestamos | Anular pago de cuota | Pago 1 del seed | POST anular | Contramovimiento −196.667; EN_MORA; auditoría | Integración | Sí · `FlujosNegocioIT#anularPagoDeCuota` |
| IN-04 | integrantes | Aportes en lote → atraso | Integrante 2 atrasado | Lote | Atraso 100.000 → 0 | Integración | Sí · `FlujosNegocioIT#aportesEnLoteActualizanElAtraso` |
| IN-05 | tesoreria | Lote todo-o-nada | Integrante ajeno en el lote | Lote | 404 y nada guardado | Integración | Sí · `FlujosNegocioIT#aporteEnLoteConIntegranteAjenoNoGuardaNada` |
| IN-06 | actividades | Rifas → utilidad real | Seed | GET, pago de boletas | Utilidad real −1.050.000 → recaudo 1.000.000; integrante ve solo su fila | Integración | Sí · `FlujosNegocioIT#rifas` |
| IN-07 | reparticion | Proyectada vs real | Seed | GET /reparticion | Proy. 180.000 / 70.000 c/u; real 30.000; 14.400; 47.568,33; 720 real | Integración | Sí · `FlujosNegocioIT#reparticion` |
| IN-08 | tesoreria | Conciliación con Nu | Cajita 4 con −720 | Reportar 300.720 | Pasa a cuadrar | Integración | Sí · `FlujosNegocioIT#conciliacion` |
| IN-09 | tesoreria | Anulación de movimiento | Movimiento manual | Anular dos veces | Contramovimiento; 2.ª 422 | Integración | Sí · `FlujosNegocioIT#anulacionDeMovimiento` |
| IN-10 | integrantes | Cuota propia en INSCRIPCION + auditoría | Admin abre inscripción | PUT cuota | 200, ahorro esperado 3.000.000, registro CAMBIAR_CUOTA | Integración | Sí · `FlujosNegocioIT#cuotaPropia` |
| IN-11 | resumen | Resumen y KPIs | Seed | GET /resumen | Caja 1.250.000 / 1.500.000; KPIs golden | Integración | Sí · `FlujosNegocioIT#resumen` |
| IN-12 | resumen | Alerta de caja negativa | Préstamo 3.000.000 en mes 1 | GET /resumen | `alertaCajaNegativa=true` | Integración + E2E | Sí · `FlujosNegocioIT#alertaDeCajaNegativa`, e2e "alerta de caja negativa" |
| IN-13 | shared | DATABASE_URL de Railway → JDBC | `postgresql://u:cl%40ve@host/db` | Post-procesador | URL JDBC, usuario y clave decodificada | Unit | Sí · `InfraestructuraTest#databaseUrlDeRailwaySeTraduceAJdbc` |

## 5. Reportes

| ID | Módulo | Caso | Precondición | Pasos | Resultado esperado | Tipo | Automatizada |
|---|---|---|---|---|---|---|---|
| RE-01 | reportes | Consolidado por integrante y alcance del tesorero | Seed | GET consolidado (admin / tesorero 2) | 10 filas, Samay con préstamo y boletas / solo 6..10 | Integración | Sí · `ReportesIT#consolidadoPorIntegranteConAlcanceDelTesorero` |
| RE-02 | reportes | Deudores: tres tipos, orden y filtros | Seed | GET con tipo/tesorero/soloMora | Orden descendente; APORTES {2,9}; PRESTAMOS {3}; BOLETAS 10; mora {2,9} | Integración | Sí · `ReportesIT#deudoresConLosTresTiposDeDeudaYFiltros` |
| RE-03 | reportes | Estado de cuenta | Integrante 3 | GET propio | Aportes 12 meses, movimientos, préstamo con plan, 2 rifas, reparto | Integración | Sí · `ReportesIT#estadoDeCuentaIndividual` |
| RE-04 | reportes | Excel: hojas, encabezados, formato COP | — | `?formato=xlsx` | Hojas Resumen…Repartición; formato con `$` | Integración | Sí · `ReportesIT#exportacionExcelConUnaHojaPorTablaYFormatoCop` |
| RE-05 | reportes | CSV: BOM, `;`, filas | — | `?formato=csv` | Encabezado y 10 filas | Integración + Unit | Sí · `ReportesIT#exportacionCsvSeparadaPorPuntoYComaConBom`, `InfraestructuraTest#csvFormatea…` |
| RE-06 | reportes | Tesorería por rango y tipo | Tesorero 1 | febrero, AHORRO | 5 cajitas, 4 movimientos AHORRO | Integración | Sí · `ReportesIT#reporteDeTesoreriaPorRangoDeFechasYTipo` |
| RE-07 | reportes | Alcance por rol de los reportes | — | Matriz | Ver 3.1 | Integración | Sí · `SeguridadIT#matrizRolPorEndpoint` |
| RE-08 | frontend | Versión imprimible | Cualquier página | Ctrl+P | Sin menú, tarjetas sin cortes | Manual | No |

## 6. E2E (Playwright)

| ID | Caso | Pasos | Resultado esperado | Automatizada |
|---|---|---|---|---|
| E2-01 | Credenciales incorrectas | Login con clave errada | Alerta "Usuario o contraseña incorrectos" | Sí · `FE/e2e/natillera.spec.ts` |
| E2-02 | Integrante solo ve "Mi cuenta" | Login integrante 3; ir a /admin y /tesoreria | Menú = Mi cuenta, Rifas; redirige a /mi-cuenta | Sí |
| E2-03 | IDOR desde el navegador | Pedir estado de cuenta 3 y 7 por el BFF | 200 / 404 | Sí |
| E2-04 | Navegación del tesorero | Login tesorero 1 | /tesoreria, 5 cajitas, sin "Usuarios" | Sí |
| E2-05 | Logout | Salir y volver a /mi-cuenta | Cookies httpOnly/Lax; redirige a /login | Sí |
| E2-06 | Tesorero registra aporte en lote | Aportes del mes | "Se registraron 1 aportes" | Sí |
| E2-07 | Admin crea préstamo y ve alerta de caja negativa | Nuevo préstamo 4.000.000 en enero | Detalle del préstamo; alerta en /admin | Sí |
| E2-08 | Validación de cuotas en el formulario | 13 cuotas | Mensaje "…más de 12 cuotas (un año)" | Sí |

Cada caso corre en dos proyectos: **Pixel 7 (mobile-first)** y **Chrome de escritorio** (16 ejecuciones).

## 7. Frontend (unitarias)

| ID | Caso | Resultado esperado | Automatizada |
|---|---|---|---|
| FU-01 | Formato COP (`$1.250.000`, redondeo, negativos, vacíos) | Correcto | Sí · `FE/src/lib/formato.test.ts` |
| FU-02 | Porcentaje, fecha de negocio sin corrimiento de zona, mes del ciclo que cruza de año | Correcto | Sí · `formato.test.ts` |
| FU-03 | Esquemas Zod (login, contraseña, préstamo, movimiento, lote, integrante, usuario) | Válido/inválido con mensaje | Sí · `FE/src/lib/esquemas.test.ts` |
| FU-04 | Guardas de rutas y menú por rol | Correcto | Sí · `FE/src/lib/rutas.test.ts` |
| FU-05 | Sesión firmada (firma/lectura/manipulación) | Correcto | Sí · `FE/src/lib/sesion.test.ts` |
| FU-06 | Alertas visuales: dinero en rojo, PAGADO en verde con ícono, ProblemDetail | Correcto | Sí · `FE/src/components/ui.test.tsx` |

## 8. No funcionales

| ID | Caso | Medición / criterio | Resultado | Automatizada |
|---|---|---|---|---|
| NF-01 | p95 de reportes < 500 ms con datos semilla | 40 peticiones tras 5 de calentamiento, por endpoint | p95: consolidado 98 ms, deudores 70, estado de cuenta 89, tesorería 24, resumen 45, repartición 23, aportes 12, préstamos 11 ms | Sí · `BT/nofuncional/RendimientoIT#p95MenorA500ms` |
| NF-02 | Revisión de N+1 | Sentencias JPA por petición antes y después de agregar 3 préstamos, 3 movimientos y 1 pago | Iguales (consolidado 16→16, estado de cuenta 31→31, resumen 15→15, …) | Sí · `RendimientoIT#sinNmas1` |
| NF-03 | Arranque en frío < 10 s | Tiempo hasta contexto listo con flags de producción | Jar extraído + CDS: **7,2 s** (CDS entrenado con BD) y **8,8 s** (entrenado sin BD, como el Dockerfile); fat jar: 19,7–27,4 s ✗ | Manual (medido; ver ARQUITECTURA §8) |
| NF-04 | Memoria del contenedor < 400 MB en reposo | RSS del proceso Java con `-Xmx256m` tras atender peticiones | ≈ 301 MB | Manual (medido en Windows; en contenedor: `docker stats`) |
| NF-05 | Responsive mobile-first | Capturas a 412 px y 1280 px | Sin scroll horizontal de página; tablas con scroll propio | Manual + E2E en Pixel 7 |
| NF-06 | Accesibilidad básica | Labels en campos, `aria-current`, roles `alert`/`status`, foco visible, estados con ícono + texto | Cumple | Manual + E2E (selectores por rol/label) |

## 9. Regresión contra el Excel (manual)

1. Abra `Natillera_Familiar (1).xlsx`. En **Integrantes** escriba las mismas cuotas de la web; en **Configuración** las
   mismas tasas y fecha de inicio.
2. En **Tesorería** copie los movimientos de la web (Reportes → Tesorería → Excel, hoja "Movimientos"): fecha, cajita,
   tipo (con la etiqueta del Excel, p. ej. "Ahorro", "Pago cuota préstamo") y entrada/salida.
3. En **Ahorros Mensuales** copie los aportes por mes (Ahorros mensuales en la web, o el reporte Consolidado).
4. En **Préstamos** copie cada préstamo (fecha, integrante, monto, mes, cuotas) y las cuotas pagadas.
5. En **Actividades** copie las rifas y lo pagado por integrante.
6. Compare hoja por hoja con la web (tolerancia: ±1 peso por presentación sin decimales):

| Hoja | Pantalla / reporte web | Qué comparar |
|---|---|---|
| Integrantes | Admin → Integrantes | Ahorro esperado, % del total, rendimiento proyectado |
| Ahorros Mensuales | Ahorros mensuales | Matriz, total, diferencia, % cumplimiento, atraso (rojo), totales y acumulados |
| Tesorería | Mis cajitas + Reportes → Tesorería | Saldos por cajita y tesorero, diferencia con Nu, totales por tipo, saldo acumulado por mes |
| Préstamos | Préstamos (+ detalle) | Columnas H–P y la grilla de intereses R–AC |
| Actividades | Rifas | Recaudos, costos, utilidades esperada/real y por integrante |
| Repartición | Repartición | Conceptos B5:C9 y columnas B–N por integrante |
| Resumen | Admin → Resumen | Columnas C–K por mes |

Diferencias esperadas y documentadas: el **plan de pagos** redondea cada cuota al peso con residuo en la última (el
Excel no tiene plan); préstamos que pasan el cierre o superan el saldo de la cajita se **bloquean** en la web.

## 10. Checklist de despliegue

- [ ] Railway: PostgreSQL creado y **Backups** habilitados.
- [ ] Servicio backend con Root Directory `backend`; build con `Dockerfile` (verificar en el log la etapa `cds`).
- [ ] Variables backend: `DATABASE_URL=${{Postgres.DATABASE_URL}}` (red privada), `JWT_SECRET` (≥ 32 bytes aleatorios), `CORS_ALLOWED_ORIGIN`, `FRONTEND_URL`, `SPRING_PROFILES_ACTIVE=prod`. **No** definir `FECHA_SIMULADA`.
- [ ] Migraciones: el log muestra `Successfully applied 1 migration … v1` (en prod no corre el seed).
- [ ] Healthcheck `/actuator/health` en verde; `/v3/api-docs` y `/swagger-ui.html` responden 404/401 en prod.
- [ ] *App sleeping* activo; primera petición tras dormir < 10 s.
- [ ] Primer administrador creado (ARQUITECTURA §9) y obligado a cambiar la contraseña.
- [ ] Vercel: Root Directory `frontend`; variables `BACKEND_URL` (dominio de Railway), `SESSION_SECRET` (≥ 32), `COOKIE_SECURE=true`.
- [ ] Dominio final de Vercel puesto en `CORS_ALLOWED_ORIGIN` y redeploy del backend.
- [ ] Login desde el celular; las cookies `nat_at`, `nat_rt`, `nat_sesion` son httpOnly + Secure.
- [ ] Prueba de humo E2E contra producción: `E2E_BASE_URL=https://<app>.vercel.app npx playwright test --grep "credenciales"`.

---

## Trazabilidad requisito → pruebas

| Requisito (sección del prompt) | IDs |
|---|---|
| 2.1–2.2 Roles y matriz de permisos | SE-01, SE-02, SE-07, SE-19, RE-07 |
| 2.2 Prevención de IDOR (404) | SE-03, SE-04, SE-05, SE-06, E2-03 |
| 2.2 Supuesto: tesorero ve todos los préstamos | SE-06, RE-01 |
| 2.3 JWT 15 min, refresh rotativo hasheado, familia revocada | SE-09, SE-10, SE-11, SE-12 |
| 2.3 BFF, cookies httpOnly/Secure/Lax | SE-20, SE-21, E2-05 |
| 2.3 Logout, cambio obligatorio, rate limit, CORS | SE-13, SE-18, SE-15, SE-16 |
| 3.1 Configuración del ciclo | UN-09, UN-10, UN-11 |
| 3.2 Integrantes y cuota propia en inscripción | UN-01, UN-06, UN-12, UN-13, VA-13, IN-10 |
| 3.3 Aportes desde el libro | UN-14, UN-15, UN-16, IN-04, IN-05, E2-06 |
| 3.4 Préstamos y validaciones | UN-02..UN-04, UN-21..UN-27, VA-01..VA-06, IN-01..IN-03, E2-07, E2-08 |
| 3.5 Rifas | UN-07, VA-07, IN-06 |
| 3.6 Tesorería, conciliación, anulaciones | UN-28, UN-29, VA-08..VA-12, IN-08, IN-09 |
| 3.7 Repartición | UN-05, UN-17..UN-20, IN-07 |
| 3.8 Resumen y alertas | UN-08, IN-11, IN-12, E2-07 |
| 3.9 Golden tests | UN-01..UN-08, IN-01, IN-07, IN-11 |
| 4 Reportes y exportación | RE-01..RE-08, SE-22, NF-01, NF-02 |
| 6 Frontend (formato, Zod, guardas, alertas) | FU-01..FU-06 |
| 7 Recursos y despliegue | NF-03, NF-04, IN-13, §10 |
| 8 Seguridad adicional (headers, validación, sin secretos en logs) | SE-17, VA-14, SE-14 |

---

## Evidencia de la última ejecución (2026-10-26, Windows 11, JDK 21.0.7, Node 22.17)

**Backend — `./mvnw verify`** (sin Docker en la máquina: las pruebas de integración usaron el PostgreSQL 16.15
embebido; en CI usan Testcontainers):

```
Unitarias (surefire)
  BordesDominioTest ............................. 20 OK
  GoldenExcelTest (+ PrestamoGolden) ............  8 OK
  CalculadoraPrestamoTest ....................... 17 OK
  AccessScopeServiceTest ........................  5 OK
  AuthServiceTest ...............................  7 OK
  InfraestructuraTest ...........................  5 OK
  Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
Integración (failsafe)
  SeguridadIT (matriz 30 + sin token 30 + IDOR 6 + tokens 6 + protecciones 4) ... 76 OK
  FlujosNegocioIT ............................... 13 OK
  ReportesIT ....................................  7 OK
  RendimientoIT .................................  9 OK
  Tests run: 105, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (1 min 25 s)
```

**Frontend — `npm test` (Vitest 3.2)**

```
Test Files  5 passed (5)
     Tests  21 passed (21)
```

`npm run lint` sin hallazgos; `npm run typecheck` sin errores; `npm run build` OK.
`npm audit --omit=dev`: **0 vulnerabilidades** (zod 4.6.7; `postcss` forzado a 8.5.31 con `overrides` dentro de Next).

**E2E — `npx playwright test`** contra Next.js `next start` + backend (fat jar, perfil dev) + PostgreSQL 16 embebido:

```
16 passed (1.7m)   — 8 casos × {celular (Pixel 7), escritorio}; repetido tras migrar a zod 4
```

**No verificado en esta máquina:** construcción de las imágenes Docker (`backend/Dockerfile`, `frontend/Dockerfile`),
`docker compose up` y el job E2E de CI, por no tener Docker instalado. La técnica del Dockerfile (jar extraído + CDS
entrenado sin base) sí se ejecutó a mano con el mismo JDK: 8,8 s de arranque.
