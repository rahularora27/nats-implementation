package com.example.inventoryservice;

import com.example.inventoryservice.model.Product;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class InventoryserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryserviceApplication.class, args);
	}

	private Connection natsConnection;

	@PostConstruct
	public void init() {
		try {
			// Connection to the NATS server
			natsConnection = Nats.connect("nats://localhost:4222");
			System.out.println("Inventory Service connected to NATS server");

			// Creation of a Dispatcher that handles the incoming message
			Dispatcher dispatcher = natsConnection.createDispatcher(this::handleOrder);

			// Subscription to "order.placed" subject
			Dispatcher sub = dispatcher.subscribe("order.placed");

			System.out.println("Inventory Service is listening for orders");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method is invoked when an order event is received.
	 * publishes a success or failure event.
	 */
	private void handleOrder(Message msg) {
		String productId = new String(msg.getData());

		// publish a success event
		try {
			natsConnection.publish("order.success", productId.getBytes());
			System.out.println("Published order success: " + productId);
		} catch (Exception e) {
			e.printStackTrace();
			natsConnection.publish("order.failure", productId.getBytes());
			System.out.println("Published order failure: " + productId);
		}
	}
}
