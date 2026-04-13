package com.example.dsentrictestservice.controller.kotlinimpl;

import com.example.dsentrictestservice.controller.BaseEntityController;
import com.example.dsentrictestservice.model.ResponseWrapper;
import com.example.dsentrictestservice.service.kotlinimpl.KotlinOrderService;
import com.example.dsentrictestservice.service.kotlinimpl.KotlinProductService;
import com.example.dsentrictestservice.service.kotlinimpl.KotlinUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class KotlinEntityController extends BaseEntityController {
    private final KotlinUserService userService;
    private final KotlinProductService productService;
    private final KotlinOrderService orderService;

    public KotlinEntityController(KotlinUserService userService, KotlinProductService productService, KotlinOrderService orderService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
    }

    @PostMapping("/users-kotlin") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createUser(@RequestBody Map<String, Object> payload) { return created(userService, payload); }
    @GetMapping("/users-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getUser(@PathVariable long id) { return read(userService, id); }
    @PutMapping("/users-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateUser(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(userService, id, payload); }
    @PatchMapping("/users-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchUser(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(userService, id, payload); }
    @DeleteMapping("/users-kotlin/{id}") public ResponseEntity<Void> deleteUser(@PathVariable long id) { return deleted(userService, id); }
    @GetMapping("/users-kotlin") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listUsers() { return listed(userService); }

    @PostMapping("/products-kotlin") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createProduct(@RequestBody Map<String, Object> payload) { return created(productService, payload); }
    @GetMapping("/products-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getProduct(@PathVariable long id) { return read(productService, id); }
    @PutMapping("/products-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateProduct(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(productService, id, payload); }
    @PatchMapping("/products-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchProduct(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(productService, id, payload); }
    @DeleteMapping("/products-kotlin/{id}") public ResponseEntity<Void> deleteProduct(@PathVariable long id) { return deleted(productService, id); }
    @GetMapping("/products-kotlin") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listProducts() { return listed(productService); }

    @PostMapping("/orders-kotlin") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createOrder(@RequestBody Map<String, Object> payload) { return created(orderService, payload); }
    @GetMapping("/orders-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getOrder(@PathVariable long id) { return read(orderService, id); }
    @PutMapping("/orders-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateOrder(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(orderService, id, payload); }
    @PatchMapping("/orders-kotlin/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchOrder(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(orderService, id, payload); }
    @DeleteMapping("/orders-kotlin/{id}") public ResponseEntity<Void> deleteOrder(@PathVariable long id) { return deleted(orderService, id); }
    @GetMapping("/orders-kotlin") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listOrders() { return listed(orderService); }
}
