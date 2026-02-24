package org.example.monitoringservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.monitoringservice.dto.DeviceMeasurementDTO;
import org.example.monitoringservice.service.MonitoringService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataConsumer {

    private final MonitoringService monitoringService;

    @RabbitListener(queues = "${data.queue.name}")
    public void consumeMessage(DeviceMeasurementDTO measurement) {
        log.info("Received message from queue: {}", measurement);
        monitoringService.processMeasurement(measurement);
    }
}