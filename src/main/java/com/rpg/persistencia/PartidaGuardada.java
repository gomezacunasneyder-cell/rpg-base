package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Foto del estado de la partida: es la raiz del grafo que se escribe con un solo writeObject. */
public class PartidaGuardada implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Gremio gremio;
    private final PerfilJugador perfil;
    private final HashMap<String, Integer> derrotados;
    private final LocalDateTime fecha;

    public PartidaGuardada(Gremio gremio, PerfilJugador perfil, Map<String, Integer> derrotados) {
        this.gremio = gremio;
        this.perfil = perfil;
        this.derrotados = new HashMap<>(derrotados);
        this.fecha = LocalDateTime.now();
    }

    public Gremio getGremio() {
        return gremio;
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Map<String, Integer> getDerrotados() {
        return derrotados;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
