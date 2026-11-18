package com.tiendaonline.clientes.service;

import org.springframework.stereotype.Service;

@Service 
public class PiiMaskingService {

    public String enmascararNombre(String nombre){
        // Separa el nombre completo
        String[] palabras = nombre.trim().split("\\s+");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {            
            // Letra + asteriscos
            resultado.append(palabra.charAt(0)).append("*** ");
        }

        return resultado.toString().trim();
    }

    public String enmascararEmail(String email){     
        
        // Arma el resultado
        StringBuilder resultado = new StringBuilder();
        resultado.append("****@****");

        // Busca el último punto para conservar la extensión
        int posUltimoPunto = email.lastIndexOf(".");
        int posArroba = email.lastIndexOf("@");
        if(posUltimoPunto > posArroba){
            String extension = email.substring(posUltimoPunto);
            resultado.append(extension);
        }

        return resultado.toString();
    }
}
