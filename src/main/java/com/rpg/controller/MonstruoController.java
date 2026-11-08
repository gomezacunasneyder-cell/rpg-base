package com.rpg.controller;

import com.rpg.dao.DaoException;
import com.rpg.dao.MonstruoDAO;
import com.rpg.dao.VictoriaDAO;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;

import java.util.List;
import java.util.Optional;

/**
 * Controller del bestiario (tabla monstruos). Reglas de negocio de este modulo:
 *   - solo el rol admin puede crear, editar o eliminar monstruos (leer es libre);
 *   - no se puede eliminar un monstruo que ya tiene victorias registradas: se consulta al
 *     DAO ANTES de borrar, para dar un mensaje claro sin dejar que reviente la FOREIGN KEY;
 *   - no puede haber dos monstruos con el mismo nombre.
 *
 * El DAO solo guarda y lee; quien decide si algo se permite es este Controller.
 */
public class MonstruoController {

    private static final String SOLO_ADMIN = "Solo un administrador puede crear, editar o eliminar monstruos.";

    private final Jugador usuarioActual;
    private final MonstruoDAO monstruoDAO;
    private final VictoriaDAO victoriaDAO;

    public MonstruoController(Jugador usuarioActual) {
        this(usuarioActual, new MonstruoDAO(), new VictoriaDAO());
    }

    public MonstruoController(Jugador usuarioActual, MonstruoDAO monstruoDAO, VictoriaDAO victoriaDAO) {
        this.usuarioActual = usuarioActual;
        this.monstruoDAO = monstruoDAO;
        this.victoriaDAO = victoriaDAO;
    }

    /**
     * La View lo consulta antes de pedir datos, para no hacerle escribir un monstruo completo a
     * alguien que no tiene permiso. Aun asi, crear, editar y eliminar vuelven a comprobarlo.
     */
    public Resultado<Void> verificarPermisoAdmin() {
        if (usuarioActual == null || !usuarioActual.esAdmin()) {
            return Resultado.error(SOLO_ADMIN);
        }
        return Resultado.exitoSinDato("");
    }

    // ---- lectura (cualquier jugador)

    public Resultado<List<Monstruo>> listar() {
        try {
            return Resultado.exito(monstruoDAO.listar());
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    public Resultado<Monstruo> buscarPorId(int id) {
        try {
            Optional<Monstruo> monstruo = monstruoDAO.buscarPorId(id);
            if (monstruo.isEmpty()) {
                return Resultado.error("No existe un monstruo con id " + id + ".");
            }
            return Resultado.exito(monstruo.get());
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    public Resultado<Monstruo> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Resultado.error("Escribe el nombre a buscar.");
        }
        try {
            Optional<Monstruo> monstruo = monstruoDAO.buscarPorNombre(nombre);
            if (monstruo.isEmpty()) {
                return Resultado.error("No existe un monstruo llamado '" + nombre + "'.");
            }
            return Resultado.exito(monstruo.get());
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    public Resultado<List<Monstruo>> listarPorVidaMinima(int vidaMinima) {
        if (vidaMinima < 0) {
            return Resultado.error("La vida minima no puede ser negativa.");
        }
        try {
            return Resultado.exito(monstruoDAO.listarPorVidaMinima(vidaMinima));
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    // ---- escritura (solo admin)

    public Resultado<Void> crear(String nombre, int vida, int ataque) {
        Resultado<Void> permiso = verificarPermisoAdmin();
        if (permiso.esError()) {
            return permiso;
        }
        try {
            Monstruo nuevo = new Monstruo(0, nombre, vida, ataque);
            if (monstruoDAO.buscarPorNombre(nuevo.getNombre()).isPresent()) {
                return Resultado.error("Ya existe un monstruo llamado '" + nuevo.getNombre() + "'.");
            }
            monstruoDAO.insertar(nuevo);
            return Resultado.exitoSinDato("Monstruo '" + nuevo.getNombre() + "' creado.");
        } catch (IllegalArgumentException | DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    public Resultado<Void> editar(int id, String nombre, int vida, int ataque) {
        Resultado<Void> permiso = verificarPermisoAdmin();
        if (permiso.esError()) {
            return permiso;
        }
        try {
            if (monstruoDAO.buscarPorId(id).isEmpty()) {
                return Resultado.error("No existe un monstruo con id " + id + ".");
            }
            Monstruo cambios = new Monstruo(id, nombre, vida, ataque);
            Optional<Monstruo> conEseNombre = monstruoDAO.buscarPorNombre(cambios.getNombre());
            if (conEseNombre.isPresent() && conEseNombre.get().getId() != id) {
                return Resultado.error("Ya existe otro monstruo llamado '" + cambios.getNombre() + "'.");
            }
            if (monstruoDAO.actualizar(cambios) == 0) {
                return Resultado.error("El monstruo ya no existe, no se modifico nada.");
            }
            return Resultado.exitoSinDato("Monstruo '" + cambios.getNombre() + "' actualizado.");
        } catch (IllegalArgumentException | DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    public Resultado<Void> eliminar(int id) {
        Resultado<Void> permiso = verificarPermisoAdmin();
        if (permiso.esError()) {
            return permiso;
        }
        try {
            Optional<Monstruo> monstruo = monstruoDAO.buscarPorId(id);
            if (monstruo.isEmpty()) {
                return Resultado.error("No existe un monstruo con id " + id + ".");
            }
            // Regla de negocio: se pregunta al DAO ANTES de borrar.
            int victorias = victoriaDAO.contarPorMonstruo(id);
            if (victorias > 0) {
                return Resultado.error("No se puede eliminar a '" + monstruo.get().getNombre() + "': tiene "
                        + victorias + " victoria(s) registrada(s).");
            }
            if (monstruoDAO.eliminar(id) == 0) {
                return Resultado.error("El monstruo ya no existe, no se elimino nada.");
            }
            return Resultado.exitoSinDato("Monstruo '" + monstruo.get().getNombre() + "' eliminado.");
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }
}
