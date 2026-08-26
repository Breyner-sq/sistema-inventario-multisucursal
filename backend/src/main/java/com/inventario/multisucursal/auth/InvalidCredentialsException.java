package com.inventario.multisucursal.auth;

import com.inventario.multisucursal.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Login fallido: email inexistente, contraseña incorrecta o cuenta inactiva.
 * Deliberadamente el mismo código y mensaje genérico para los tres casos —
 * distinguirlos revelaría a un atacante si un correo está registrado
 * (enumeración de usuarios), un riesgo de seguridad innecesario.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Credenciales inválidas.");
    }
}
