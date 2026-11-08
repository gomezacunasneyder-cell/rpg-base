package com.rpg.dao;

import com.rpg.model.PuestoRanking;
import com.rpg.model.VictoriaDetalle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de la tabla victorias (relaciona jugadores con monstruos). Todo con PreparedStatement.
 * Las consultas de lectura usan JOIN para mostrar nombres en vez de ids, y se convierten en
 * objetos Java (PuestoRanking, VictoriaDetalle): el ResultSet nunca sale del DAO.
 */
public class VictoriaDAO {

    private static final String SQL_REGISTRAR =
            "INSERT INTO victorias (jugador_id, monstruo_id, fecha) VALUES (?, ?, ?)";

    // JOIN une victorias con jugadores por la clave foranea; GROUP BY agrupa las victorias de cada
    // jugador y COUNT las cuenta. Se ordena de mayor a menor (desempate por usuario).
    private static final String SQL_RANKING =
            "SELECT j.usuario, COUNT(v.id) AS total FROM jugadores j JOIN victorias v ON v.jugador_id = j.id GROUP BY j.id, j.usuario ORDER BY total DESC, j.usuario ASC";

    // Dos JOIN: victorias -> jugadores (para filtrar por usuario) y victorias -> monstruos (para el nombre).
    private static final String SQL_VICTORIAS_DE_JUGADOR =
            "SELECT m.nombre, v.fecha FROM victorias v JOIN jugadores j ON j.id = v.jugador_id JOIN monstruos m ON m.id = v.monstruo_id WHERE j.usuario = ? ORDER BY v.fecha DESC, v.id DESC";

    private static final String SQL_CONTAR_POR_MONSTRUO =
            "SELECT COUNT(*) AS total FROM victorias WHERE monstruo_id = ?";

    /**
     * Registra una victoria con la fecha actual. executeUpdate devuelve las filas insertadas (1).
     * La fecha se envia como Timestamp por parametro (hora de este equipo), asi no depende de la
     * zona horaria del servidor MySQL. Si el jugador o el monstruo no existen, la FOREIGN KEY lo
     * rechaza y se informa con un mensaje claro.
     */
    public int registrar(int jugadorId, int monstruoId) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_REGISTRAR)) {
            consulta.setInt(1, jugadorId);
            consulta.setInt(2, monstruoId);
            consulta.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return consulta.executeUpdate();
        } catch (SQLException e) {
            throw ErroresDao.traducir("registrar la victoria", e);
        }
    }

    /** Ranking: usuario y total de victorias, de mayor a menor. Solo aparecen quienes tienen victorias. */
    public List<PuestoRanking> rankingPorJugador() throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_RANKING);
             ResultSet resultado = consulta.executeQuery()) {
            List<PuestoRanking> ranking = new ArrayList<>();
            while (resultado.next()) {
                ranking.add(new PuestoRanking(
                        resultado.getString("usuario"),
                        resultado.getInt("total")));
            }
            return ranking;
        } catch (SQLException e) {
            throw ErroresDao.traducir("consultar el ranking", e);
        }
    }

    /** Monstruo y fecha de cada victoria de un jugador, de la mas reciente a la mas antigua. */
    public List<VictoriaDetalle> victoriasDeJugador(String usuario) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_VICTORIAS_DE_JUGADOR)) {
            consulta.setString(1, usuario);
            List<VictoriaDetalle> victorias = new ArrayList<>();
            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    victorias.add(new VictoriaDetalle(
                            resultado.getString("nombre"),
                            resultado.getTimestamp("fecha").toLocalDateTime()));
                }
            }
            return victorias;
        } catch (SQLException e) {
            throw ErroresDao.traducir("consultar las victorias del jugador", e);
        }
    }

    /**
     * Cuantas victorias tiene registradas un monstruo. El Controller la usa para no permitir
     * eliminar un monstruo con victorias, antes de que la FOREIGN KEY reviente.
     */
    public int contarPorMonstruo(int monstruoId) throws DaoException {
        try (Connection conexion = ConexionDB.obtener();
             PreparedStatement consulta = conexion.prepareStatement(SQL_CONTAR_POR_MONSTRUO)) {
            consulta.setInt(1, monstruoId);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() ? resultado.getInt("total") : 0;
            }
        } catch (SQLException e) {
            throw ErroresDao.traducir("contar las victorias del monstruo", e);
        }
    }
}
