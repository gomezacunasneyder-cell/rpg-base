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
 *
 * Las claves llegan YA convertidas a hash SHA-256 (ver ClaveHash): la conversion es
 * responsabilidad del Controller; el DAO solo habla con la base de datos.
 *
 * Los metodos que modifican (INSERT / UPDATE / DELETE) usan executeUpdate y devuelven el numero
 * de filas afectadas (0 = no existia ese id). Los que leen (SELECT) usan executeQuery y devuelven
 * objetos Java: el ResultSet nunca sale del DAO.
 */
public class JugadorDAO {

    private static final String SQL_REGISTRAR =
            "INSERT INTO jugadores (usuario, clave, rol) VALUES (?, ?, ?)";
    private static final String SQL_AUTENTICAR =
            "SELECT id, usuario, rol FROM jugadores WHERE usuario = ? AND clave = ?";
    private static final String SQL_CAMBIAR_ROL =
            "UPDATE jugadores SET rol = ? WHERE id = ?";
    private static final String SQL_ELIMINAR =
            "DELETE FROM jugadores WHERE id = ?";

    /**
     * Inserta un jugador nuevo. executeUpdate devuelve las filas insertadas (1).
     * Si el usuario ya existe, la columna UNIQUE lo rechaza y se informa con un mensaje claro.
     */
    public int registrar(Jugador jugador, String claveHash) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_REGISTRAR)) {
            consulta.setString(1, jugador.getUsuario());
            consulta.setString(2, claveHash);
            consulta.setString(3, jugador.getRol());
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("registrar el jugador", e);
        }
    }

    /**
     * Valida un login. Devuelve el jugador si usuario y clave coinciden, o Optional.empty() si no.
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
            throw ErroresDao.traducir("validar el login", e);
        }
    }

    /** Cambia el rol de un jugador. Devuelve las filas afectadas (0 si no existe ese id). */
    public int cambiarRol(int id, String rol) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_CAMBIAR_ROL)) {
            consulta.setString(1, rol);
            consulta.setInt(2, id);
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("cambiar el rol", e);
        }
    }

    /**
     * Elimina un jugador. Devuelve las filas afectadas (0 si no existe ese id).
     * Sus victorias se eliminan tambien (ON DELETE CASCADE en schema.sql).
     */
    public int eliminar(int id) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_ELIMINAR)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("eliminar el jugador", e);
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
