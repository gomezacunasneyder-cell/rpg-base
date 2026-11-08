package com.rpg.controller;

import com.rpg.dao.DaoException;
import com.rpg.dao.VictoriaDAO;
import com.rpg.model.Jugador;
import com.rpg.model.PuestoRanking;
import com.rpg.model.VictoriaDetalle;

import java.util.List;

/**
 * Controller de victorias y ranking. Tambien es quien registra una victoria cuando el jugador
 * gana un combate, para que el juego nunca hable directo con el DAO.
 */
public class RankingController {

    private final Jugador usuarioActual;
    private final VictoriaDAO victoriaDAO;

    public RankingController(Jugador usuarioActual) {
        this(usuarioActual, new VictoriaDAO());
    }

    public RankingController(Jugador usuarioActual, VictoriaDAO victoriaDAO) {
        this.usuarioActual = usuarioActual;
        this.victoriaDAO = victoriaDAO;
    }

    public Resultado<List<PuestoRanking>> obtenerRanking() {
        try {
            return Resultado.exito(victoriaDAO.rankingPorJugador());
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    /** Victorias de la persona que tiene la sesion abierta. */
    public Resultado<List<VictoriaDetalle>> misVictorias() {
        return victoriasDe(usuarioActual == null ? null : usuarioActual.getUsuario());
    }

    public Resultado<List<VictoriaDetalle>> victoriasDe(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return Resultado.error("Escribe el nombre del jugador.");
        }
        try {
            return Resultado.exito(victoriaDAO.victoriasDeJugador(usuario));
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    /** Registra que quien tiene la sesion abierta derroto a ese monstruo (fecha: ahora). */
    public Resultado<Void> registrarVictoria(int monstruoId) {
        if (usuarioActual == null || usuarioActual.getId() <= 0) {
            return Resultado.error("No hay una sesion iniciada para registrar la victoria.");
        }
        if (monstruoId <= 0) {
            return Resultado.error("El monstruo indicado no es valido.");
        }
        try {
            victoriaDAO.registrar(usuarioActual.getId(), monstruoId);
            return Resultado.exitoSinDato("Victoria registrada.");
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }
}
