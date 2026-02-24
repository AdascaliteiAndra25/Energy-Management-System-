package org.example.simulator;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.FileInputStream;
import java.time.LocalTime;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

public class DeviceSimulator {
    private static final String QUEUE_NAME = "data_collection_queue";
    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    public static void main(String[] args) {

        Properties config = new Properties();
        Long deviceId;
        String host;
        
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            config.load(fis);

            String deviceIdStr = config.getProperty("device.id");
            if (deviceIdStr == null || deviceIdStr.trim().isEmpty()) {
                System.err.println("ERROR: device.id is not configured in config.properties");
                System.err.println("Please set device.id in config.properties file");
                System.exit(1);
            }
            
            deviceId = Long.parseLong(deviceIdStr);
            host = config.getProperty("rabbitmq.host", "localhost");

            System.out.println("  Device ID: " + deviceId);
            System.out.println("  RabbitMQ Host: " + host);
        } catch (java.io.FileNotFoundException e) {
            System.err.println("ERROR: config.properties file not found!");
            System.err.println("Please create config.properties file with device.id configuration");
            System.exit(1);
            return;
        } catch (NumberFormatException e) {
            System.err.println("ERROR: device.id must be a valid number in config.properties");
            System.exit(1);
            return;
        } catch (Exception e) {
            System.err.println("ERROR: Could not load config.properties: " + e.getMessage());
            System.exit(1);
            return;
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername("user");
        factory.setPassword("password");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            
            System.out.println("\n=== Device Simulator Started ===");
            System.out.println("Device ID: " + deviceId);
            System.out.println("Sending measurements every 10 minutes");
            System.out.println("Press Ctrl+C to stop\n");

            double baseLoad = 0.5 + random.nextDouble() * 1.5;

            while (true) {
                double consumption = generateConsumption(baseLoad);
                
                DeviceMeasurement measurement = new DeviceMeasurement(
                    System.currentTimeMillis(),
                    deviceId,
                    consumption
                );

                String message = gson.toJson(measurement);
                

                com.rabbitmq.client.AMQP.BasicProperties props = new com.rabbitmq.client.AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .build();
                
                channel.basicPublish("", QUEUE_NAME, props, message.getBytes());
                
                System.out.println("Sent: " + String.format("%.2f kWh", consumption) + 
                    " at " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

                Thread.sleep(10000);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static double generateConsumption(double baseLoad) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        
        double hourlyFactor;
        if (hour >= 0 && hour < 6) {
            hourlyFactor = 0.5;
        } else if (hour >= 6 && hour < 9) {
            hourlyFactor = 1.2;
        } else if (hour >= 9 && hour < 17) {
            hourlyFactor = 0.8;
        } else if (hour >= 17 && hour < 22) {
            hourlyFactor = 1.5;
        } else {
            hourlyFactor = 1.0;
        }
        
        double randomVariation = 0.9 + random.nextDouble() * 0.2;
        
        return baseLoad * hourlyFactor * randomVariation;
    }
}
