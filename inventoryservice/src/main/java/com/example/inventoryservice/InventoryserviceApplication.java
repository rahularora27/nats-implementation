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
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class InventoryserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryserviceApplication.class, args);
	}

	private Connection natsConnection;
	// Hard-coded inventory: key is productId, value is the Product
	private final Map<String, Product> inventory = new HashMap<>();

	@PostConstruct
	public void init() {
		// Initialize inventory
		inventory.put("P1", new Product("Laptop", 5));
		inventory.put("P2", new Product("Smartphone", 10));
		inventory.put("P3", new Product("Headphone", 15));

		try {
			// Establish connection to the NATS server
			natsConnection = Nats.connect("nats://localhost:4222");

			// Create a Dispatcher that will handle incoming messages using the handleOrder method
			Dispatcher dispatcher = natsConnection.createDispatcher(this::handleOrder);

			// Subscribe to the "order.placed" subject with a queue group (for load balancing if needed)
			Dispatcher sub = dispatcher.subscribe("order.placed", "inventoryGroup");

			System.out.println("Inventory Service is listening for orders...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method is invoked when an order event is received.
	 * It validates whether the product exists and if the requested quantity is available,
	 * updates inventory accordingly, and publishes a success or failure event.
	 */
	private void handleOrder(Message msg) {
		String orderJson = new String(msg.getData(), StandardCharsets.UTF_8);
		System.out.println("InventoryService received order: " + orderJson);

		// Simple JSON extraction
		String productId = extractValue(orderJson, "productId");
		int quantity = Integer.parseInt(extractValue(orderJson, "quantity"));

		Product product = inventory.get(productId);
		if (product == null) {
			// Product not found; publish a failure event.
			String failMessage = String.format(
					"{\"orderStatus\":\"failure\",\"reason\":\"Product ID %s not found\"}", productId
			);
			try {
				natsConnection.publish("order.failure", failMessage.getBytes(StandardCharsets.UTF_8));
				System.out.println("Published order failure: " + failMessage);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		if (product.getStock() < quantity) {
			// Insufficient stock; publish a failure event.
			String failMessage = String.format(
					"{\"orderStatus\":\"failure\",\"reason\":\"Insufficient stock for %s\"}", product.getName()
			);
			try {
				natsConnection.publish("order.failure", failMessage.getBytes(StandardCharsets.UTF_8));
				System.out.println("Published order failure: " + failMessage);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		// Valid order – update inventory and publish a success event.
		product.setStock(product.getStock() - quantity);
		String successMessage = String.format(
				"{\"orderStatus\":\"success\",\"productName\":\"%s\",\"quantity\":%d}", product.getName(), quantity
		);
		try {
			natsConnection.publish("order.success", successMessage.getBytes(StandardCharsets.UTF_8));
			System.out.println("Published order success: " + successMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This is a naïve method to extract a value from a JSON-like string.
	 * It looks for the key and its corresponding value. This is for demonstration only.
	 * For a robust solution, use a JSON library such as Jackson.
	 */
	private String extractValue(String json, String key) {
		String searchKey = "\"" + key + "\":";
		int start = json.indexOf(searchKey);
		if (start < 0) {
			return "";
		}
		start += searchKey.length();
		// Check if the value is in double quotes
		if (json.charAt(start) == '"') {
			start++;
			int end = json.indexOf("\"", start);
			return json.substring(start, end);
		} else {
			int end = json.indexOf(",", start);
			if (end < 0) {
				end = json.indexOf("}", start);
			}
			return json.substring(start, end).trim();
		}
	}
}
