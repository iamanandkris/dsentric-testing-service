package com.example.dsentrictestservice.controller.scalaimpl;

import com.example.dsentrictestservice.controller.BaseEntityController;
import com.example.dsentrictestservice.model.ResponseWrapper;
import com.example.dsentrictestservice.service.scalaimpl.ScalaOrderService;
import com.example.dsentrictestservice.service.scalaimpl.ScalaProductService;
import com.example.dsentrictestservice.service.scalaimpl.ScalaUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ScalaEntityController extends BaseEntityController {
    private final ScalaUserService userService;
    private final ScalaProductService productService;
    private final ScalaOrderService orderService;

    public ScalaEntityController(ScalaUserService userService, ScalaProductService productService, ScalaOrderService orderService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
    }

    @PostMapping("/users-scala") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createUser(@RequestBody Map<String, Object> payload) { return created(userService, payload); }
    @GetMapping("/users-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getUser(@PathVariable long id) { return read(userService, id); }
    @PutMapping("/users-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateUser(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(userService, id, payload); }
    @PatchMapping("/users-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchUser(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(userService, id, payload); }
    @DeleteMapping("/users-scala/{id}") public ResponseEntity<Void> deleteUser(@PathVariable long id) { return deleted(userService, id); }
    @GetMapping("/users-scala") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listUsers() { return listed(userService); }

    @PostMapping("/products-scala") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createProduct(@RequestBody Map<String, Object> payload) { return created(productService, payload); }
    @GetMapping("/products-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getProduct(@PathVariable long id) { return read(productService, id); }
    @PutMapping("/products-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateProduct(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(productService, id, payload); }
    @PatchMapping("/products-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchProduct(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(productService, id, payload); }
    @DeleteMapping("/products-scala/{id}") public ResponseEntity<Void> deleteProduct(@PathVariable long id) { return deleted(productService, id); }
    @GetMapping("/products-scala") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listProducts() { return listed(productService); }

    @PostMapping("/orders-scala") public ResponseEntity<ResponseWrapper<Map<String, Object>>> createOrder(@RequestBody Map<String, Object> payload) { return created(orderService, payload); }
    @GetMapping("/orders-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> getOrder(@PathVariable long id) { return read(orderService, id); }
    @PutMapping("/orders-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> updateOrder(@PathVariable long id, @RequestBody Map<String, Object> payload) { return updated(orderService, id, payload); }
    @PatchMapping("/orders-scala/{id}") public ResponseEntity<ResponseWrapper<Map<String, Object>>> patchOrder(@PathVariable long id, @RequestBody Map<String, Object> payload) { return patched(orderService, id, payload); }
    @DeleteMapping("/orders-scala/{id}") public ResponseEntity<Void> deleteOrder(@PathVariable long id) { return deleted(orderService, id); }
    @GetMapping("/orders-scala") public ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> listOrders() { return listed(orderService); }
}
