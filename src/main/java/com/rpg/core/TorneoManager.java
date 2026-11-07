package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/** Torneo de 4 gladiadores: dos semifinales y una final. */
public class TorneoManager {

    private static final long PAUSA_OBSERVACION_MS = 200;

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        // TALLER CONCURRENCIA: las dos semifinales se simulan una tras otra en el hilo principal.
        // La final solo puede empezar cuando ambas hayan terminado.
        SemifinalTask tareaA = new SemifinalTask(gladiadores.get(0), gladiadores.get(1));
        SemifinalTask tareaB = new SemifinalTask(gladiadores.get(2), gladiadores.get(3));
        Thread semifinalA = new Thread(tareaA, "Semifinal-A");
        Thread semifinalB = new Thread(tareaB, "Semifinal-B");

        registrarEstados("antes de start()", semifinalA, semifinalB);
        semifinalA.start();
        semifinalB.start();
        registrarEstados("justo despues de start()", semifinalA, semifinalB);

        try {
            Thread.sleep(PAUSA_OBSERVACION_MS);
            registrarEstados("durante la ejecucion", semifinalA, semifinalB);
            semifinalA.join();
            registrarEstados("despues de join() de A", semifinalA, semifinalB);
            semifinalB.join();
            registrarEstados("despues de join() de B", semifinalA, semifinalB);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            semifinalA.interrupt();
            semifinalB.interrupt();
            throw new IllegalStateException("El torneo fue interrumpido antes de terminar las semifinales", e);
        }

        Personaje finalista1 = tareaA.getGanador();
        Personaje finalista2 = tareaB.getGanador();

        return simularFinal(finalista1, finalista2);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private void registrarEstados(String momento, Thread semifinalA, Thread semifinalB) {
        registro.agregar("[Estado %s] %s=%s | %s=%s", momento,
                semifinalA.getName(), semifinalA.getState(), semifinalB.getName(), semifinalB.getState());
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }

    private class SemifinalTask implements Runnable {

        private final Personaje a;
        private final Personaje b;
        private Personaje ganador;

        SemifinalTask(Personaje a, Personaje b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void run() {
            ganador = simularSemifinal(a, b);
        }

        Personaje getGanador() {
            return ganador;
        }
    }
}
