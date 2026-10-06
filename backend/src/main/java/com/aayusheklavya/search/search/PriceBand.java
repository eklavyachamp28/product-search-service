package com.aayusheklavya.search.search;

import java.math.BigDecimal;
import java.util.List;

/** Fixed price bands used for the price facet. {@code to} is exclusive; null means unbounded. */
public record PriceBand(String key, BigDecimal from, BigDecimal to) {

    public static final List<PriceBand> BANDS = List.of(
            new PriceBand("under-50", null, bd(50)),
            new PriceBand("50-100", bd(50), bd(100)),
            new PriceBand("100-250", bd(100), bd(250)),
            new PriceBand("250-500", bd(250), bd(500)),
            new PriceBand("500-1000", bd(500), bd(1000)),
            new PriceBand("1000-plus", bd(1000), null));

    public boolean matches(BigDecimal min, BigDecimal max) {
        return eq(from, min) && eq(to, max);
    }

    private static boolean eq(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    private static BigDecimal bd(int v) { return BigDecimal.valueOf(v); }
}
