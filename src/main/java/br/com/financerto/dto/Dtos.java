package br.com.financerto.dto;

import br.com.financerto.model.TipoMovimentacao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Dtos {
    private Dtos() {}

    public record RegistroRequest(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 6, max = 72) String senha) {}

    public record CategoriaRequest(
            @NotBlank @Size(max = 60) String nome,
            @NotNull TipoMovimentacao tipo,
            @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal limiteMensal) {}

    public record MovimentacaoRequest(
            @NotBlank @Size(max = 120) String descricao,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal valor,
            @NotNull LocalDate data,
            @NotNull Long categoriaId) {}

    public record CategoriaResponse(Long id, String nome, TipoMovimentacao tipo, BigDecimal limiteMensal) {}

    public record MovimentacaoResponse(Long id, String descricao, BigDecimal valor, LocalDate data,
                                       TipoMovimentacao tipo, Long categoriaId, String categoriaNome) {}

    public record Resumo(String mes, BigDecimal saldoGeral, BigDecimal receitas, BigDecimal despesas,
                         BigDecimal saldoDoMes, BigDecimal taxaPoupanca, BigDecimal variacaoDespesas,
                         String maiorCategoriaDespesa) {}

    public record GastoCategoria(String categoria, BigDecimal total, BigDecimal percentual) {}

    public record EvolucaoMes(String mes, BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {}

    public record Orcamento(String categoria, BigDecimal limite, BigDecimal gasto,
                            BigDecimal percentualUsado, boolean estourado) {}
}
