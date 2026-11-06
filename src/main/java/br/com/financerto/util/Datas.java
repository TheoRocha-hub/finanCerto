package br.com.financerto.util;

import java.time.YearMonth;
import java.time.ZoneId;

public final class Datas {
    public static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private Datas() {}

    public static YearMonth ou(YearMonth mes) {
        return mes != null ? mes : YearMonth.now(BRASIL);
    }
}
