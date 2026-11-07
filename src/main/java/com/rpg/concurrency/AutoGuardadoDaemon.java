package com.rpg.concurrency;

import java.util.function.Supplier;

public class AutoGuardadoDaemon implements Runnable {

    private final long intervaloMs;
    private final Supplier<String> guardado;

    public AutoGuardadoDaemon(long intervaloMs, Supplier<String> guardado) {
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
            guardado.get();
        }
    }
}
