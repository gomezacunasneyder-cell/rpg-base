package com.rpg.view;

import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.model.Jugador;

/** View del menu principal del juego. Se muestra solo despues de un login correcto. */
public class MenuPrincipalUI {

    private final Consola consola;
    private final Jugador cuenta;

    public MenuPrincipalUI(Consola consola, Jugador cuenta) {
        this.consola = consola;
        this.cuenta = cuenta;
    }

    public void mostrar() {
        MotorJuego motor = new MotorJuego(consola, cuenta);
        MonstruoUI monstruoUI = new MonstruoUI(consola, cuenta);
        RankingUI rankingUI = new RankingUI(consola, cuenta);
        motor.iniciarAutoGuardado();
        try {
            int opcion;
            do {
                mostrarMenu();
                opcion = consola.leerEntero("Opcion: ", 0, 9);
                switch (opcion) {
                    case 1 -> motor.combateRapido();
                    case 2 -> motor.jugarTorneo();
                    case 3 -> monstruoUI.mostrar();
                    case 4 -> motor.mostrarInfoArchivos();
                    case 5 -> motor.verHistorial();
                    case 6 -> motor.borrarPartidaGuardada();
                    case 7 -> motor.cargarPartida();
                    case 8 -> motor.verAutoguardado();
                    case 9 -> rankingUI.mostrar();
                    default -> System.out.println("Sesion cerrada.");
                }
            } while (opcion != 0);
        } finally {
            motor.detenerAutoGuardado();
        }
    }

    private void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG === Sesion: " + cuenta.getUsuario()
                + " (" + cuenta.getRol() + ")");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Bestiario (base de datos)");
        System.out.println("4. Archivos de partida");
        System.out.println("5. Ver historial de combates");
        System.out.println("6. Borrar partida guardada");
        System.out.println("7. Cargar partida");
        System.out.println("8. Ver ultimo autoguardado");
        System.out.println("9. Ranking y victorias");
        System.out.println("0. Cerrar sesion");
    }
}
