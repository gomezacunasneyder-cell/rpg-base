-- =====================================================================
-- TALLER UNIDAD 5.2 - Arena Segura (ejercicio individual)
-- Base de datos: rpg_arena_db   |   Motor: MySQL (InnoDB, utf8mb4)
-- Ejecutar completo en DBeaver (Ctrl+Alt+Shift+X = ejecutar script).
-- Es seguro ejecutarlo mas de una vez: no borra nada que ya exista.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS rpg_arena_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpg_arena_db;

-- ---------------------------------------------------------------------
-- jugadores: quien inicia sesion.
--   usuario : unico (UNIQUE) para que no existan dos cuentas iguales.
--   clave   : guarda el HASH SHA-256 en hexadecimal (64 caracteres), nunca
--             el texto plano (reto extra del taller).
--   rol     : solo puede valer 'jugador' o 'admin' (ENUM). El valor por
--             defecto es 'jugador', asi nadie nace siendo administrador.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS jugadores (
    id      INT          NOT NULL AUTO_INCREMENT,
    usuario VARCHAR(30)  NOT NULL,
    clave   CHAR(64)     NOT NULL,
    rol     ENUM('jugador', 'admin') NOT NULL DEFAULT 'jugador',
    PRIMARY KEY (id),
    CONSTRAINT uq_jugadores_usuario UNIQUE (usuario)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- monstruos: el bestiario guardado en la base de datos.
--   nombre : unico. vida y ataque deben ser positivos (CHECK).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monstruos (
    id     INT         NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(40) NOT NULL,
    vida   INT         NOT NULL,
    ataque INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_monstruos_nombre UNIQUE (nombre),
    CONSTRAINT ck_monstruos_vida   CHECK (vida > 0),
    CONSTRAINT ck_monstruos_ataque CHECK (ataque > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- victorias: tabla que RELACIONA jugadores con monstruos (muchos a muchos:
-- un jugador vence a muchos monstruos y un monstruo es vencido por muchos
-- jugadores). Cada fila es una victoria con su fecha.
--   fk_victorias_jugador  ON DELETE CASCADE : si se elimina un jugador,
--       se eliminan tambien sus victorias (no tienen sentido sin el).
--   fk_victorias_monstruo ON DELETE RESTRICT: NO se puede borrar un monstruo
--       que ya tiene victorias. El Controller lo detecta antes (consultando
--       al DAO) para mostrar un mensaje claro; la FK es la red de seguridad.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS victorias (
    id          INT      NOT NULL AUTO_INCREMENT,
    jugador_id  INT      NOT NULL,
    monstruo_id INT      NOT NULL,
    fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_victorias_jugador  FOREIGN KEY (jugador_id)
        REFERENCES jugadores (id) ON DELETE CASCADE,
    CONSTRAINT fk_victorias_monstruo FOREIGN KEY (monstruo_id)
        REFERENCES monstruos (id) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Datos iniciales (INSERT IGNORE: si ya existen, no se duplican).
-- Cuentas de DESARROLLO para probar el juego; cambia la clave del admin
-- antes de usar esto fuera de una practica.
--   admin / admin123      (rol admin)
--   jugador1 / jugador123 (rol jugador)
-- La clave se guarda como SHA-256 en hex, igual que lo hace la aplicacion.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO jugadores (usuario, clave, rol) VALUES
    ('admin',    SHA2('admin123', 256),    'admin'),
    ('jugador1', SHA2('jugador123', 256),  'jugador');

-- Mismos monstruos (nombre, vida, ataque) que el Bestiario del juego.
INSERT IGNORE INTO monstruos (nombre, vida, ataque) VALUES
    ('Goblin',        50,  8),
    ('Arana Gigante', 60, 10),
    ('Ogro',         100, 14),
    ('Dragon Joven', 160, 20);
