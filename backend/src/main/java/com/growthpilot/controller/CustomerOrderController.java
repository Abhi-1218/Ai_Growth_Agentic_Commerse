package com.growthpilot.controller;

import com.growthpilot.dto.CustomerOrderDto;
import com.growthpilot.service.CustomerOrderService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {
    private final CustomerOrderService service;
    public CustomerOrderController(CustomerOrderService service) { this.service = service; }
    @GetMapping public List<CustomerOrderDto> list() { return service.list(); }
    @GetMapping("/{id}") public CustomerOrderDto get(@PathVariable Long id) { return service.get(id); }
}
