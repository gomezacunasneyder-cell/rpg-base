package com.rpg.dao;

import com.rpg.model.Jugador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO de la tabla jugadores. Aqui (y solo en el paquete dao) vive el codigo JDBC.
 * Todas las consultas usan PreparedStatement con comodines ?; nunca se concatena texto del usuario.
 */
public class JugadorDAO {

    private static final String SQL_AUTENTICAR =
            "SELECT id, usuario, rol FROM jugadores WHERE usuario = ? AND clave = ?";

    /**
     * Valida un login. Devuelve el jugador si usuario y clave coinciden, o Optional.empty() si no.
     *
     * @param claveHash la clave YA convertida a hash SHA-256 (la conversion es responsabilidad del
     *                  Controller, ver ClaveHash); el DAO solo habla con la base de datos.
     *
     * POR QUE ESTA CONSULTA RESISTE LA INYECCION (diapositiva "El Escudo")
     * La consulta se escribe una sola vez con comodines: "... WHERE usuario = ? AND clave = ?".
     * Los ? reservan espacios estrictamente para DATOS, nunca para logica ejecutable. Los valores se
     * entregan por separado con setString. Si alguien escribe  admin' OR 1=1 --  como usuario, la base
     * de datos busca un usuario cuyo nombre literal sea exactamente  admin' OR 1=1 --  : no existe, el
     * login falla y la logica de la consulta (el AND con la clave) permanece intacta. El texto
     * escrito por el usuario nunca llega a ser parte de las instrucciones SQL.
     */
    public Optional<Jugador> autenticar(String usuario, String claveHash) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_AUTENTICAR)) {
            consulta.setString(1, usuario);
            consulta.setString(2, claveHash);
            // SELECT -> executeQuery (devuelve un ResultSet). Se cierra solo al salir del bloque.
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo validar el login en la base de datos: " + e.getMessage(), e);
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Java; el ResultSet nunca sale del DAO. */
    private static Jugador mapear(ResultSet resultado) throws SQLException {
        return new Jugador(
                resultado.getInt("id"),
                resultado.getString("usuario"),
                resultado.getString("rol"));
    }
}
