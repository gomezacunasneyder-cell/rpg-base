package com.rpg;

import com.rpg.view.LoginUI;

/** Punto de entrada: solo arranca la aplicacion llamando a la View. */
public class Main {

    public static void main(String[] args) {
        new LoginUI().iniciar();
    }
}