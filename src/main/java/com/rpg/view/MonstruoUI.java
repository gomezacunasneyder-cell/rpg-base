package com.rpg.view;

import com.rpg.controller.MonstruoController;
import com.rpg.controller.Resultado;
import com.rpg.core.Consola;
import com.rpg.model.Jugador;
import com.rpg.model.Monstruo;

import java.util.List;

/**
 * View del bestiario. Muestra el menu, pide datos y presenta resultados.
 * Todas las opciones se muestran a todos: quien decide si alguien puede crear, editar o
 * eliminar es el MonstruoController (regla de negocio), no esta clase.
 */
public class MonstruoUI {

    private static final int MAXIMO_ESTADISTICA = 100000;

    private final Consola consola;
    private final MonstruoController controlador;

    public MonstruoUI(Consola consola, Jugador usuario) {
        this.consola = consola;
        this.controlador = new MonstruoController(usuario);
    }

    public void mostrar() {
        int opcion;
        do {
            System.out.println("\n=== BESTIARIO (base de datos) ===");
            System.out.println("1. Ver todos los monstruos");
            System.out.println("2. Buscar por nombre");
            System.out.println("3. Filtrar por vida minima");
            System.out.println("4. Crear monstruo (solo admin)");
            System.out.println("5. Editar monstruo (solo admin)");
            System.out.println("6. Eliminar monstruo (solo admin)");
            System.out.println("0. Volver");
            opcion = consola.leerEntero("Opcion: ", 0, 6);
            switch (opcion) {
                case 1 -> mostrarLista(controlador.listar());
                case 2 -> buscarPorNombre();
                case 3 -> filtrarPorVida();
                case 4 -> crear();
                case 5 -> editar();
                case 6 -> eliminar();
                default -> System.out.println("Volviendo al menu principal.");
            }
        } while (opcion != 0);
    }

    private void buscarPorNombre() {
        String nombre = consola.leerLinea("Nombre del monstruo: ");
        Resultado<Monstruo> resultado = controlador.buscarPorNombre(nombre);
        if (resultado.esError()) {
            System.out.println(resultado.getMensaje());
            return;
        }
        mostrarLista(Resultado.exito(List.of(resultado.getDato())));
    }

    private void filtrarPorVida() {
        int vidaMinima = consola.leerEntero("Vida minima: ", 0, MAXIMO_ESTADISTICA);
        mostrarLista(controlador.listarPorVidaMinima(vidaMinima));
    }

    private void crear() {
        if (!tienePermiso()) {
            return;
        }
        String nombre = consola.leerLinea("Nombre: ");
        int vida = consola.leerEntero("Vida: ", 1, MAXIMO_ESTADISTICA);
        int ataque = consola.leerEntero("Ataque: ", 1, MAXIMO_ESTADISTICA);
        System.out.println(controlador.crear(nombre, vida, ataque).getMensaje());
    }

    private void editar() {
        if (!tienePermiso()) {
            return;
        }
        int id = consola.leerEntero("Id del monstruo a editar: ", 1, Integer.MAX_VALUE);
        Resultado<Monstruo> actual = controlador.buscarPorId(id);
        if (actual.esError()) {
            System.out.println(actual.getMensaje());
            return;
        }
        System.out.println("Actual: " + describir(actual.getDato()));
        String nombre = consola.leerLinea("Nuevo nombre: ");
        int vida = consola.leerEntero("Nueva vida: ", 1, MAXIMO_ESTADISTICA);
        int ataque = consola.leerEntero("Nuevo ataque: ", 1, MAXIMO_ESTADISTICA);
        System.out.println(controlador.editar(id, nombre, vida, ataque).getMensaje());
    }

    private void eliminar() {
        if (!tienePermiso()) {
            return;
        }
        int id = consola.leerEntero("Id del monstruo a eliminar: ", 1, Integer.MAX_VALUE);
        Resultado<Monstruo> actual = controlador.buscarPorId(id);
        if (actual.esError()) {
            System.out.println(actual.getMensaje());
            return;
        }
        String respuesta = consola.leerLinea("Seguro que quieres eliminar a "
                + actual.getDato().getNombre() + "? (s/n): ");
        if (!respuesta.equalsIgnoreCase("s")) {
            System.out.println("Eliminacion cancelada.");
            return;
        }
        System.out.println(controlador.eliminar(id).getMensaje());
    }

    /** Pregunta al Controller si hay permiso; la View solo muestra la respuesta. */
    private boolean tienePermiso() {
        Resultado<Void> permiso = controlador.verificarPermisoAdmin();
        if (permiso.esError()) {
            System.out.println(permiso.getMensaje());
            return false;
        }
        return true;
    }

    private void mostrarLista(Resultado<List<Monstruo>> resultado) {
        if (resultado.esError()) {
            System.out.println(resultado.getMensaje());
            return;
        }
        if (resultado.getDato().isEmpty()) {
            System.out.println("No hay monstruos que mostrar.");
            return;
        }
        System.out.printf("%-4s %-30s %8s %8s%n", "ID", "NOMBRE", "VIDA", "ATAQUE");
        for (Monstruo monstruo : resultado.getDato()) {
            System.out.printf("%-4d %-30s %8d %8d%n", monstruo.getId(), monstruo.getNombre(),
                    monstruo.getVida(), monstruo.getAtaque());
        }
    }

    private static String describir(Monstruo monstruo) {
        return monstruo.getNombre() + " (vida " + monstruo.getVida() + ", ataque " + monstruo.getAtaque() + ")";
    }
}
