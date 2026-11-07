package com.rpg.entities;

import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.entities.enums.EstadoPersonaje;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// TALLER ARCHIVOS: para guardar la partida con ObjectOutputStream, Personaje debe volverse Serializable
// (con serialVersionUID). Piensa que campos son transient y que clases anidadas (Inventario, Pocion, Arma,
// Gremio, EstadoEfecto, enums) tambien deben ser Serializable, o tendras NotSerializableException.
// Ojo: Gremio y Personaje se referencian entre si (la serializacion maneja el ciclo).
public abstract class Personaje implements ICombatiente {

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;
    private static final long INTERVALO_EFECTO_MS = 2000;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();
    private final Map<String, Thread> hilosEfectos = new ConcurrentHashMap<>();
    private volatile Consumer<String> oyenteEfectos = System.out::println;

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida

    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        // TALLER CONCURRENCIA: lee, calcula y escribe la vida en pasos separados sin proteccion.
        // Con varios hilos sobre el mismo Personaje (aqui y en curar), el resultado no es seguro.
        setVida(vida - cantidad);
    }

    /** @return vida realmente recuperada */
    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + cantidad);
        return vida - vidaAntes;
    }

    public synchronized void restaurarVida() {
        cancelarEfectos();
        estado = EstadoPersonaje.VIVO;
        vida = vidaMaxima;
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT (cada efecto corre en su propio hilo y se cancela con interrupt)

    public void setOyenteEfectos(Consumer<String> oyente) {
        this.oyenteEfectos = oyente;
    }

    public synchronized void aplicarEfecto(EstadoEfecto efecto) {
        EstadoEfecto activo = efecto.copia();
        Thread anterior = hilosEfectos.remove(activo.getNombre());
        if (anterior != null) {
            anterior.interrupt();
        }
        efectos.put(activo.getNombre(), activo);
        if (activo.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        Thread hilo = new Thread(new EfectoContinuoTask(this, activo, INTERVALO_EFECTO_MS),
                "Efecto-" + activo.getNombre() + "-" + nombre);
        hilosEfectos.put(activo.getNombre(), hilo);
        hilo.start();
    }

    public synchronized boolean quitarEfecto(String nombreEfecto) {
        Thread hilo = hilosEfectos.remove(nombreEfecto);
        if (hilo != null) {
            hilo.interrupt();
        }
        if (efectos.remove(nombreEfecto) == null) {
            return false;
        }
        normalizarEstado();
        return true;
    }

    public synchronized void quitarEfectosDanio() {
        for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
            EstadoEfecto efecto = efectos.get(nombreEfecto);
            if (efecto != null && efecto.esDanio()) {
                quitarEfecto(nombreEfecto);
            }
        }
    }

    /** Interrumpe todos los hilos de efecto y deja al personaje sin efectos (fin de combate). */
    public synchronized void cancelarEfectos() {
        for (String nombreEfecto : new ArrayList<>(hilosEfectos.keySet())) {
            Thread hilo = hilosEfectos.remove(nombreEfecto);
            if (hilo != null) {
                hilo.interrupt();
            }
        }
        efectos.clear();
        normalizarEstado();
    }

    /**
     * Un tick de un efecto, llamado desde su hilo.
     * @return true si el efecto sigue activo y el hilo debe continuar
     */
    public boolean aplicarTickEfecto(EstadoEfecto efecto) {
        String mensajeTick;
        String mensajeFin = null;
        boolean continuar;
        synchronized (this) {
            if (efectos.get(efecto.getNombre()) != efecto || !estaVivo()) {
                limpiarEfecto(efecto);
                return false;
            }
            mensajeTick = aplicarTick(efecto);
            efecto.consumirTurno();
            if (efecto.haTerminado()) {
                mensajeFin = nombre + ": termina " + efecto.getNombre();
            }
            continuar = !efecto.haTerminado() && estaVivo();
            if (!continuar) {
                limpiarEfecto(efecto);
            }
        }
        oyenteEfectos.accept(mensajeTick);
        if (mensajeFin != null) {
            oyenteEfectos.accept(mensajeFin);
        }
        return continuar;
    }

    /** Retira el efecto solo si sigue siendo la misma instancia (no pisa uno reaplicado). */
    public synchronized void limpiarEfecto(EstadoEfecto efecto) {
        efectos.remove(efecto.getNombre(), efecto);
        hilosEfectos.remove(efecto.getNombre(), Thread.currentThread());
        normalizarEstado();
    }

    private String aplicarTick(EstadoEfecto efecto) {
        if (efecto.esDanio()) {
            recibirDanio(efecto.getValorPorTick());
            return String.format("%s sufre %d de dano por %s", nombre, efecto.getValorPorTick(), efecto.getNombre());
        }
        int curado = curar(efecto.getValorPorTick());
        return String.format("%s recupera %d de vida por %s", nombre, curado, efecto.getNombre());
    }

    private synchronized boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private synchronized void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public synchronized String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public void desequiparArma() {
        this.arma = null;
    }

    public void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
        }
    }

    public void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public Arma getArma() {
        return arma;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public Set<String> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public synchronized Collection<EstadoEfecto> getEfectos() {
        return List.copyOf(efectos.values());
    }
}
