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
            // Connection to the NATS server
            natsConnection = Nats.connect("nats://localhost:4222");
            System.out.println("Order Service connected to NATS server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody Order order) {
        try {
            // Publish the order event on "order.placed" subject.
            int n = 10;
            for (int i=0; i<n; i++) {
                natsConnection.publish("order.placed", order.getProductId().getBytes());
            }
            System.out.println("OrderService published: " + n + " x " + order.getProductId());
            return new ResponseEntity<>("Post request successful", HttpStatus.CREATED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Error placing order", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
