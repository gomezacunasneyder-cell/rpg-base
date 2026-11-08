package com.rpg.concurrency;

import com.rpg.entities.Enemigo;
import com.rpg.entities.Personaje;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Hilo de usuario vs hilo daemon: la JVM no termina mientras exista algun hilo de usuario vivo, pero no espera
 * a los hilos daemon (son de servicio, como el auto-guardado). setDaemon(true) solo se puede llamar antes de start().
 */
public class DemoConcurrencia {

    private static final int VIDA_INICIAL = 20_000_000;
    private static final int GOLPES_POR_HILO = 5_000_000;
    private static final int REPETICIONES = 3;

    public static void main(String[] args) throws InterruptedException {
        demostrarCondicionDeCarrera();
        demostrarLimiteDeAtomicInteger();
        demostrarReentrantLock();
        demostrarHiloDaemon();
    }

    private static void demostrarCondicionDeCarrera() throws InterruptedException {
        System.out.println("=== 1. Condicion de carrera: dos DoT sobre el mismo Personaje ===");
        int esperada = VIDA_INICIAL - 2 * GOLPES_POR_HILO;
        for (int i = 1; i <= REPETICIONES; i++) {
            Personaje objetivo = new Enemigo("Muneco", VIDA_INICIAL, 1, 0);
            Runnable dot = () -> {
                for (int golpe = 0; golpe < GOLPES_POR_HILO; golpe++) {
                    objetivo.recibirDanio(1);
                }
            };
            Thread veneno = new Thread(dot, "Veneno");
            Thread quemadura = new Thread(dot, "Quemadura");
            veneno.start();
            quemadura.start();
            veneno.join();
            quemadura.join();
            System.out.printf("Intento %d: vida esperada=%d, vida obtenida=%d, golpes perdidos=%d%n",
                    i, esperada, objetivo.getVida(), objetivo.getVida() - esperada);
        }
    }

    private static void demostrarLimiteDeAtomicInteger() {
        System.out.println("\n=== 2. Por que AtomicInteger.addAndGet no basta ===");
        AtomicInteger vida = new AtomicInteger(10);
        int sinAcotar = vida.addAndGet(-50);
        System.out.println("addAndGet(-50) sobre vida 10 -> " + sinAcotar + " (setVida la dejaria en 0 y marcaria MUERTO)");
        vida.set(10);
        int acotada = vida.updateAndGet(actual -> Math.max(0, actual - 50));
        System.out.println("updateAndGet con Math.max(0, ...) -> " + acotada
                + " (la vida si queda acotada, pero cambiar el estado a MUERTO sigue siendo otra operacion separada)");
    }

    private static void demostrarReentrantLock() throws InterruptedException {
        System.out.println("\n=== 3. Mismo experimento con ReentrantLock (lock/unlock en try-finally) ===");
        VidaConLock vida = new VidaConLock(VIDA_INICIAL);
        Runnable dot = () -> {
            for (int golpe = 0; golpe < GOLPES_POR_HILO; golpe++) {
                vida.recibirDanio(1);
            }
        };
        Thread veneno = new Thread(dot, "Veneno-Lock");
        Thread quemadura = new Thread(dot, "Quemadura-Lock");
        veneno.start();
        quemadura.start();
        veneno.join();
        quemadura.join();
        System.out.printf("Vida esperada=%d, vida obtenida=%d%n", VIDA_INICIAL - 2 * GOLPES_POR_HILO, vida.getVida());
    }

    private static void demostrarHiloDaemon() throws InterruptedException {
        System.out.println("\n=== 4. Hilo daemon ===");
        Thread demonio = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Demonio-Infinito");
        demonio.setDaemon(true);
        demonio.start();
        System.out.println("Hilo daemon vivo: " + demonio.isAlive() + " (isDaemon=" + demonio.isDaemon() + ")");

        Thread tarde = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Hilo-Tarde");
        tarde.start();
        try {
            tarde.setDaemon(true);
        } catch (IllegalThreadStateException e) {
            System.out.println("setDaemon(true) despues de start() -> IllegalThreadStateException");
        }
        tarde.join();
        System.out.println("Fin de main: el hilo daemon sigue vivo (" + demonio.isAlive()
                + ") pero la JVM cierra igual. Si este programa termina, queda demostrado.");
    }

    /*
     * Version con ReentrantLock de Personaje.recibirDanio: lock() antes de tocar la vida y unlock() siempre en el
     * finally, porque si algo lanza una excepcion antes de liberar, el lock quedaria tomado para siempre y los demas
     * hilos esperarian sin fin. Lo preferiria a synchronized cuando necesite tryLock() con tiempo de espera,
     * lockInterruptibly(), un candado justo (fair) o varias Condition; para el resto, synchronized es mas simple
     * y libera el candado solo.
     */
    private static class VidaConLock {

        private final ReentrantLock candado = new ReentrantLock();
        private final int vidaMaxima;
        private int vida;

        VidaConLock(int vidaMaxima) {
            this.vidaMaxima = vidaMaxima;
            this.vida = vidaMaxima;
        }

        void recibirDanio(int cantidad) {
            candado.lock();
            try {
                vida = Math.max(0, Math.min(vida - cantidad, vidaMaxima));
            } finally {
                candado.unlock();
            }
        }

        int getVida() {
            candado.lock();
            try {
                return vida;
            } finally {
                candado.unlock();
            }
        }
    }
}
