package org.example.monitoringservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${data.queue.name}")
    private String dataQueueName;

    @Value("${sync.queue.name}")
    private String syncQueueName;

    @Value("${notification.queue.name}")
    private String notificationQueueName;

    // Notification exchange and routing key
    public static final String NOTIFICATION_EXCHANGE = "notification_exchange";
    public static final String OVERCONSUMPTION_ROUTING_KEY = "notification.overconsumption";

    @Bean
    public Queue dataQueue() {
        return new Queue(dataQueueName, true);
    }

    @Bean
    public Queue syncQueue() {
        return new Queue(syncQueueName, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(notificationQueueName, true);
    }


    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(OVERCONSUMPTION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(converter.getJavaTypeMapper());
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setPriority(null);
            return message;
        });
        return rabbitTemplate;
    }
}
