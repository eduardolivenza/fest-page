package com.festdescans.api.domain.product;

import com.festdescans.api.domain.shared.InvalidDataException;
import com.festdescans.api.domain.shared.Money;
import java.math.BigDecimal;

public record NewProductSize(int width, int length, BigDecimal price) {

    public NewProductSize {
        if (width <= 0 || length <= 0) {
            throw new InvalidDataException("size dimensions must be positive");
        }
        price = Money.requirePositivePrice(price);
    }
}
