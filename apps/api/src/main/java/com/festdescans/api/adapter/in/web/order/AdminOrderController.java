package com.festdescans.api.adapter.in.web.order;

import com.festdescans.api.adapter.in.web.common.PageResponse;
import com.festdescans.api.adapter.in.web.error.ErrorResponse;
import com.festdescans.api.adapter.in.web.order.OrderResponses.AdminOrderResponse;
import com.festdescans.api.adapter.in.web.order.OrderResponses.OrderResponse;
import com.festdescans.api.application.port.in.ManageOrdersUseCase;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
class AdminOrderController {

    static final int MAX_PAGE_SIZE = 100;

    private final ManageOrdersUseCase orders;

    AdminOrderController(ManageOrdersUseCase orders) {
        this.orders = orders;
    }

    @GetMapping
    PageResponse<AdminOrderResponse> list(@RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.of(orders.listOrders(status, PageRequest.of(page, pageSize, MAX_PAGE_SIZE)),
                AdminOrderResponse::from);
    }

    record StatusChange(String status) {
    }

    @PatchMapping("/{id}/status")
    ResponseEntity<?> changeStatus(@PathVariable String id, @RequestBody StatusChange body) {
        Optional<OrderStatus> status = Arrays.stream(OrderStatus.values())
                .filter(s -> s.name().equals(body.status()))
                .findFirst();
        if (status.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid status"));
        }
        return ResponseEntity.ok(OrderResponse.from(orders.changeOrderStatus(id, status.get())));
    }
}
