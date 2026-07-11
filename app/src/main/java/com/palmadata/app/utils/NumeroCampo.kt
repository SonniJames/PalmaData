package com.palmadata.app.utils

/**
 * Conversión de texto de campo a decimal aceptando coma O punto.
 *
 * En celulares configurados en español, el teclado numérico de
 * android:inputType="numberDecimal" muestra COMA como separador decimal.
 * "12,5".toDoubleOrNull() devuelve null → los registros quedaban en 0.0
 * sin ningún aviso (combustible, horómetros, cantidad, defoliación, polen).
 *
 * Este helper normaliza la coma a punto antes de convertir, de modo que
 * "12,5" y "12.5" producen ambos 12.5.
 */
fun String.aDecimalCampo(): Double? =
    trim().replace(",", ".").toDoubleOrNull()
