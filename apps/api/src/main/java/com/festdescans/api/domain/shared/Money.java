package com.festdescans.api.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Prices are stored as DECIMAL(10,2): two decimals, at most 8 integer digits. */
public final class Money {

    private static final BigDecimal MAX = new BigDecimal("99999999.99");

    private Money() {
    }

    public static BigDecimal requirePositivePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new InvalidDataException("price must be positive");
        }
        BigDecimal scaled = price.setScale(2, RoundingMode.HALF_UP);
        if (scaled.compareTo(MAX) > 0) {
            throw new InvalidDataException("price is too large");
        }
        return scaled;
    }
}
