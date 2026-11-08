package com.rpg.controller;

/**
 * Respuesta de un Controller hacia la View: dice si la operacion salio bien, trae un mensaje
 * para mostrar y, cuando corresponde, el dato pedido (un jugador, una lista de monstruos...).
 *
 * Gracias a esto la View no atrapa excepciones ni conoce la base de datos: el Controller ya
 * convirtio cualquier DaoException en un mensaje claro.
 */
public final class Resultado<T> {

    private final boolean exito;
    private final String mensaje;
    private final T dato;

    private Resultado(boolean exito, String mensaje, T dato) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.dato = dato;
    }

    public static <T> Resultado<T> exito(T dato) {
        return new Resultado<>(true, "", dato);
    }

    public static <T> Resultado<T> exito(T dato, String mensaje) {
        return new Resultado<>(true, mensaje, dato);
    }

    /** Operacion correcta que no devuelve ningun dato (crear, editar, eliminar...). */
    public static Resultado<Void> exitoSinDato(String mensaje) {
        return new Resultado<>(true, mensaje, null);
    }

    public static <T> Resultado<T> error(String mensaje) {
        return new Resultado<>(false, mensaje, null);
    }

    public boolean esExito() {
        return exito;
    }

    public boolean esError() {
        return !exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public T getDato() {
        return dato;
    }
}
