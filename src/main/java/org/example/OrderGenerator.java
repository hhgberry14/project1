package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Actor responsible for generating new car orders at fixed intervals.
 * Simulates customer orders coming into the system.
 */

public class OrderGenerator extends AbstractBehavior<OrderGenerator.GenerateOrder> {

    //Internal message to trigger order generation
    public static final class GenerateOrder {}

    /**
     * Factory method to create the order generator
     * @param orderBook Reference to the order book where new orders will be sent
     */
    public static Behavior<GenerateOrder> create(akka.actor.typed.ActorRef<OrderBook.AddOrder> orderBook) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers ->
                new OrderGenerator(context, timers, orderBook)));
    }

    private final akka.actor.typed.ActorRef<OrderBook.AddOrder> orderBook;
    private int orderCounter = 1;

    private OrderGenerator(ActorContext<GenerateOrder> context,
                           TimerScheduler<GenerateOrder> timers,
                           akka.actor.typed.ActorRef<OrderBook.AddOrder> orderBook) {
        super(context);
        this.orderBook = orderBook;

        // Schedule order generation every 15 seconds
        timers.startTimerWithFixedDelay(new GenerateOrder(), Duration.ofSeconds(15));
    }

    @Override
    public Receive<GenerateOrder> createReceive() {
        return newReceiveBuilder()
                .onMessage(GenerateOrder.class, this::onGenerateOrder)
                .build();
    }

    /**
     * Handles order generation by creating a new order number and sending it to the order book
     */
    private Behavior<GenerateOrder> onGenerateOrder(GenerateOrder msg) {
        int orderNumber = orderCounter++;
        getContext().getLog().info("Generating new order: {}", orderNumber);
        orderBook.tell(new OrderBook.AddOrder(orderNumber));
        return this;
    }
}