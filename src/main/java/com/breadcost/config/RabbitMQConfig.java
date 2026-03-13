package com.breadcost.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ configuration — B1: Event bus infrastructure.
 * Defines a topic exchange with 3 priority queues and a dead-letter queue.
 * Active only when "rabbit" profile is enabled.
 */
@Configuration
@Profile("rabbit")
public class RabbitMQConfig {

    public static final String EXCHANGE = "breadcost.events";
    public static final String QUEUE_HIGH = "events.high";
    public static final String QUEUE_NORMAL = "events.normal";
    public static final String QUEUE_LOW = "events.low";
    public static final String DLQ = "events.dlq";
    public static final String DLX = "breadcost.events.dlx";

    // ── Exchange ──────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX, true, false);
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue highPriorityQueue() {
        return QueueBuilder.durable(QUEUE_HIGH)
                .withArgument("x-dead-letter-exchange", DLX)
                .build();
    }

    @Bean
    public Queue normalPriorityQueue() {
        return QueueBuilder.durable(QUEUE_NORMAL)
                .withArgument("x-dead-letter-exchange", DLX)
                .build();
    }

    @Bean
    public Queue lowPriorityQueue() {
        return QueueBuilder.durable(QUEUE_LOW)
                .withArgument("x-dead-letter-exchange", DLX)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────
    // High: order.*, inventory.*
    // Normal: production.*, notification.*
    // Low: audit.*, analytics.*

    @Bean
    public Binding bindHighOrders() {
        return BindingBuilder.bind(highPriorityQueue()).to(eventExchange()).with("order.*");
    }

    @Bean
    public Binding bindHighInventory() {
        return BindingBuilder.bind(highPriorityQueue()).to(eventExchange()).with("inventory.*");
    }

    @Bean
    public Binding bindNormalProduction() {
        return BindingBuilder.bind(normalPriorityQueue()).to(eventExchange()).with("production.*");
    }

    @Bean
    public Binding bindNormalNotification() {
        return BindingBuilder.bind(normalPriorityQueue()).to(eventExchange()).with("notification.*");
    }

    @Bean
    public Binding bindLowAudit() {
        return BindingBuilder.bind(lowPriorityQueue()).to(eventExchange()).with("audit.*");
    }

    @Bean
    public Binding bindLowAnalytics() {
        return BindingBuilder.bind(lowPriorityQueue()).to(eventExchange()).with("analytics.*");
    }

    @Bean
    public Binding bindDlq() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    // ── Message converter ─────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EXCHANGE);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
