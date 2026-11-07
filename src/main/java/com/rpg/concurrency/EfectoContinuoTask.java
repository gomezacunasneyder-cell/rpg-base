package com.rpg.concurrency;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

public class EfectoContinuoTask implements Runnable {

    private final Personaje personaje;
    private final EstadoEfecto efecto;
    private final String nombreEfecto;
    private final long intervaloMs;

    public EfectoContinuoTask(Personaje personaje, EstadoEfecto efecto, long intervaloMs) {
        this.personaje = personaje;
        this.efecto = efecto;
        this.nombreEfecto = efecto.getNombre();
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(intervaloMs);
                if (!personaje.aplicarTickEfecto(efecto)) {
                    break;
                }
            } catch (InterruptedException e) {
                personaje.limpiarEfecto(efecto);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public String getNombreEfecto() {
        return nombreEfecto;
    }
}
