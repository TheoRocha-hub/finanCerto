package br.com.financerto.controller;

import br.com.financerto.dto.Dtos.RegistroRequest;
import br.com.financerto.model.Usuario;
import br.com.financerto.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody RegistroRequest req) {
        usuarioService.registrar(req);
    }

    /** Quem está logado (a tela usa para mostrar o nome). */
    @GetMapping("/eu")
    public Map<String, String> eu() {
        Usuario u = usuarioService.atual();
        return Map.of("nome", u.getNome(), "email", u.getEmail());
    }
}
