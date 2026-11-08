package com.rpg.model;

import java.time.LocalDateTime;

/** Una victoria de un jugador: el monstruo vencido y la fecha. */
public class VictoriaDetalle {

    private final String monstruo;
    private final LocalDateTime fecha;

    public VictoriaDetalle(String monstruo, LocalDateTime fecha) {
        this.monstruo = monstruo;
        this.fecha = fecha;
    }

    public String getMonstruo() {
        return monstruo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return "VictoriaDetalle{monstruo='" + monstruo + "', fecha=" + fecha + "}";
    }
}
