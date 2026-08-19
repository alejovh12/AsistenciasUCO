package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error;

import org.springframework.http.HttpStatus;

record ApiErrorDescriptor(String code, String message, HttpStatus status) {
}
