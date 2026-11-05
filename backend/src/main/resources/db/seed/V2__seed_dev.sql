-- Datos semilla SOLO para desarrollo (perfil dev). Contraseña de todos los usuarios: Natillera2027*
-- 1 admin, 2 tesoreros y 10 integrantes con las cuotas de la sección 3.9 del prompt; nombres de la hoja "Integrantes".
-- Con FECHA_SIMULADA=2027-03-20 el ciclo va en el mes 3: hay aportes de enero a marzo, el préstamo de ejemplo
-- (mes 2, 6 cuotas) con la cuota 1 pagada y las 2 rifas, con boletas de la rifa 1 pagadas por algunos integrantes.

UPDATE ciclo SET estado = 'ACTIVO' WHERE id = 1;

INSERT INTO usuario (id, nombre, correo, celular, password_hash, activo, debe_cambiar_password, creado_por) VALUES
  (1,  'John Jairo Valencia', 'admin@natillera.local',        '3000000001', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (2,  'Pascuala Leyes',      'tesorero1@natillera.local',    '3000000002', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (3,  'Arley Leudo',         'tesorero2@natillera.local',    '3000000006', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (4,  'Samay Moreno',        'integrante3@natillera.local',  '3000000003', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (5,  'Sindy Valencia',      'integrante4@natillera.local',  '3000000004', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (6,  'Trinidad Agualimpia', 'integrante5@natillera.local',  '3000000005', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (7,  'Haddy Moreno',        'integrante7@natillera.local',  '3000000007', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (8,  'Juan José Valencia',  'integrante8@natillera.local',  '3000000008', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (9,  'Yarle',               'integrante9@natillera.local',  '3000000009', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed'),
  (10, 'Integrante 10',       'integrante10@natillera.local', '3000000010', '$2a$12$UmqY7ns0tN1meGXecxKV..Lf6xixUcLRENkqug55lFOBVreqA4wnO', TRUE, FALSE, 'seed');

-- ADMIN=1, TESORERO=2, INTEGRANTE=3 (el admin y los tesoreros también ahorran)
INSERT INTO usuario_rol (usuario_id, rol_id) VALUES
  (1, 1), (1, 3),
  (2, 2), (2, 3),
  (3, 2), (3, 3),
  (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3);

INSERT INTO tesorero (id, usuario_id, nombre, creado_por) VALUES
  (1, 2, 'Pascuala Leyes', 'seed'),
  (2, 3, 'Arley Leudo', 'seed');

INSERT INTO integrante (id, usuario_id, nombre, telefono, cuota_mensual, tesorero_id, activo, creado_por) VALUES
  (1,  1,  'John Jairo Valencia', '3000000001', 100000, 1, TRUE, 'seed'),
  (2,  2,  'Pascuala Leyes',      '3000000002',  50000, 1, TRUE, 'seed'),
  (3,  4,  'Samay Moreno',        '3000000003', 200000, 1, TRUE, 'seed'),
  (4,  5,  'Sindy Valencia',      '3000000004', 100000, 1, TRUE, 'seed'),
  (5,  6,  'Trinidad Agualimpia', '3000000005', 150000, 1, TRUE, 'seed'),
  (6,  3,  'Arley Leudo',         '3000000006',  50000, 2, TRUE, 'seed'),
  (7,  7,  'Haddy Moreno',        '3000000007', 300000, 2, TRUE, 'seed'),
  (8,  8,  'Juan José Valencia',  '3000000008', 100000, 2, TRUE, 'seed'),
  (9,  9,  'Yarle',               '3000000009',  80000, 2, TRUE, 'seed'),
  (10, 10, 'Integrante 10',       '3000000010', 120000, 2, TRUE, 'seed');

INSERT INTO cajita (id, integrante_id, nombre, creado_por)
SELECT id, id, CONCAT('Natillera - ', nombre), 'seed' FROM integrante;

-- Aportes (movimientos AHORRO): enero completo; en febrero no aporta Pascuala (2); en marzo no aportan 2 ni Yarle (9)
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, creado_por)
SELECT i.id, DATE '2027-01-05', 'AHORRO', 'Cuota enero', i.cuota_mensual, 0, 'seed' FROM integrante i;
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, creado_por)
SELECT i.id, DATE '2027-02-05', 'AHORRO', 'Cuota febrero', i.cuota_mensual, 0, 'seed' FROM integrante i WHERE i.id <> 2;
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, creado_por)
SELECT i.id, DATE '2027-03-05', 'AHORRO', 'Cuota marzo', i.cuota_mensual, 0, 'seed' FROM integrante i WHERE i.id NOT IN (2, 9);

-- Rendimientos Nu de febrero (≈ saldo de enero × tasa mensual equivalente)
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, creado_por) VALUES
  (1, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 720, 0, 'seed'),
  (2, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 360, 0, 'seed'),
  (3, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 1441, 0, 'seed'),
  (4, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 720, 0, 'seed'),
  (5, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 1081, 0, 'seed'),
  (6, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 360, 0, 'seed'),
  (7, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 2162, 0, 'seed'),
  (8, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 720, 0, 'seed'),
  (9, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 577, 0, 'seed'),
  (10, DATE '2027-02-28', 'RENDIMIENTO_NU', 'Rendimiento febrero', 865, 0, 'seed');

-- Préstamo de ejemplo (golden test): 1.000.000 a Samay Moreno (3), mes 2, 6 cuotas al 3 %, desde la cajita de Haddy (7).
-- Se inserta directo (sin pasar por las reglas): en un ciclo real la cajita tendría saldo suficiente el día del desembolso.
INSERT INTO prestamo (id, integrante_id, cajita_origen_id, fecha_desembolso, monto, mes_del_prestamo, numero_cuotas,
                      tasa_mensual, creado_por)
VALUES (1, 3, 7, DATE '2027-02-15', 1000000, 2, 6, 0.03, 'seed');
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, prestamo_id, creado_por)
VALUES (7, DATE '2027-02-15', 'DESEMBOLSO_PRESTAMO', 'Desembolso préstamo #1 a Samay Moreno', 0, 1000000, 1, 'seed');

-- Cuota 1 pagada (vence en el mes 3): 166.667 de capital + 30.000 de interés
INSERT INTO pago_cuota (id, prestamo_id, numero_cuota, fecha, monto, cajita_destino_id, creado_por)
VALUES (1, 1, 1, DATE '2027-03-10', 196667, 7, 'seed');
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, prestamo_id, pago_cuota_id, creado_por)
VALUES (7, DATE '2027-03-10', 'PAGO_CUOTA_PRESTAMO', 'Cuota 1 préstamo #1', 196667, 0, 1, 1, 'seed');

-- Las 2 rifas del Excel
INSERT INTO actividad (id, nombre, fecha_sorteo, valor_boleta, boletas_por_integrante, costo_premio, otros_gastos,
                       creado_por) VALUES
  (1, 'Rifa Televisor 55"', DATE '2027-06-15', 10000, 20, 1800000, 50000, 'seed'),
  (2, 'Rifa PS5',           DATE '2027-11-30', 15000, 20, 2400000, 50000, 'seed');

-- Boletas de la rifa 1 pagadas por John (1), Samay (3), Arley (6) y Haddy (7)
INSERT INTO movimiento (cajita_id, fecha, tipo, descripcion, entrada, salida, actividad_id, creado_por) VALUES
  (1, DATE '2027-03-12', 'PAGO_BOLETAS_RIFA', 'Boletas Rifa Televisor 55"', 200000, 0, 1, 'seed'),
  (3, DATE '2027-03-12', 'PAGO_BOLETAS_RIFA', 'Boletas Rifa Televisor 55"', 200000, 0, 1, 'seed'),
  (6, DATE '2027-03-12', 'PAGO_BOLETAS_RIFA', 'Boletas Rifa Televisor 55"', 200000, 0, 1, 'seed'),
  (7, DATE '2027-03-12', 'PAGO_BOLETAS_RIFA', 'Boletas Rifa Televisor 55"', 200000, 0, 1, 'seed');

-- Conciliación: la cajita 1 cuadra con la app; la cajita 4 tiene una diferencia de -720 (se verá en rojo)
INSERT INTO saldo_reportado_nu (cajita_id, fecha, saldo_app, creado_por) VALUES
  (1, DATE '2027-03-15', 500720, 'seed'),
  (4, DATE '2027-03-15', 300000, 'seed');
