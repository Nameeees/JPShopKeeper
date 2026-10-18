package com.jp.shopkeeper.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Formato y validacion de los numeros que se escriben por chat. */
public class NumbersTest {

    @Test
    public void formatoSinDecimalesCuandoEsEntero() {
        assertEquals("250", Numbers.format(250.0));
        assertEquals("0", Numbers.format(0.0));
        assertEquals("250.50", Numbers.format(250.5));
    }

    @Test
    public void aceptaComaComoSeparadorDecimal() {
        assertEquals(Double.valueOf(500.5), Numbers.parseDoubleOrNull("500,5"));
        assertEquals(Double.valueOf(250.0), Numbers.parseDoubleOrNull("250"));
    }

    @Test
    public void rechazaValoresNoFinitos() {
        // NaN nunca es > 0: un precio asi dejaria el tradeo gratis sin cobrar.
        assertNull(Numbers.parseDoubleOrNull("NaN"));
        assertNull(Numbers.parseDoubleOrNull("Infinity"));
        assertNull(Numbers.parseDoubleOrNull("-Infinity"));
    }

    @Test
    public void rechazaTextoVacioONulo() {
        assertNull(Numbers.parseDoubleOrNull(null));
        assertNull(Numbers.parseDoubleOrNull("abc"));
        assertNull(Numbers.parseIntOrNull("1.5"));
    }

    @Test
    public void clampAcotaEnLosDosSentidos() {
        assertEquals(1, Numbers.clamp(0, 1, 64));
        assertEquals(64, Numbers.clamp(999, 1, 64));
        assertEquals(30, Numbers.clamp(30, 1, 64));
        assertEquals(0.5, Numbers.clamp(0.1, 0.5, 5.0), 0.0001);
    }
}
