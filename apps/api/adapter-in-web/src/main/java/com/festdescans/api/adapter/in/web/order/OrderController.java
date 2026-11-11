package com.festdescans.api.adapter.in.web.order;

import com.festdescans.api.adapter.in.web.order.OrderResponses.PlacedOrderResponse;
import com.festdescans.api.application.port.in.PlaceOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final PlaceOrderUseCase orders;

    OrderController(PlaceOrderUseCase orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlacedOrderResponse create(@RequestBody @Valid CreateOrderRequest body) {
        return PlacedOrderResponse.from(orders.placeOrder(body.customer(), body.lines(), body.notes()));
    }
}
