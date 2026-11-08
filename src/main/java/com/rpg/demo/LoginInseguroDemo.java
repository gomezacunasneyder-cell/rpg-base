package com.rpg.demo;

import com.rpg.dao.ConexionDB;
import com.rpg.dao.DaoException;
import com.rpg.dao.JugadorDAO;
import com.rpg.model.Jugador;
import com.rpg.util.ClaveHash;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Scanner;

/**
 * *** SOLO CON FINES DIDACTICOS - NUNCA USAR ESTE CODIGO EN LA APLICACION REAL ***
 *
 * Muestra la amenaza (inyeccion SQL) y el escudo (PreparedStatement) lado a lado:
 *   - entrarInseguro(): arma el SQL CONCATENANDO el texto del usuario y lo ejecuta con Statement.
 *   - JugadorDAO.autenticar(): hace el mismo login con PreparedStatement y comodines ?.
 *
 * Esta es la unica clase del proyecto que usa Statement (sin Prepared) y, junto con el paquete
 * dao, la unica que importa java.sql: por eso vive aislada en el paquete demo.
 *
 * Ejecutar el main (Run). Prueba, entre otros casos, la entrada  admin' OR 1=1 --  (con un
 * espacio al final: MySQL exige un espacio despues de "--" para tratarlo como comentario).
 */
public class LoginInseguroDemo {

    private static int numeroCaso = 0;

    /** LOGIN VULNERABLE: el usuario se pega directamente dentro del texto SQL. */
    static Optional<Jugador> entrarInseguro(String usuario, String clave) {
        String sql = "SELECT id, usuario, rol FROM jugadores WHERE usuario = '" + usuario
                + "' AND clave = '" + ClaveHash.sha256(clave) + "'";
        System.out.println("  SQL armado: " + sql);
        try (Connection conexion = ConexionDB.obtener();
             Statement consulta = conexion.createStatement();
             ResultSet resultado = consulta.executeQuery(sql)) {
            int filas = 0;
            Jugador primero = null;
            while (resultado.next()) {
                filas++;
                if (primero == null) {
                    primero = new Jugador(resultado.getInt("id"),
                            resultado.getString("usuario"), resultado.getString("rol"));
                }
            }
            System.out.println("  Filas devueltas por la consulta: " + filas);
            return Optional.ofNullable(primero);
        } catch (DaoException e) {
            System.out.println("  No se pudo conectar: " + e.getMessage());
            return Optional.empty();
        } catch (SQLException e) {
            System.out.println("  La base de datos rechazo el SQL armado (error visible para el atacante): "
                    + e.getMessage());
            return Optional.empty();
        }
    }

    /** LOGIN SEGURO: el mismo login, pero con JugadorDAO.autenticar (PreparedStatement). */
    static Optional<Jugador> entrarSeguro(String usuario, String clave) {
        try {
            return new JugadorDAO().autenticar(usuario, ClaveHash.sha256(clave));
        } catch (DaoException e) {
            System.out.println("  No se pudo validar: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static void probarCaso(String titulo, String usuario, String clave, boolean esAtaque) {
        numeroCaso++;
        System.out.println();
        System.out.println("--- Caso " + numeroCaso + ": " + titulo + " ---");
        System.out.println("Entrada -> usuario=\"" + usuario + "\"  clave=\"" + clave + "\"");

        System.out.println("[INSEGURO] Statement + concatenacion");
        Optional<Jugador> inseguro = entrarInseguro(usuario, clave);
        if (inseguro.isPresent()) {
            System.out.println("  RESULTADO: ENTRO como '" + inseguro.get().getUsuario()
                    + "' (rol " + inseguro.get().getRol() + ")"
                    + (esAtaque ? "  <<< VULNERABLE: entro SIN conocer la clave" : ""));
        } else {
            System.out.println("  RESULTADO: login rechazado");
        }

        System.out.println("[SEGURO] PreparedStatement + comodines ?");
        Optional<Jugador> seguro = entrarSeguro(usuario, clave);
        if (seguro.isPresent()) {
            System.out.println("  RESULTADO: ENTRO como '" + seguro.get().getUsuario()
                    + "' (rol " + seguro.get().getRol() + ")");
        } else {
            System.out.println("  RESULTADO: login RECHAZADO (usuario o clave incorrectos)"
                    + (esAtaque ? "  <<< DEFENDIDO: la entrada se trato como un dato, no como codigo" : ""));
        }
    }

    private static void modoLibre() {
        System.out.println();
        System.out.println("--- Prueba tu propia entrada (deja el usuario vacio para terminar) ---");
        Scanner teclado = new Scanner(System.in);
        while (true) {
            System.out.print("Usuario: ");
            if (!teclado.hasNextLine()) {
                return;
            }
            String usuario = teclado.nextLine();   // sin trim(): el espacio final de "-- " importa
            if (usuario.isEmpty()) {
                return;
            }
            System.out.print("Clave: ");
            if (!teclado.hasNextLine()) {
                return;
            }
            probarCaso("Entrada libre", usuario, teclado.nextLine(), false);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== DEMO DE INYECCION SQL (solo con fines didacticos) ===");
        System.out.println("Cuentas de prueba de schema.sql: admin / admin123  y  jugador1 / jugador123");

        probarCaso("Login legitimo", "admin", "admin123", false);
        probarCaso("Clave incorrecta", "admin", "equivocada", false);
        probarCaso("ATAQUE: OR 1=1 y comentario", "admin' OR 1=1 -- ", "no_la_se", true);
        probarCaso("ATAQUE: entrar como otro jugador sin su clave", "jugador1' -- ", "no_la_se", true);
        probarCaso("Comilla suelta (provoca un error de SQL)", "'", "x", true);

        modoLibre();
    }
}
