package com.example.dsentrictestservice.controller.javaimpl;

import com.example.dsentrictestservice.controller.BaseEntityController;
import com.example.dsentrictestservice.model.ResponseWrapper;
import com.example.dsentrictestservice.service.javaimpl.JavaOrderService;
import com.example.dsentrictestservice.service.javaimpl.JavaProductService;
import com.example.dsentrictestservice.service.javaimpl.JavaUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class JavaEntityController extends BaseEntityController {
    private final JavaUserService userService;
    private final JavaProductService productService;
    private final JavaOrderService orderService;

    public JavaEntityController(JavaUserService userService, JavaProductService productService, JavaOrderService orderService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
    }

    @PostMapping("/users-java") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createUser(@RequestBody Map<String, Object> payload) { return created(userService, payload); }
    @GetMapping("/users-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getUser(@PathVariable long id) { return read(userService, id); }
    @PutMapping("/users-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateUser(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(userService, id, payload); }
    @PatchMapping("/users-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchUser(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(userService, id, payload); }
    @DeleteMapping("/users-java/{id}") public ResponseEntity<Void> deleteUser(@PathVariable long id) { return deleted(userService, id); }
    @GetMapping("/users-java") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listUsers() { return listed(userService); }

    @PostMapping("/products-java") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createProduct(@RequestBody Map<String, Object> payload) { return created(productService, payload); }
    @GetMapping("/products-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getProduct(@PathVariable long id) { return read(productService, id); }
    @PutMapping("/products-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateProduct(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(productService, id, payload); }
    @PatchMapping("/products-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchProduct(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(productService, id, payload); }
    @DeleteMapping("/products-java/{id}") public ResponseEntity<Void> deleteProduct(@PathVariable long id) { return deleted(productService, id); }
    @GetMapping("/products-java") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listProducts() { return listed(productService); }

    @PostMapping("/orders-java") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createOrder(@RequestBody Map<String, Object> payload) { return created(orderService, payload); }
    @GetMapping("/orders-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getOrder(@PathVariable long id) { return read(orderService, id); }
    @PutMapping("/orders-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateOrder(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(orderService, id, payload); }
    @PatchMapping("/orders-java/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchOrder(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(orderService, id, payload); }
    @DeleteMapping("/orders-java/{id}") public ResponseEntity<Void> deleteOrder(@PathVariable long id) { return deleted(orderService, id); }
    @GetMapping("/orders-java") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listOrders() { return listed(orderService); }
}
