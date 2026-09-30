package com.growthpilot.service;

import com.growthpilot.dto.CustomerOrderDto;
import com.growthpilot.entity.Order;
import com.growthpilot.exception.ResourceNotFoundException;
import com.growthpilot.repository.OrderRepository;
import com.growthpilot.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CustomerOrderService {
    private final OrderRepository orders;
    private final SecurityUtils security;
    public CustomerOrderService(OrderRepository orders, SecurityUtils security) { this.orders = orders; this.security = security; }
    @Transactional(readOnly = true)
    public List<CustomerOrderDto> list() {
        return orders.findAllByCustomerId(security.getCurrentCustomer().getId()).stream().sorted(
                java.util.Comparator.comparing(Order::getOrderDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(this::dto).toList();
    }
    @Transactional(readOnly = true)
    public CustomerOrderDto get(Long id) {
        return dto(orders.findByIdAndCustomerId(id, security.getCurrentCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }
    private CustomerOrderDto dto(Order o) {
        List<CustomerOrderDto.Item> items = o.getItems().stream().map(i ->
                new CustomerOrderDto.Item(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(), i.getPriceAtPurchase())).toList();
        return new CustomerOrderDto(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCurrency(),
                o.getRazorpayOrderId(), o.getOrderDate(), items);
    }
}
