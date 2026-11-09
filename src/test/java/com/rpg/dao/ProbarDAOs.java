package com.rpg.dao;

import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;
import com.rpg.model.PuestoRanking;
import com.rpg.model.VictoriaDetalle;
import com.rpg.util.ClaveHash;

import java.util.List;
import java.util.Optional;

/**
 * Prueba manual de los tres DAO contra la base de datos real (Parte C del taller).
 * Ejecutar con el boton verde de IntelliJ. Necesita config.txt y schema.sql ya ejecutado.
 *
 * Crea un jugador y un monstruo de prueba, ejercita cada metodo, imprime cada resultado y al final
 * BORRA sus propios datos de prueba (tambien al empezar, por si una ejecucion anterior se corto).
 * No toca los datos reales (admin, jugador1 y el bestiario).
 */
public class ProbarDAOs {

    private static final String USUARIO_PRUEBA = "prueba_dao";
    private static final String MONSTRUO_PRUEBA = "Monstruo de Prueba";

    private static int correctas = 0;
    private static int fallidas = 0;

    private interface Accion {
        void ejecutar() throws DaoException;
    }

    public static void main(String[] args) {
        JugadorDAO jugadores = new JugadorDAO();
        MonstruoDAO monstruos = new MonstruoDAO();
        VictoriaDAO victorias = new VictoriaDAO();

        try {
            limpiar(jugadores, monstruos);
            probarJugadores(jugadores);
            probarMonstruos(monstruos);
            probarVictorias(jugadores, monstruos, victorias);
        } catch (DaoException e) {
            fallidas++;
            System.out.println("ERROR inesperado de base de datos: " + e.getMessage());
        } finally {
            try {
                limpiar(jugadores, monstruos);
                System.out.println();
                System.out.println("Datos de prueba eliminados.");
            } catch (DaoException e) {
                fallidas++;
                System.out.println("No se pudieron borrar los datos de prueba: " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("RESULTADO: " + correctas + " verificaciones correctas, " + fallidas + " fallidas.");
    }

    private static void probarJugadores(JugadorDAO jugadores) throws DaoException {
        System.out.println("=== JugadorDAO ===");
        String hash = ClaveHash.sha256("clave123");

        verificar("registrar() inserta 1 fila",
                jugadores.registrar(new Jugador(0, USUARIO_PRUEBA, Jugador.ROL_JUGADOR), hash) == 1);

        String error = mensajeDeError(() ->
                jugadores.registrar(new Jugador(0, USUARIO_PRUEBA, Jugador.ROL_JUGADOR), hash));
        System.out.println("      mensaje al repetir usuario: " + error);
        verificar("registrar() rechaza un usuario repetido con mensaje claro", error != null);

        Optional<Jugador> encontrado = jugadores.autenticar(USUARIO_PRUEBA, hash);
        verificar("autenticar() con clave correcta devuelve el jugador", encontrado.isPresent());
        verificar("autenticar() con clave incorrecta no entra",
                jugadores.autenticar(USUARIO_PRUEBA, ClaveHash.sha256("otra")).isEmpty());
        verificar("autenticar() con inyeccion SQL no entra",
                jugadores.autenticar("admin' OR 1=1 -- ", hash).isEmpty());

        int id = encontrado.map(Jugador::getId).orElse(-1);
        verificar("cambiarRol() a admin afecta 1 fila", jugadores.cambiarRol(id, Jugador.ROL_ADMIN) == 1);
        verificar("el rol cambio de verdad",
                jugadores.autenticar(USUARIO_PRUEBA, hash).map(Jugador::esAdmin).orElse(false));
        verificar("cambiarRol() con id inexistente afecta 0 filas", jugadores.cambiarRol(999999, Jugador.ROL_ADMIN) == 0);
    }

    private static void probarMonstruos(MonstruoDAO monstruos) throws DaoException {
        System.out.println("=== MonstruoDAO ===");
        verificar("insertar() inserta 1 fila",
                monstruos.insertar(new Monstruo(0, MONSTRUO_PRUEBA, 77, 7)) == 1);

        String error = mensajeDeError(() -> monstruos.insertar(new Monstruo(0, MONSTRUO_PRUEBA, 1, 1)));
        System.out.println("      mensaje al repetir nombre: " + error);
        verificar("insertar() rechaza un nombre repetido con mensaje claro", error != null);

        Optional<Monstruo> porNombre = monstruos.buscarPorNombre(MONSTRUO_PRUEBA);
        verificar("buscarPorNombre() lo encuentra con sus datos",
                porNombre.isPresent() && porNombre.get().getVida() == 77 && porNombre.get().getAtaque() == 7);
        verificar("buscarPorNombre() con inyeccion SQL no devuelve nada",
                monstruos.buscarPorNombre("x' OR '1'='1").isEmpty());

        int id = porNombre.map(Monstruo::getId).orElse(-1);
        verificar("buscarPorId() lo encuentra", monstruos.buscarPorId(id).isPresent());
        verificar("listar() incluye el monstruo nuevo",
                monstruos.listar().stream().anyMatch(m -> m.getId() == id));

        List<Monstruo> fuertes = monstruos.listarPorVidaMinima(77);
        System.out.println("      monstruos con vida >= 77: " + fuertes.size());
        verificar("listarPorVidaMinima(77) trae solo vida >= 77 e incluye al de prueba",
                fuertes.stream().allMatch(m -> m.getVida() >= 77) && fuertes.stream().anyMatch(m -> m.getId() == id));

        verificar("actualizar() afecta 1 fila", monstruos.actualizar(new Monstruo(id, MONSTRUO_PRUEBA, 88, 9)) == 1);
        verificar("la actualizacion se guardo",
                monstruos.buscarPorId(id).map(m -> m.getVida() == 88 && m.getAtaque() == 9).orElse(false));
    }

    private static void probarVictorias(JugadorDAO jugadores, MonstruoDAO monstruos, VictoriaDAO victorias)
            throws DaoException {
        System.out.println("=== VictoriaDAO ===");
        int jugadorId = jugadores.autenticar(USUARIO_PRUEBA, ClaveHash.sha256("clave123"))
                .map(Jugador::getId).orElse(-1);
        int monstruoId = monstruos.buscarPorNombre(MONSTRUO_PRUEBA).map(Monstruo::getId).orElse(-1);

        verificar("registrar() inserta la victoria 1", victorias.registrar(jugadorId, monstruoId) == 1);
        verificar("registrar() inserta la victoria 2", victorias.registrar(jugadorId, monstruoId) == 1);

        String error = mensajeDeError(() -> victorias.registrar(999999, monstruoId));
        System.out.println("      mensaje con jugador inexistente: " + error);
        verificar("registrar() con jugador inexistente da mensaje claro", error != null);

        List<PuestoRanking> ranking = victorias.rankingPorJugador();
        System.out.println("      ranking actual:");
        for (PuestoRanking puesto : ranking) {
            System.out.println("        " + puesto.getUsuario() + " -> " + puesto.getTotalVictorias() + " victorias");
        }
        verificar("rankingPorJugador() incluye al jugador de prueba con 2 victorias",
                ranking.stream().anyMatch(p -> p.getUsuario().equals(USUARIO_PRUEBA) && p.getTotalVictorias() == 2));
        boolean ordenado = true;
        for (int i = 1; i < ranking.size(); i++) {
            if (ranking.get(i - 1).getTotalVictorias() < ranking.get(i).getTotalVictorias()) {
                ordenado = false;
            }
        }
        verificar("el ranking esta ordenado de mayor a menor", ordenado);

        List<VictoriaDetalle> detalle = victorias.victoriasDeJugador(USUARIO_PRUEBA);
        for (VictoriaDetalle victoria : detalle) {
            System.out.println("      victoria: " + victoria.getMonstruo() + " el " + victoria.getFecha());
        }
        verificar("victoriasDeJugador() devuelve 2 victorias con nombre de monstruo",
                detalle.size() == 2 && detalle.get(0).getMonstruo().equals(MONSTRUO_PRUEBA));
        verificar("victoriasDeJugador() con inyeccion SQL no devuelve nada",
                victorias.victoriasDeJugador("x' OR '1'='1").isEmpty());

        verificar("contarPorMonstruo() cuenta 2", victorias.contarPorMonstruo(monstruoId) == 2);
        String errorBorrado = mensajeDeError(() -> monstruos.eliminar(monstruoId));
        System.out.println("      mensaje al borrar un monstruo con victorias: " + errorBorrado);
        verificar("eliminar() un monstruo con victorias es rechazado por la base de datos", errorBorrado != null);
    }

    /** Borra los datos de prueba: primero el jugador (sus victorias caen en cascada), luego el monstruo. */
    private static void limpiar(JugadorDAO jugadores, MonstruoDAO monstruos) throws DaoException {
        Optional<Jugador> jugador = jugadores.autenticar(USUARIO_PRUEBA, ClaveHash.sha256("clave123"));
        if (jugador.isPresent()) {
            jugadores.eliminar(jugador.get().getId());
        }
        Optional<Monstruo> monstruo = monstruos.buscarPorNombre(MONSTRUO_PRUEBA);
        if (monstruo.isPresent()) {
            monstruos.eliminar(monstruo.get().getId());
        }
    }

    private static String mensajeDeError(Accion accion) {
        try {
            accion.ejecutar();
            return null;
        } catch (DaoException e) {
            return e.getMessage();
        }
    }

    private static void verificar(String descripcion, boolean condicion) {
        if (condicion) {
            correctas++;
            System.out.println("  [OK]    " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLA] " + descripcion);
        }
    }
}
