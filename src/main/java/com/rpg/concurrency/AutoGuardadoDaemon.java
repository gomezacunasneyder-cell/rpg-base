package com.rpg.concurrency;

public class AutoGuardadoDaemon implements Runnable {

    private final long intervaloMs;
    private final Runnable guardado;

    public AutoGuardadoDaemon(long intervaloMs, Runnable guardado) {
        this.intervaloMs = intervaloMs;
        this.guardado = guardado;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            guardado.run();
        }
    }
}
