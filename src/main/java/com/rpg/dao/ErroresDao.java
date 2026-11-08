package com.rpg.dao;

import java.sql.SQLException;

/**
 * Traduce los errores de JDBC (SQLException) a DaoException con un mensaje claro para el
 * usuario. Asi cada DAO no repite la misma logica y el Controller nunca ve java.sql.
 *
 * Los codigos son los de MySQL. Sirven de red de seguridad: las reglas de negocio se validan
 * antes en el Controller, y esto cubre lo que la base de datos rechaza por si sola.
 */
final class ErroresDao {

    private static final int DUPLICADO = 1062;           // valor repetido en una columna UNIQUE
    private static final int FK_TIENE_HIJOS = 1451;      // borrar una fila que otra tabla referencia
    private static final int FK_PADRE_INEXISTENTE = 1452; // insertar apuntando a una fila que no existe

    private ErroresDao() {
    }

    static DaoException traducir(String accion, SQLException e) {
        String mensaje;
        switch (e.getErrorCode()) {
            case DUPLICADO:
                mensaje = "No se pudo " + accion + ": ya existe un registro con ese usuario o nombre.";
                break;
            case FK_TIENE_HIJOS:
                mensaje = "No se pudo " + accion + ": tiene datos relacionados (por ejemplo victorias) "
                        + "y no se puede eliminar.";
                break;
            case FK_PADRE_INEXISTENTE:
                mensaje = "No se pudo " + accion + ": el jugador o el monstruo indicado no existe.";
                break;
            default:
                mensaje = "No se pudo " + accion + " en la base de datos: " + e.getMessage();
                break;
        }
        return new DaoException(mensaje, e);
    }
}
