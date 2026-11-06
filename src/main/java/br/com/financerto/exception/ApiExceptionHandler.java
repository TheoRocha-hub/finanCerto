package br.com.financerto.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", "Dados inválidos");
        corpo.put("campos", campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        String msg = ex.getReason() != null ? ex.getReason() : "Erro";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("erro", msg));
    }
}
