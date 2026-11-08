package com.rpg.view;

import com.rpg.controller.LoginController;
import com.rpg.controller.Resultado;
import com.rpg.core.Consola;
import com.rpg.model.Jugador;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * View del login: es la puerta de entrada de la aplicacion (Main solo la arranca).
 * Solo pide datos con el teclado, los pasa al LoginController y muestra lo que este responde.
 * No conoce la base de datos, no decide quien entra ni cuantos intentos quedan.
 */
public class LoginUI {

    private final LoginController controlador = new LoginController();

    public void iniciar() {
        try (Scanner scanner = new Scanner(System.in)) {
            ejecutar(new Consola(scanner));
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private void ejecutar(Consola consola) {
        boolean terminar = false;
        while (!terminar) {
            System.out.println("\n=== ARENA SEGURA ===");
            System.out.println("1. Iniciar sesion");
            System.out.println("2. Crear cuenta");
            System.out.println("0. Salir");
            int opcion = consola.leerEntero("Opcion: ", 0, 2);
            switch (opcion) {
                case 1 -> terminar = iniciarSesion(consola);
                case 2 -> crearCuenta(consola);
                default -> {
                    System.out.println("Hasta luego.");
                    terminar = true;
                }
            }
        }
    }

    /** @return true si la sesion quedo bloqueada y el programa debe cerrarse */
    private boolean iniciarSesion(Consola consola) {
        String usuario = consola.leerLinea("Usuario: ");
        String clave = consola.leerLinea("Clave: ");
        Resultado<Jugador> resultado = controlador.iniciarSesion(usuario, clave);
        System.out.println(resultado.getMensaje());
        if (resultado.esExito()) {
            new MenuPrincipalUI(consola, resultado.getDato()).mostrar();
            controlador.cerrarSesion();
            return false;
        }
        if (controlador.estaBloqueado()) {
            System.out.println("El programa se cerrara. Vuelve a abrirlo para intentarlo de nuevo.");
            return true;
        }
        return false;
    }

    private void crearCuenta(Consola consola) {
        String usuario = consola.leerLinea("Nuevo usuario (3 a 30 letras, numeros o _): ");
        String clave = consola.leerLinea("Clave (minimo 6 caracteres): ");
        Resultado<Void> resultado = controlador.registrar(usuario, clave);
        System.out.println(resultado.getMensaje());
    }
}
