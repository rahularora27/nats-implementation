package com.example.notificationservice;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class NotificationserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationserviceApplication.class, args);
	}

	private Connection natsConnection;

	@PostConstruct
	public void init() {
		try {
			// Connection to the NATS server
			natsConnection = Nats.connect("nats://localhost:4222");
			System.out.println("Notification Service connected to NATS server");

			// Creation of a Dispatcher that handles the incoming message
			Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);

			// Subscribe to both "order.success" and "order.failure" subjects.
			// Using "notificationGroup" as the queue group for load balancing if running multiple instances.
			Dispatcher subSuccess = dispatcher.subscribe("order.success", "NotificationQG");
			Dispatcher subFailure = dispatcher.subscribe("order.failure", "NotificationQG");

			System.out.println("Notification Service is listening for order status");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method processes incoming messages from NATS.
	 * It checks the message subject and prints out a success or failure notification.
	 */
	private void handleMessage(Message msg) {
		String subject = msg.getSubject();
		String productId = new String(msg.getData());

		if ("order.success".equals(subject)) {
			System.out.println("Order placed successfully for " + productId);
		} else if ("order.failure".equals(subject)) {
			System.out.println("Order did not get placed for " + productId);
		} else {
			System.out.println("Received message on subject " + subject + ": " + productId);
		}
	}
}
