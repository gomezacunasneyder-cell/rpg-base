package com.rpg.dao;

import com.rpg.model.Monstruo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO de la tabla monstruos: CRUD completo mas buscarPorNombre y listarPorVidaMinima.
 * Todo con PreparedStatement. Modificar (INSERT/UPDATE/DELETE) -> executeUpdate, que devuelve las
 * filas afectadas. Leer (SELECT) -> executeQuery, que se convierte en objetos Monstruo.
 *
 * Aqui NO se valida quien puede hacer cada cosa: que solo el admin edite monstruos es una regla de
 * negocio y vive en el Controller.
 */
public class MonstruoDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO monstruos (nombre, vida, ataque) VALUES (?, ?, ?)";
    private static final String SQL_LISTAR =
            "SELECT id, nombre, vida, ataque FROM monstruos ORDER BY id";
    private static final String SQL_BUSCAR_POR_ID =
            "SELECT id, nombre, vida, ataque FROM monstruos WHERE id = ?";
    private static final String SQL_BUSCAR_POR_NOMBRE =
            "SELECT id, nombre, vida, ataque FROM monstruos WHERE nombre = ?";
    private static final String SQL_LISTAR_POR_VIDA_MINIMA =
            "SELECT id, nombre, vida, ataque FROM monstruos WHERE vida >= ? ORDER BY vida, nombre";
    private static final String SQL_ACTUALIZAR =
            "UPDATE monstruos SET nombre = ?, vida = ?, ataque = ? WHERE id = ?";
    private static final String SQL_ELIMINAR =
            "DELETE FROM monstruos WHERE id = ?";

    /** CREATE. Devuelve las filas insertadas (1). Si el nombre ya existe, informa con un mensaje claro. */
    public int insertar(Monstruo monstruo) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_INSERTAR)) {
            consulta.setString(1, monstruo.getNombre());
            consulta.setInt(2, monstruo.getVida());
            consulta.setInt(3, monstruo.getAtaque());
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("guardar el monstruo", e);
        }
    }

    /** READ (todos). Devuelve una lista de objetos Monstruo, nunca un ResultSet. */
    public List<Monstruo> listar() throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR)) {
            return leerLista(consulta);
        } catch (SQLException e) {
            throw ErroresDao.traducir("listar los monstruos", e);
        }
    }

    /** READ (uno por id). Optional.empty() si no existe. */
    public Optional<Monstruo> buscarPorId(int id) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {
            consulta.setInt(1, id);
            return leerUno(consulta);
        } catch (SQLException e) {
            throw ErroresDao.traducir("buscar el monstruo", e);
        }
    }

    /** Busca por nombre exacto (el nombre es UNIQUE, asi que hay como maximo uno). */
    public Optional<Monstruo> buscarPorNombre(String nombre) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_BUSCAR_POR_NOMBRE)) {
            consulta.setString(1, nombre);
            return leerUno(consulta);
        } catch (SQLException e) {
            throw ErroresDao.traducir("buscar el monstruo", e);
        }
    }

    /** Monstruos con vida mayor o igual al minimo indicado, del mas debil al mas fuerte. */
    public List<Monstruo> listarPorVidaMinima(int vidaMinima) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_LISTAR_POR_VIDA_MINIMA)) {
            consulta.setInt(1, vidaMinima);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw ErroresDao.traducir("listar los monstruos por vida", e);
        }
    }

    /** UPDATE. Devuelve las filas afectadas (0 si no existe ese id). */
    public int actualizar(Monstruo monstruo) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_ACTUALIZAR)) {
            consulta.setString(1, monstruo.getNombre());
            consulta.setInt(2, monstruo.getVida());
            consulta.setInt(3, monstruo.getAtaque());
            consulta.setInt(4, monstruo.getId());
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("actualizar el monstruo", e);
        }
    }

    /**
     * DELETE. Devuelve las filas afectadas (0 si no existe ese id). Si el monstruo tiene victorias, la
     * FOREIGN KEY lo impide; el Controller lo detecta ANTES (VictoriaDAO.contarPorMonstruo) para dar
     * un mensaje claro, y esto es solo la red de seguridad.
     */
    public int eliminar(int id) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_ELIMINAR)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("eliminar el monstruo", e);
        }
    }

    /** executeQuery + while (rs.next()): cada fila se convierte en un objeto Monstruo. */
    private static List<Monstruo> leerLista(PreparedStatement consulta) throws SQLException {
        List<Monstruo> monstruos = new ArrayList<>();
        try (ResultSet resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                monstruos.add(mapear(resultado));
            }
        }
        return monstruos;
    }

    private static Optional<Monstruo> leerUno(PreparedStatement consulta) throws SQLException {
        try (ResultSet resultado = consulta.executeQuery()) {
            if (resultado.next()) {
                return Optional.of(mapear(resultado));
            }
            return Optional.empty();
        }
    }

    private static Monstruo mapear(ResultSet resultado) throws SQLException {
        return new Monstruo(
                resultado.getInt("id"),
                resultado.getString("nombre"),
                resultado.getInt("vida"),
                resultado.getInt("ataque"));
    }
}
