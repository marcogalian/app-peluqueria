package com.marcog.peluqueria.chatbot.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Puerto de salida que ejecuta las funciones que el modelo solicita.
 *
 * La implementacion concreta accede a BD para resolver datos en tiempo real
 * (citas, ganancias, stock, etc.) y devuelve un JSON serializado al modelo.
 */
public interface ConsultasGestionPeluqueria {

    /**
     * @param functionName  nombre que el modelo declaro en functionCall
     * @param args          argumentos de la funcion (puede ser null)
     * @param peluqueroId   id del peluquero autenticado (null si es admin sin peluquero)
     * @param isAdmin       si el usuario tiene rol ROLE_ADMIN
     * @return JSON serializado para devolver al modelo
     */
    String execute(String functionName, JsonNode args, UUID peluqueroId, boolean isAdmin);
}
