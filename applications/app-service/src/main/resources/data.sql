-- Catálogos que necesitan las historias de usuario. Idempotente: no duplica si ya existen.

-- HU3: roles del sistema
INSERT INTO rol (id_rol, nombre, descripcion) VALUES
    (1, 'ADMIN',   'Administrador del sistema'),
    (2, 'ASESOR',  'Asesor que registra usuarios y revisa solicitudes'),
    (3, 'CLIENTE', 'Cliente que envía solicitudes de préstamo')
ON CONFLICT (id_rol) DO NOTHING;

-- HU2 y HU4: estados de una solicitud
INSERT INTO estados (id_estado, nombre, descripcion) VALUES
    (1, 'Pendiente de revisión', 'Estado inicial de toda solicitud nueva'),
    (2, 'Rechazada',             'La solicitud no fue aprobada'),
    (3, 'Revisión manual',       'Un asesor debe revisar la solicitud'),
    (4, 'Aprobada',              'La solicitud fue aprobada')
ON CONFLICT (id_estado) DO NOTHING;

-- HU2: tipos de préstamo existentes (valores de ejemplo, ajustarlos a lo que pida la clase)
INSERT INTO tipo_prestamo (nombre, monto_minimo, monto_maximo, tasa_interes, validacion_automatica) VALUES
    ('Libre inversión', 1000000,  50000000, 1.80, TRUE),
    ('Vehículo',       10000000, 150000000, 1.40, FALSE),
    ('Educativo',        500000,  30000000, 1.10, TRUE)
ON CONFLICT (nombre) DO NOTHING;
