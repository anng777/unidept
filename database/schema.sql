-- =====================================================================
-- UniDept - Script de Base de Datos (MySQL 8+)
-- Alquileres de departamentos/habitaciones para estudiantes universitarios
-- =====================================================================

DROP DATABASE IF EXISTS unidept;
CREATE DATABASE unidept CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE unidept;

-- ---------------------------------------------------------------------
-- Tabla: usuarios (estudiantes, arrendadores y administrador)
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
    dni             VARCHAR(8)  NOT NULL UNIQUE,
    nombre_usuario  VARCHAR(30) NOT NULL UNIQUE,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    telefono        VARCHAR(9) NOT NULL,
    tipo_usuario    ENUM('ESTUDIANTE', 'ARRENDADOR', 'ADMINISTRADOR') NOT NULL,
    verificado      BOOLEAN NOT NULL DEFAULT FALSE,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,   -- FALSE = cuenta suspendida por el administrador
    fecha_registro  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- Tabla: universidades (usadas para calcular distancia/tiempo)
-- ---------------------------------------------------------------------
CREATE TABLE universidades (
    id_universidad  INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    direccion       VARCHAR(255),
    latitud         DECIMAL(10,7) NOT NULL,
    longitud        DECIMAL(10,7) NOT NULL
);

-- ---------------------------------------------------------------------
-- Tabla: alojamientos (departamentos / habitaciones publicados)
-- ---------------------------------------------------------------------
CREATE TABLE alojamientos (
    id_alojamiento    INT AUTO_INCREMENT PRIMARY KEY,
    id_arrendador     INT NOT NULL,
    titulo            VARCHAR(150) NOT NULL,
    descripcion       TEXT,
    tipo              ENUM('DEPARTAMENTO', 'HABITACION') NOT NULL,
    precio_mensual    DECIMAL(8,2) NOT NULL CHECK (precio_mensual >= 0),
    direccion         VARCHAR(255) NOT NULL,
    distrito          VARCHAR(100),
    latitud           DECIMAL(10,7) NOT NULL,
    longitud          DECIMAL(10,7) NOT NULL,
    capacidad         INT DEFAULT 1,
    amoblado          BOOLEAN DEFAULT FALSE,
    wifi              BOOLEAN DEFAULT FALSE,
    admite_roommates  BOOLEAN NOT NULL DEFAULT FALSE,
    admite_mascotas   BOOLEAN NOT NULL DEFAULT FALSE,
    estado_publicacion ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    disponible        BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_publicacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alojamiento_arrendador
        FOREIGN KEY (id_arrendador) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: imagenes_alojamiento (varias fotos por alojamiento)
-- ---------------------------------------------------------------------
CREATE TABLE imagenes_alojamiento (
    id_imagen       INT AUTO_INCREMENT PRIMARY KEY,
    id_alojamiento  INT NOT NULL,
    url_imagen      VARCHAR(255) NOT NULL,
    CONSTRAINT fk_imagen_alojamiento
        FOREIGN KEY (id_alojamiento) REFERENCES alojamientos(id_alojamiento)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: solicitudes (un estudiante solicita reservar un alojamiento)
-- ---------------------------------------------------------------------
CREATE TABLE solicitudes (
    id_solicitud    INT AUTO_INCREMENT PRIMARY KEY,
    id_alojamiento  INT NOT NULL,
    id_estudiante   INT NOT NULL,
    mensaje         VARCHAR(500),
    estado          ENUM('PENDIENTE', 'ACEPTADA', 'RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    fecha_inicio    DATE NULL,
    fecha_fin       DATE NULL,
    -- Pago simulado de la reserva (10% del precio mensual) al enviar la solicitud
    monto_pago      DECIMAL(8,2) NULL,
    metodo_pago     VARCHAR(30) NULL,
    estado_pago     ENUM('PAGADO', 'PENDIENTE') NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago      DATETIME NULL,
    fecha_solicitud DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_solicitud_alojamiento
        FOREIGN KEY (id_alojamiento) REFERENCES alojamientos(id_alojamiento)
        ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_estudiante
        FOREIGN KEY (id_estudiante) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: ocupaciones (rangos de fechas en que un alojamiento está OCUPADO).
-- Las crea el arrendador a mano o se generan al ACEPTAR una solicitud.
-- ---------------------------------------------------------------------
CREATE TABLE ocupaciones (
    id_ocupacion    INT AUTO_INCREMENT PRIMARY KEY,
    id_alojamiento  INT NOT NULL,
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE NOT NULL,
    nota            VARCHAR(200),
    id_solicitud    INT NULL,
    CONSTRAINT fk_ocup_alojamiento FOREIGN KEY (id_alojamiento) REFERENCES alojamientos(id_alojamiento) ON DELETE CASCADE,
    CONSTRAINT fk_ocup_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitudes(id_solicitud) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: mensajes (chat entre el estudiante y el arrendador de una solicitud)
-- ---------------------------------------------------------------------
CREATE TABLE mensajes (
    id_mensaje      INT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud    INT NOT NULL,
    id_remitente    INT NOT NULL,
    contenido       VARCHAR(1000) NOT NULL,
    leido           BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mensaje_solicitud
        FOREIGN KEY (id_solicitud) REFERENCES solicitudes(id_solicitud)
        ON DELETE CASCADE,
    CONSTRAINT fk_mensaje_remitente
        FOREIGN KEY (id_remitente) REFERENCES usuarios(id_usuario)
        ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: calificaciones (estudiante <-> arrendador, solo tras una solicitud ACEPTADA)
-- ---------------------------------------------------------------------
CREATE TABLE calificaciones (
    id_calificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud    INT NOT NULL,
    id_calificador  INT NOT NULL,
    id_calificado   INT NOT NULL,
    puntuacion      INT NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
    comentario      VARCHAR(500),
    fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_calificacion UNIQUE (id_solicitud, id_calificador),
    CONSTRAINT fk_calif_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitudes(id_solicitud) ON DELETE CASCADE,
    CONSTRAINT fk_calif_calificador FOREIGN KEY (id_calificador) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_calif_calificado FOREIGN KEY (id_calificado) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: reportes (contra un usuario o contra un alojamiento; los gestiona el administrador)
-- ---------------------------------------------------------------------
CREATE TABLE reportes (
    id_reporte               INT AUTO_INCREMENT PRIMARY KEY,
    id_reportante            INT NOT NULL,
    id_usuario_reportado     INT NULL,
    id_alojamiento_reportado INT NULL,
    motivo                   VARCHAR(500) NOT NULL,
    estado                   ENUM('PENDIENTE','REVISADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rep_reportante FOREIGN KEY (id_reportante) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_rep_usuario FOREIGN KEY (id_usuario_reportado) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_rep_aloj FOREIGN KEY (id_alojamiento_reportado) REFERENCES alojamientos(id_alojamiento) ON DELETE CASCADE
    -- (MySQL no permite un CHECK sobre columnas con ON DELETE CASCADE; el servlet exige usuario O alojamiento)
);

-- =====================================================================
-- DATOS DE EJEMPLO
-- Todos los usuarios, alojamientos, solicitudes y mensajes de prueba
-- quedan REGISTRADOS DIRECTAMENTE EN LA BASE DE DATOS con este script
-- (ya no se generan desde Java al arrancar el servidor).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios de ejemplo
-- Contraseña para los 5 = "Password123"
-- El hash de abajo es un hash BCrypt REAL, generado y verificado con el
-- algoritmo bcrypt estándar (formato "$2a$", compatible con BCryptPasswordEncoder de Spring Security y con la librería
-- jBCrypt que usa este proyecto) — no es un valor de relleno.
-- ---------------------------------------------------------------------
INSERT INTO usuarios (dni, nombre_usuario, nombres, apellidos, email, password_hash, telefono, tipo_usuario, verificado) VALUES
('71234567', 'ana_ramirez',    'Ana Lucía', 'Ramírez Torres', 'ana.estudiante@pucp.edu.pe',  '$2a$10$pg/lqmRuUUcMoLA.JIg/iOb4fqg6i7DVeL/RwGOnoUt8DXolmIJG2', '987654321', 'ESTUDIANTE', TRUE),
('73456789', 'valeria_soto',   'Valeria',   'Soto Campos',    'valeria.soto@upn.edu.pe',      '$2a$10$pg/lqmRuUUcMoLA.JIg/iOb4fqg6i7DVeL/RwGOnoUt8DXolmIJG2', '923456789', 'ESTUDIANTE', TRUE),
('72345678', 'carlos_mendoza', 'Carlos',    'Mendoza Vega',   'carlos.arrienda@gmail.com',    '$2a$10$pg/lqmRuUUcMoLA.JIg/iOb4fqg6i7DVeL/RwGOnoUt8DXolmIJG2', '912345678', 'ARRENDADOR', TRUE),
('74567890', 'jorge_torres',   'Jorge',     'Torres Ríos',    'jorge.arrienda@hotmail.com',   '$2a$10$pg/lqmRuUUcMoLA.JIg/iOb4fqg6i7DVeL/RwGOnoUt8DXolmIJG2', '934567890', 'ARRENDADOR', TRUE);
-- Cuenta de ADMINISTRADOR (ingresa desde /admin/login). Misma contraseña: Password123
INSERT INTO usuarios (dni, nombre_usuario, nombres, apellidos, email, password_hash, telefono, tipo_usuario, verificado) VALUES
('00000001', 'admin', 'Administrador', 'UniDept', 'admin@unidept.com', '$2a$10$pg/lqmRuUUcMoLA.JIg/iOb4fqg6i7DVeL/RwGOnoUt8DXolmIJG2', '900000000', 'ADMINISTRADOR', TRUE);
-- id_usuario generados: 1=ana_ramirez, 2=valeria_soto, 3=carlos_mendoza, 4=jorge_torres, 5=admin

-- ---------------------------------------------------------------------
-- Universidades de Lima, Perú (incluye 5 sedes de la UTP)
-- ---------------------------------------------------------------------
INSERT INTO universidades (nombre, direccion, latitud, longitud) VALUES
('Pontificia Universidad Católica del Perú (PUCP)', 'Av. Universitaria 1801, San Miguel', -12.0689, -77.0786),
('Universidad Nacional Mayor de San Marcos (UNMSM)', 'Av. Germán Amézaga 375, Cercado de Lima', -12.0561, -77.0846),
('Universidad Peruana de Ciencias Aplicadas (UPC) - Monterrico', 'Prolongación Primavera 2390, Santiago de Surco', -12.1031, -76.963),
('Universidad de Lima', 'Av. Javier Prado Este 4600, Santiago de Surco', -12.0847, -76.9712),
('Universidad Nacional de Ingeniería (UNI)', 'Av. Túpac Amaru 210, Rímac', -12.0228, -77.048),
('Universidad Tecnológica del Perú (UTP) - Lima Centro', 'Av. Petit Thouars 116, Cercado de Lima', -12.0585, -77.0372),
('Universidad Tecnológica del Perú (UTP) - Sede Ate', 'Av. Nicolás Ayllón con, Ate 15487', -12.0138412, -76.8825823),
('Universidad Tecnológica del Perú (UTP) - Sede San Juan de Lurigancho', 'Av. Próceres de la Independencia 2110, SJL', -11.9958, -77.0089),
('Universidad Tecnológica del Perú (UTP) - Sede Independencia', 'Av. Túpac Amaru 2110, Independencia', -11.9925, -77.0567),
('Universidad Tecnológica del Perú (UTP) - Sede Chorrillos', 'Av. Defensores del Morro 1277, Chorrillos', -12.1857, -77.0202),
('Universidad Privada del Norte (UPN) - Lima Centro', 'Av. Paseo de la República 3691, San Isidro', -12.0913, -77.0295),
('Universidad San Ignacio de Loyola (USIL)', 'Av. La Fontana 550, La Molina', -12.0972, -76.9459),
('Universidad Ricardo Palma (URP)', 'Av. Benavides 5440, Santiago de Surco', -12.1306, -76.9731),
('Universidad del Pacífico', 'Av. Salaverry 2020, Jesús María', -12.0873, -77.0515),
('Universidad César Vallejo (UCV) - Lima Este', 'Carretera Central km 3.5, Ate', -12.0245, -76.9245);

-- ---------------------------------------------------------------------
-- Alojamientos de ejemplo (13), repartidos entre carlos_mendoza (3) y
-- jorge_torres (4), incluyendo 3 en la zona de Ate (cerca a la nueva
-- sede UTP - Ate). Los ids se generan en el orden de este INSERT (1-13).
-- ---------------------------------------------------------------------
INSERT INTO alojamientos (id_arrendador, titulo, descripcion, tipo, precio_mensual, direccion, distrito, latitud, longitud, capacidad, amoblado, wifi, admite_roommates) VALUES
(3, 'Habitación amoblada cerca a la PUCP', 'Habitación individual, baño compartido, muy cerca de la PUCP.', 'HABITACION', 650.00, 'Av. La Mar 1550', 'San Miguel', -12.0765, -77.0825, 1, TRUE, TRUE, FALSE),
(3, 'Departamento 2 dorm. en San Miguel (roommates)', 'Departamento completo, ideal para compartir entre 2-3 estudiantes de la PUCP.', 'DEPARTAMENTO', 1400.00, 'Av. Universitaria 1900', 'San Miguel', -12.071, -77.083, 3, TRUE, TRUE, TRUE),
(3, 'Cuarto sencillo cerca a la UNMSM', 'Habitación sencilla a pocas cuadras de San Marcos, zona tranquila.', 'HABITACION', 500.00, 'Jr. Húsares de Junín 350', 'Cercado de Lima', -12.055, -77.079, 1, FALSE, TRUE, FALSE),
(4, 'Depa moderno frente a la UPC Monterrico', 'Departamento moderno a 2 cuadras de la UPC sede Monterrico, ideal para 2 estudiantes.', 'DEPARTAMENTO', 1600.00, 'Calle Las Begonias 450', 'Santiago de Surco', -12.112, -76.987, 2, TRUE, TRUE, TRUE),
(4, 'Habitación económica cerca a la Universidad de Lima', 'Habitación económica cerca de la Universidad de Lima.', 'HABITACION', 480.00, 'Av. Aviación 3200', 'Santiago de Surco', -12.086, -76.972, 1, FALSE, TRUE, FALSE),
(3, 'Departamento para compartir cerca a la UNI', 'Departamento de 4 habitaciones para compartir entre estudiantes de la UNI.', 'DEPARTAMENTO', 1200.00, 'Av. Caquetá 500', 'Rímac', -12.0195, -77.0455, 4, FALSE, TRUE, TRUE),
(4, 'Habitación cerca a la UTP - Lima Centro', 'Habitación amoblada cerca de la sede de la UTP en el Centro de Lima.', 'HABITACION', 700.00, 'Av. Petit Thouars 200', 'Lima Cercado', -12.0705, -77.0375, 1, TRUE, TRUE, FALSE),
(3, 'Habitación cerca a la USIL', 'Habitación en casa familiar, admite compartir con otro estudiante (roomie).', 'HABITACION', 620.00, 'Av. La Fontana 700', 'La Molina', -12.0955, -76.948, 2, TRUE, TRUE, TRUE),
(4, 'Departamento amplio cerca a la Universidad del Pacífico', 'Departamento de 3 dormitorios, ideal para compartir gastos entre estudiantes.', 'DEPARTAMENTO', 1800.00, 'Av. Salaverry 2100', 'Jesús María', -12.078, -77.047, 3, TRUE, TRUE, TRUE),
(3, 'Cuarto cerca a la UPN - Lima Centro', 'Habitación sencilla, wifi incluido, cerca de la UPN.', 'HABITACION', 550.00, 'Av. Arequipa 3200', 'San Isidro', -12.093, -77.03, 1, FALSE, TRUE, FALSE),
(3, 'Habitación cerca a la UTP sede Ate', 'Habitación amoblada cerca de la sede Ate de la UTP.', 'HABITACION', 550.00, 'Av. Nicolás Ayllón 3200', 'Ate', -12.0405, -76.9175, 1, TRUE, TRUE, FALSE),
(4, 'Departamento para compartir cerca a la UTP Ate', 'Departamento de 3 dormitorios para compartir, muy cerca a la sede Ate de la UTP.', 'DEPARTAMENTO', 1300.00, 'Jr. Los Claveles 220', 'Ate', -12.038, -76.92, 3, TRUE, TRUE, TRUE),
(3, 'Cuarto económico cerca de la UTP Ate', 'Habitación sencilla en casa familiar, ambiente tranquilo, cerca al paradero de la Carretera Central.', 'HABITACION', 480.00, 'Jr. Las Magnolias 110', 'Ate', -12.042, -76.915, 1, FALSE, TRUE, FALSE);

INSERT INTO imagenes_alojamiento (id_alojamiento, url_imagen) VALUES
(1, 'https://picsum.photos/seed/alojamiento1-1/700/500'),
(1, 'https://picsum.photos/seed/alojamiento1-2/700/500'),
(1, 'https://picsum.photos/seed/alojamiento1-3/700/500'),
(2, 'https://picsum.photos/seed/alojamiento2-1/700/500'),
(2, 'https://picsum.photos/seed/alojamiento2-2/700/500'),
(2, 'https://picsum.photos/seed/alojamiento2-3/700/500'),
(3, 'https://picsum.photos/seed/alojamiento3-1/700/500'),
(3, 'https://picsum.photos/seed/alojamiento3-2/700/500'),
(3, 'https://picsum.photos/seed/alojamiento3-3/700/500'),
(4, 'https://picsum.photos/seed/alojamiento4-1/700/500'),
(4, 'https://picsum.photos/seed/alojamiento4-2/700/500'),
(4, 'https://picsum.photos/seed/alojamiento4-3/700/500'),
(5, 'https://picsum.photos/seed/alojamiento5-1/700/500'),
(5, 'https://picsum.photos/seed/alojamiento5-2/700/500'),
(5, 'https://picsum.photos/seed/alojamiento5-3/700/500'),
(6, 'https://picsum.photos/seed/alojamiento6-1/700/500'),
(6, 'https://picsum.photos/seed/alojamiento6-2/700/500'),
(6, 'https://picsum.photos/seed/alojamiento6-3/700/500'),
(7, 'https://picsum.photos/seed/alojamiento7-1/700/500'),
(7, 'https://picsum.photos/seed/alojamiento7-2/700/500'),
(7, 'https://picsum.photos/seed/alojamiento7-3/700/500'),
(8, 'https://picsum.photos/seed/alojamiento8-1/700/500'),
(8, 'https://picsum.photos/seed/alojamiento8-2/700/500'),
(8, 'https://picsum.photos/seed/alojamiento8-3/700/500'),
(9, 'https://picsum.photos/seed/alojamiento9-1/700/500'),
(9, 'https://picsum.photos/seed/alojamiento9-2/700/500'),
(9, 'https://picsum.photos/seed/alojamiento9-3/700/500'),
(10, 'https://picsum.photos/seed/alojamiento10-1/700/500'),
(10, 'https://picsum.photos/seed/alojamiento10-2/700/500'),
(10, 'https://picsum.photos/seed/alojamiento10-3/700/500'),
(11, 'https://picsum.photos/seed/alojamiento11-1/700/500'),
(11, 'https://picsum.photos/seed/alojamiento11-2/700/500'),
(11, 'https://picsum.photos/seed/alojamiento11-3/700/500'),
(12, 'https://picsum.photos/seed/alojamiento12-1/700/500'),
(12, 'https://picsum.photos/seed/alojamiento12-2/700/500'),
(12, 'https://picsum.photos/seed/alojamiento12-3/700/500'),
(13, 'https://picsum.photos/seed/alojamiento13-1/700/500'),
(13, 'https://picsum.photos/seed/alojamiento13-2/700/500'),
(13, 'https://picsum.photos/seed/alojamiento13-3/700/500');

-- ---------------------------------------------------------------------
-- Solicitudes de ejemplo (para que los dashboards no se vean vacíos)
-- ---------------------------------------------------------------------
INSERT INTO solicitudes (id_alojamiento, id_estudiante, mensaje, estado) VALUES
(1, 1, 'Hola, me interesa esta habitación para el próximo semestre. ¿Sigue disponible?', 'PENDIENTE'),
(7, 2, '¿Podría visitar el lugar este fin de semana?', 'ACEPTADA'),
(4, 1, '¿Aún hay cupo para roomies en este departamento?', 'PENDIENTE');
-- id_solicitud generados: 1, 2, 3 (en ese orden)

-- El mensaje de cada solicitud aparece como PRIMER mensaje del chat interno
INSERT INTO mensajes (id_solicitud, id_remitente, contenido)
SELECT id_solicitud, id_estudiante, mensaje FROM solicitudes ORDER BY id_solicitud;

-- ---------------------------------------------------------------------
-- Mensajes de ejemplo (chat entre estudiante y arrendador de la solicitud 2)
-- ---------------------------------------------------------------------
INSERT INTO mensajes (id_solicitud, id_remitente, contenido) VALUES
(2, 2, 'Hola, ¿podría visitar el departamento este sábado por la tarde?'),
(2, 4, 'Claro que sí, te espero el sábado a las 3pm. Cualquier cosa me escribes por aquí.');

-- ---------------------------------------------------------------------
-- Índices útiles para los filtros de búsqueda
-- ---------------------------------------------------------------------
CREATE INDEX idx_alojamiento_precio ON alojamientos(precio_mensual);
CREATE INDEX idx_alojamiento_tipo ON alojamientos(tipo);
CREATE INDEX idx_alojamiento_disponible ON alojamientos(disponible);
CREATE INDEX idx_alojamiento_roommates ON alojamientos(admite_roommates);
CREATE INDEX idx_usuario_nombre_usuario ON usuarios(nombre_usuario);
CREATE INDEX idx_mensaje_solicitud ON mensajes(id_solicitud);

-- ---------------------------------------------------------------------
-- Estado de publicación, mascotas, notificaciones, reseñas y reportes de ejemplo
-- ---------------------------------------------------------------------
-- Todos los alojamientos de ejemplo quedan APROBADOS, salvo el 13 (queda PENDIENTE para
-- que el administrador pruebe aprobar/rechazar).
UPDATE alojamientos SET estado_publicacion = 'APROBADA' WHERE id_alojamiento > 0;
UPDATE alojamientos SET estado_publicacion = 'PENDIENTE' WHERE id_alojamiento = 13;
-- Alojamientos que admiten mascotas
UPDATE alojamientos SET admite_mascotas = TRUE WHERE id_alojamiento IN (1, 4, 8, 11);
-- Mensajes ya leídos (los demás quedan sin leer para mostrar la notificación)
UPDATE mensajes SET leido = TRUE WHERE id_mensaje IN (2, 4);

INSERT INTO calificaciones (id_solicitud, id_calificador, id_calificado, puntuacion, comentario) VALUES
(2, 2, 4, 5, 'Excelente arrendadora, muy atenta y puntual.'),
(2, 4, 2, 5, 'Estudiante responsable y respetuosa.');

INSERT INTO reportes (id_reportante, id_usuario_reportado, id_alojamiento_reportado, motivo) VALUES
(1, NULL, 13, 'Las fotos del anuncio no parecen corresponder al lugar real.');


-- Coordenadas de los alojamientos: cada uno queda a la distancia indicada de su universidad de referencia
-- (así la distancia y el tiempo calculados coinciden con lo que se ve en el mapa).
UPDATE alojamientos SET latitud = -12.074352, longitud = -77.081819 WHERE id_alojamiento = 1;
UPDATE alojamientos SET latitud = -12.072947, longitud = -77.071432 WHERE id_alojamiento = 2;
UPDATE alojamientos SET latitud = -12.0561, longitud = -77.078623 WHERE id_alojamiento = 3;
UPDATE alojamientos SET latitud = -12.106248, longitud = -76.963 WHERE id_alojamiento = 4;
UPDATE alojamientos SET latitud = -12.080653, longitud = -76.978368 WHERE id_alojamiento = 5;
UPDATE alojamientos SET latitud = -12.027025, longitud = -77.049572 WHERE id_alojamiento = 6;
UPDATE alojamientos SET latitud = -12.062725, longitud = -77.035627 WHERE id_alojamiento = 7;
UPDATE alojamientos SET latitud = -12.0873, longitud = -76.949919 WHERE id_alojamiento = 8;
UPDATE alojamientos SET latitud = -12.084152, longitud = -77.045925 WHERE id_alojamiento = 9;
UPDATE alojamientos SET latitud = -12.085069, longitud = -77.033179 WHERE id_alojamiento = 10;
UPDATE alojamientos SET latitud = -12.0155, longitud = -76.8801 WHERE id_alojamiento = 11;
UPDATE alojamientos SET latitud = -12.0112, longitud = -76.8849 WHERE id_alojamiento = 12;
UPDATE alojamientos SET latitud = -12.0169, longitud = -76.8848 WHERE id_alojamiento = 13;

-- Fechas de las solicitudes de ejemplo (relativas a la fecha de hoy)
UPDATE solicitudes SET fecha_inicio = DATE_ADD(CURDATE(), INTERVAL 30 DAY),  fecha_fin = DATE_ADD(CURDATE(), INTERVAL 150 DAY) WHERE id_solicitud = 1;
UPDATE solicitudes SET fecha_inicio = DATE_ADD(CURDATE(), INTERVAL 10 DAY),  fecha_fin = DATE_ADD(CURDATE(), INTERVAL 100 DAY) WHERE id_solicitud = 2;
UPDATE solicitudes SET fecha_inicio = DATE_ADD(CURDATE(), INTERVAL 45 DAY),  fecha_fin = DATE_ADD(CURDATE(), INTERVAL 135 DAY) WHERE id_solicitud = 3;

-- Ocupaciones de ejemplo: la solicitud 2 (aceptada) ocupa el alojamiento 7; otros alojamientos tienen fechas ocupadas
INSERT INTO ocupaciones (id_alojamiento, fecha_inicio, fecha_fin, nota, id_solicitud) VALUES
(7, DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 100 DAY), 'Solicitud aceptada', 2);
INSERT INTO ocupaciones (id_alojamiento, fecha_inicio, fecha_fin, nota) VALUES
(2, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 40 DAY), 'Inquilino actual'),
(5, DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'Reservado'),
(5, DATE_ADD(CURDATE(), INTERVAL 75 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'Reservado'),
(11, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'Inquilino actual');
-- El alojamiento 9 está marcado como OCUPADO por el arrendador (interruptor manual)
UPDATE alojamientos SET disponible = FALSE WHERE id_alojamiento = 9;

CREATE INDEX idx_alojamiento_mascotas ON alojamientos(admite_mascotas);
CREATE INDEX idx_alojamiento_estado ON alojamientos(estado_publicacion);
CREATE INDEX idx_calificacion_calificado ON calificaciones(id_calificado);
CREATE INDEX idx_reporte_estado ON reportes(estado);
CREATE INDEX idx_ocupacion_fechas ON ocupaciones(id_alojamiento, fecha_inicio, fecha_fin);
