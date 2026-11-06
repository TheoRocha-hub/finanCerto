package br.com.financerto.controller;

import br.com.financerto.dto.Dtos.MovimentacaoRequest;
import br.com.financerto.dto.Dtos.MovimentacaoResponse;
import br.com.financerto.service.MovimentacaoService;
import br.com.financerto.util.Datas;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/movimentacoes")
@RequiredArgsConstructor
public class MovimentacaoController {

    private final MovimentacaoService service;

    @GetMapping
    public List<MovimentacaoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.listar(Datas.ou(mes));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse criar(@Valid @RequestBody MovimentacaoRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public MovimentacaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody MovimentacaoRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
