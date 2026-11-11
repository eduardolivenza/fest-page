package com.festdescans.api.adapter.in.web.order;

import com.festdescans.api.adapter.in.web.common.Validation;
import com.festdescans.api.domain.order.CustomerDetails;
import com.festdescans.api.domain.order.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
        @NotNull @Size(min = 2, max = 100) String customerName,
        @NotNull @Pattern(regexp = Validation.EMAIL) String customerEmail,
        String customerPhone,
        @NotEmpty List<@Valid @NotNull Item> items,
        @Size(max = 500) String notes) {

    public record Item(@NotNull String productId, @NotNull @Min(1) Integer quantity) {
    }

    CustomerDetails customer() {
        return new CustomerDetails(customerName, customerEmail, customerPhone);
    }

    List<OrderLine> lines() {
        return items.stream().map(i -> new OrderLine(i.productId(), i.quantity())).toList();
    }
}
