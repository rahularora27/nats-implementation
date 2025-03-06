package com.example.notificationservice;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Subscription;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class NotificationserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationserviceApplication.class, args);
	}

	private Connection natsConnection;

	@PostConstruct
	public void init() {
		try {
			// Connect to the NATS server (adjust URL if needed)
			natsConnection = Nats.connect("nats://localhost:4222");

			// Create a Dispatcher that handles messages via our handleMessage method
			Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);

			// Subscribe to both "order.success" and "order.failure" subjects.
			// Using "notificationGroup" as the queue group for load balancing if running multiple instances.
			Dispatcher subSuccess = dispatcher.subscribe("order.success", "notificationGroup");
			Dispatcher subFailure = dispatcher.subscribe("order.failure", "notificationGroup");

			System.out.println("Notification Service is listening for events...");
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
		String payload = new String(msg.getData(), StandardCharsets.UTF_8);

		if ("order.success".equals(subject)) {
			System.out.println("Notification Service: Order placed successfully. Details: " + payload);
		} else if ("order.failure".equals(subject)) {
			System.out.println("Notification Service: Order did not get placed. Details: " + payload);
		} else {
			System.out.println("Notification Service: Received message on subject " + subject + ": " + payload);
		}
	}
}
