package br.com.financerto.controller;

import br.com.financerto.dto.Dtos.EvolucaoMes;
import br.com.financerto.dto.Dtos.GastoCategoria;
import br.com.financerto.dto.Dtos.Orcamento;
import br.com.financerto.dto.Dtos.Resumo;
import br.com.financerto.service.RelatorioService;
import br.com.financerto.util.Datas;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService service;

    @GetMapping("/resumo")
    public Resumo resumo(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.resumo(Datas.ou(mes));
    }

    @GetMapping("/por-categoria")
    public List<GastoCategoria> porCategoria(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.gastosPorCategoria(Datas.ou(mes));
    }

    @GetMapping("/evolucao")
    public List<EvolucaoMes> evolucao(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(defaultValue = "6") int meses) {
        return service.evolucao(Datas.ou(mes), meses);
    }

    @GetMapping("/orcamentos")
    public List<Orcamento> orcamentos(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.orcamentos(Datas.ou(mes));
    }
}
