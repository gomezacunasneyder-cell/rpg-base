package com.rpg.dao;

/**
 * Error de la capa de acceso a datos con un mensaje pensado para el usuario.
 *
 * Los DAO capturan las SQLException de JDBC y las convierten en esta excepcion. Asi el Controller
 * y la View solo conocen DaoException y nunca necesitan importar java.sql (regla MVC del taller:
 * solo el paquete dao usa JDBC). La causa original se conserva con getCause() para depurar.
 */
public class DaoException extends Exception {

    private static final long serialVersionUID = 1L;

    public DaoException(String mensaje) {
        super(mensaje);
    }

    public DaoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
