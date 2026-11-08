package com.rpg.core;

import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.controller.MonstruoController;
import com.rpg.controller.RankingController;
import com.rpg.controller.Resultado;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;
import com.rpg.persistencia.AutoguardadoEnDisco;
import com.rpg.persistencia.EscritorLog;
import com.rpg.persistencia.GestorArchivos;
import com.rpg.persistencia.GestorPartida;
import com.rpg.persistencia.LectorLog;
import com.rpg.persistencia.PartidaGuardada;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario y guardado. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final long INTERVALO_AUTOGUARDADO_MS = 5000;
    private static final long ESPERA_CIERRE_AUTOGUARDADO_MS = 2000;
    private static final int SEGUNDOS_TURNO = 10;
    private static final boolean AGREGAR_AL_LOG = true;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Consola consola;
    // Cuenta con la que se inicio sesion (no es el heroe del combate). Los controllers hablan con la BD.
    private final MonstruoController monstruos;
    private final RankingController ranking;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private final GestorArchivos archivos = new GestorArchivos();
    private final EscritorLog escritorLog = new EscritorLog();
    private final LectorLog lectorLog = new LectorLog();
    private final GestorPartida gestorPartida = new GestorPartida();
    private final AutoguardadoEnDisco autoguardado = new AutoguardadoEnDisco(archivos.archivoAutoguardado());
    private Thread hiloAutoGuardado;
    // TALLER ARCHIVOS: ultimoGuardado es solo el estado de la partida en memoria. Aqui (o en un metodo
    // nuevo como cargarProgreso) iria la carga de una partida guardada en disco; llamala desde Main.
    private volatile String ultimoGuardado = "";

    public MotorJuego(Consola consola, Jugador cuenta) {
        this.consola = consola;
        this.monstruos = new MonstruoController(cuenta);
        this.ranking = new RankingController(cuenta);
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        // Los rivales salen de la tabla monstruos. Se cargan primero para no pedir el heroe si falla la BD.
        Resultado<List<Monstruo>> rivales = monstruos.listar();
        if (rivales.esError()) {
            System.out.println(rivales.getMensaje());
            return;
        }
        if (rivales.getDato().isEmpty()) {
            System.out.println("No hay monstruos en la base de datos. Un administrador debe crear alguno.");
            return;
        }
        registro.limpiar();
        Personaje jugador = crearJugador();
        Monstruo rival = elegirRival(rivales.getDato());
        Enemigo enemigo = bestiario.crearEnemigo(rival);
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);
        jugador.cancelarEfectos();
        enemigo.cancelarEfectos();

        cerrarCombate(jugador, enemigo, rival, huyo);
        guardarProgreso();
        guardarLogCombate();
        guardarPartidaEnDisco();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        guardarProgreso();
        guardarLogCombate();
        guardarPartidaEnDisco();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    public void mostrarInfoArchivos() {
        System.out.println("\n=== ARCHIVOS DE PARTIDA ===");
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File log = archivos.archivoLog();
        archivos.crearSiNoExiste(log);
        System.out.println("-- Con ruta relativa --");
        System.out.println(archivos.describir(log));
        System.out.println("-- Con ruta absoluta --");
        System.out.println(archivos.describir(new File(log.getAbsolutePath())));
    }

    public void verHistorial() {
        System.out.println("\n=== HISTORIAL DE COMBATES ===");
        lectorLog.mostrar(archivos.archivoLog());
    }

    public void verAutoguardado() {
        System.out.println("\n=== ULTIMO AUTOGUARDADO ===");
        autoguardado.mostrar();
    }

    public void borrarPartidaGuardada() {
        System.out.println("\n=== BORRAR PARTIDA GUARDADA ===");
        File[] guardados = {archivos.archivoLog(), archivos.archivoPartida()};
        boolean hayAlgo = false;
        for (File archivo : guardados) {
            if (archivo.exists()) {
                System.out.println("- " + archivo.getPath() + " (" + archivo.length() + " bytes)");
                hayAlgo = true;
            }
        }
        if (!hayAlgo) {
            System.out.println("No hay partida guardada que borrar.");
            return;
        }
        String respuesta = consola.leerLinea("Seguro que quieres borrar la partida guardada? Esto no se puede deshacer (s/n): ");
        if (!respuesta.equalsIgnoreCase("s")) {
            System.out.println("Borrado cancelado. No se toco ningun archivo.");
            return;
        }
        for (File archivo : guardados) {
            archivos.borrarSiExiste(archivo);
        }
    }

    public synchronized void cargarPartida() {
        System.out.println("\n=== CARGAR PARTIDA ===");
        PartidaGuardada partida = gestorPartida.cargar(archivos.archivoPartida());
        if (partida == null) {
            return;
        }
        gremio = partida.getGremio();
        perfil = partida.getPerfil();
        bestiario.restaurarDerrotados(partida.getDerrotados());
        System.out.println("Partida guardada el " + partida.getFecha().format(FORMATO_FECHA));
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
        System.out.printf("Gremio %s (%d miembros):%n", gremio.getNombre(), gremio.getMiembros().size());
        for (Personaje miembro : gremio.getMiembros()) {
            boolean relacionado = miembro.getGremio() == gremio;
            System.out.printf("  %s | Pociones: %d | Gremio enlazado: %s%n", FormatoCombate.resumen(miembro),
                    miembro.getInventario().tamanio(), relacionado ? "si" : "no");
        }
        System.out.println("Enemigos derrotados: " + bestiario.resumenDerrotados());
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Monstruo elegirRival(List<Monstruo> rivales) {
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < rivales.size(); i++) {
            Monstruo monstruo = rivales.get(i);
            System.out.printf("  %d. %s (vida %d, ataque %d)%n", i + 1, monstruo.getNombre(),
                    monstruo.getVida(), monstruo.getAtaque());
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Monstruo rival = resolverRival(texto, rivales);
            if (rival != null) {
                return rival;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Monstruo resolverRival(String texto, List<Monstruo> rivales) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= rivales.size();
            return valido ? rivales.get(numero - 1) : null;
        } catch (NumberFormatException e) {
            for (Monstruo monstruo : rivales) {
                if (monstruo.getNombre().equalsIgnoreCase(texto)) {
                    return monstruo;
                }
            }
            return null;
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion();
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                case TIEMPO_AGOTADO -> true;
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * Pide la accion al jugador y se queda esperando hasta que escriba algo.
     */
    private Accion pedirAccion() {
        // TALLER CONCURRENCIA: esta lectura bloquea el programa sin limite de tiempo.
        // Aqui debe entrar el temporizador de turno con su callback.
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");

        GestorTurno gestor = new GestorTurno();
        gestor.iniciar(SEGUNDOS_TURNO,
                () -> System.out.println("\nTiempo agotado! Pierdes el turno (pulsa Enter para continuar)."));

        int opcion = consola.leerEntero("Accion: ", 0, 4);

        if (!gestor.cancelar()) {
            return Accion.TIEMPO_AGOTADO;
        }
        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d turnos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, Monstruo rival, boolean huyo) {
        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
            registrarVictoriaEnRanking(rival);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    /** Integracion con la base de datos: toda victoria contra un monstruo de la tabla queda registrada. */
    private void registrarVictoriaEnRanking(Monstruo rival) {
        Resultado<Void> resultado = ranking.registrarVictoria(rival.getId());
        if (resultado.esExito()) {
            registro.agregar("Victoria registrada en el ranking (rival: %s).", rival.getNombre());
        } else {
            registro.agregar("No se pudo registrar la victoria en la base de datos: %s", resultado.getMensaje());
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    private void guardarLogCombate() {
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File log = archivos.archivoLog();
        String encabezado = "=== Registro guardado el " + LocalDateTime.now().format(FORMATO_FECHA) + " ===";
        if (escritorLog.escribir(log, encabezado, registro.getTexto(), AGREGAR_AL_LOG)) {
            System.out.println("[Log de combate guardado en " + log.getPath() + " - " + log.length() + " bytes]");
        }
    }

    private synchronized void guardarPartidaEnDisco() {
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File archivo = archivos.archivoPartida();
        PartidaGuardada partida = new PartidaGuardada(gremio, perfil, bestiario.getDerrotados());
        if (gestorPartida.guardar(archivo, partida)) {
            System.out.println("[Partida guardada en " + archivo.getPath() + " - " + archivo.length() + " bytes]");
        }
    }

    /**
     * Guarda las estadisticas en memoria (el texto queda en {@code ultimoGuardado}); aun no se escribe ningun
     * archivo. Hoy solo se invoca al terminar una pelea.
     */
    public synchronized String guardarProgreso() {
        // TALLER ARCHIVOS: ademas de dejar el texto en memoria, escribe la partida en disco (por ejemplo un
        // .txt con BufferedWriter, y/o un .dat serializando el estado con ObjectOutputStream). Usa try-with-resources.
        // TALLER CONCURRENCIA: este guardado es manual. Debera hacerse solo, de fondo, mientras se juega.
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        for (Personaje miembro : gremio.getMiembros()) {
            contenido.append("  ").append(FormatoCombate.resumen(miembro)).append(System.lineSeparator());
        }
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());

        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();
        System.out.println("[Progreso guardado en memoria]");
        return ultimoGuardado;
    }
    private String autoguardar() {
        String texto = guardarProgreso();
        if (autoguardado.escribir(texto)) {
            File archivo = autoguardado.getArchivo();
            System.out.println("[Autoguardado en disco: " + archivo.getPath() + " - " + archivo.length() + " bytes]");
        }
        return texto;
    }

    public void iniciarAutoGuardado() {
        Thread hilo = new Thread(new AutoGuardadoDaemon(INTERVALO_AUTOGUARDADO_MS, this::autoguardar), "AutoGuardado");
        hilo.setDaemon(true);
        hilo.start();
        hiloAutoGuardado = hilo;
    }

    /** Al salir: interrumpe el daemon, espera a que termine la escritura en curso y guarda por ultima vez. */
    public void detenerAutoGuardado() {
        Thread hilo = hiloAutoGuardado;
        if (hilo == null) {
            return;
        }
        hilo.interrupt();
        try {
            hilo.join(ESPERA_CIERRE_AUTOGUARDADO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        autoguardar();
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }
}
