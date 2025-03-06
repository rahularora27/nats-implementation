package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import io.nats.client.Connection;
import io.nats.client.Nats;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @GetMapping
    public String info() {
        return "Order Service has started";
    }

    private Connection natsConnection;

    @PostConstruct
    public void init() {
        try {
            natsConnection = Nats.connect("nats://localhost:4222");
            System.out.println("Order Service connected to NATS server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody Order order) {
        try {
            String orderJson = String.format(
                    "{\"productId\":\"%s\", \"quantity\":%d}",
                    order.getProductId(), order.getQuantity());

            // Publish the order event on "order.placed" subject.
            natsConnection.publish("order.placed", orderJson.getBytes(StandardCharsets.UTF_8));
            System.out.println("OrderService published: " + orderJson);
            return new ResponseEntity<>("Post request successful", HttpStatus.CREATED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Error placing order", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
