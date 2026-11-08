package com.rpg.controller;

import com.rpg.dao.DaoException;
import com.rpg.dao.JugadorDAO;
import com.rpg.model.Jugador;
import com.rpg.util.ClaveHash;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Controller del login. Aqui viven las reglas de negocio de la sesion:
 *   - maximo 3 intentos fallidos; al tercero la sesion queda bloqueada;
 *   - la clave se convierte a hash SHA-256 antes de llegar al DAO (la clave en texto plano
 *     nunca sale de este metodo);
 *   - quien se registra siempre nace con rol jugador (nadie se crea una cuenta admin solo);
 *   - formato minimo de usuario y clave al crear una cuenta.
 *
 * No imprime ni lee del teclado (eso es de la View) y no escribe SQL (eso es del DAO).
 */
public class LoginController {

    public static final int MAX_INTENTOS = 3;

    private static final int USUARIO_MIN = 3;
    private static final int USUARIO_MAX = 30;
    private static final int CLAVE_MIN = 6;
    private static final int CLAVE_MAX = 64;
    private static final Pattern PATRON_USUARIO = Pattern.compile("[A-Za-z0-9_]+");

    private final JugadorDAO jugadorDAO;
    private int intentosFallidos;
    private Jugador jugadorActual;

    public LoginController() {
        this(new JugadorDAO());
    }

    public LoginController(JugadorDAO jugadorDAO) {
        this.jugadorDAO = jugadorDAO;
    }

    /**
     * Intenta iniciar sesion. Solo un usuario o clave incorrectos cuentan como intento fallido:
     * dejar campos vacios o un error de base de datos no gastan intentos.
     */
    public Resultado<Jugador> iniciarSesion(String usuario, String clave) {
        if (estaBloqueado()) {
            return Resultado.error("La sesion esta bloqueada por demasiados intentos fallidos.");
        }
        if (usuario == null || usuario.isBlank() || clave == null || clave.isEmpty()) {
            return Resultado.error("Escribe el usuario y la clave.");
        }
        try {
            Optional<Jugador> jugador = jugadorDAO.autenticar(usuario, ClaveHash.sha256(clave));
            if (jugador.isPresent()) {
                intentosFallidos = 0;
                jugadorActual = jugador.get();
                return Resultado.exito(jugadorActual, "Bienvenido, " + jugadorActual.getUsuario()
                        + " (rol: " + jugadorActual.getRol() + ").");
            }
            intentosFallidos++;
            if (estaBloqueado()) {
                return Resultado.error("Usuario o clave incorrectos. Sesion bloqueada: llegaste a "
                        + MAX_INTENTOS + " intentos fallidos.");
            }
            return Resultado.error("Usuario o clave incorrectos. Intentos restantes: " + intentosRestantes() + ".");
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    /** Crea una cuenta nueva con rol jugador. No inicia sesion: despues hay que hacer login. */
    public Resultado<Void> registrar(String usuario, String clave) {
        if (estaBloqueado()) {
            return Resultado.error("La sesion esta bloqueada por demasiados intentos fallidos.");
        }
        String problema = validarCredenciales(usuario, clave);
        if (problema != null) {
            return Resultado.error(problema);
        }
        try {
            Jugador nuevo = new Jugador(0, usuario, Jugador.ROL_JUGADOR);
            jugadorDAO.registrar(nuevo, ClaveHash.sha256(clave));
            return Resultado.exitoSinDato("Cuenta creada. Ya puedes iniciar sesion.");
        } catch (DaoException e) {
            return Resultado.error(e.getMessage());
        }
    }

    public boolean estaBloqueado() {
        return intentosFallidos >= MAX_INTENTOS;
    }

    public int intentosRestantes() {
        return Math.max(0, MAX_INTENTOS - intentosFallidos);
    }

    public Jugador getJugadorActual() {
        return jugadorActual;
    }

    public void cerrarSesion() {
        jugadorActual = null;
    }

    /** @return el problema encontrado, o null si el usuario y la clave tienen un formato valido */
    private static String validarCredenciales(String usuario, String clave) {
        if (usuario == null || usuario.length() < USUARIO_MIN || usuario.length() > USUARIO_MAX) {
            return "El usuario debe tener entre " + USUARIO_MIN + " y " + USUARIO_MAX + " caracteres.";
        }
        if (!PATRON_USUARIO.matcher(usuario).matches()) {
            return "El usuario solo puede tener letras sin tilde, numeros y guion bajo.";
        }
        if (clave == null || clave.length() < CLAVE_MIN || clave.length() > CLAVE_MAX) {
            return "La clave debe tener entre " + CLAVE_MIN + " y " + CLAVE_MAX + " caracteres.";
        }
        return null;
    }
}
