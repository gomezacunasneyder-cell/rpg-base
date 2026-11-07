package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

public class GestorTurno {

    private Thread hilo;

    public void iniciar(int segundos, TurnoCallback callback) {
        hilo = new Thread(new TemporizadorTurnoTask(segundos, callback), "Temporizador-Turno");
        hilo.start();
    }

    /**
     * Cancela el temporizador con interrupt().
     * @return true si el hilo seguia vivo (el jugador respondio a tiempo);
     *         false si ya habia terminado (se agoto el tiempo)
     */
    public boolean cancelar() {
        boolean seguiaVivo = hilo != null && hilo.isAlive();
        if (hilo != null) {
            hilo.interrupt();
        }
        return seguiaVivo;
    }
}