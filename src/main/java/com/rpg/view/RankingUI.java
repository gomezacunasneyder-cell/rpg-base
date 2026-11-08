package com.rpg.view;

import com.rpg.controller.RankingController;
import com.rpg.controller.Resultado;
import com.rpg.core.Consola;
import com.rpg.model.Jugador;
import com.rpg.model.PuestoRanking;
import com.rpg.model.VictoriaDetalle;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** View del ranking y de las victorias. Solo muestra lo que le entrega el RankingController. */
public class RankingUI {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Consola consola;
    private final RankingController controlador;

    public RankingUI(Consola consola, Jugador usuario) {
        this.consola = consola;
        this.controlador = new RankingController(usuario);
    }

    public void mostrar() {
        int opcion;
        do {
            System.out.println("\n=== RANKING Y VICTORIAS ===");
            System.out.println("1. Ver ranking de jugadores");
            System.out.println("2. Ver mis victorias");
            System.out.println("3. Ver victorias de otro jugador");
            System.out.println("0. Volver");
            opcion = consola.leerEntero("Opcion: ", 0, 3);
            switch (opcion) {
                case 1 -> mostrarRanking();
                case 2 -> mostrarVictorias(controlador.misVictorias());
                case 3 -> mostrarVictorias(controlador.victoriasDe(consola.leerLinea("Nombre del jugador: ")));
                default -> System.out.println("Volviendo al menu principal.");
            }
        } while (opcion != 0);
    }

    private void mostrarRanking() {
        Resultado<List<PuestoRanking>> resultado = controlador.obtenerRanking();
        if (resultado.esError()) {
            System.out.println(resultado.getMensaje());
            return;
        }
        if (resultado.getDato().isEmpty()) {
            System.out.println("Todavia no hay victorias registradas.");
            return;
        }
        System.out.printf("%-4s %-30s %10s%n", "PUESTO", "JUGADOR", "VICTORIAS");
        int puesto = 1;
        for (PuestoRanking fila : resultado.getDato()) {
            System.out.printf("%-6d %-30s %10d%n", puesto++, fila.getUsuario(), fila.getTotalVictorias());
        }
    }

    private void mostrarVictorias(Resultado<List<VictoriaDetalle>> resultado) {
        if (resultado.esError()) {
            System.out.println(resultado.getMensaje());
            return;
        }
        if (resultado.getDato().isEmpty()) {
            System.out.println("No hay victorias registradas para ese jugador.");
            return;
        }
        System.out.printf("%-30s %s%n", "MONSTRUO", "FECHA");
        for (VictoriaDetalle victoria : resultado.getDato()) {
            System.out.printf("%-30s %s%n", victoria.getMonstruo(), victoria.getFecha().format(FORMATO_FECHA));
        }
    }
}
