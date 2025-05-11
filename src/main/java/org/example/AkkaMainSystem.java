package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

/**
 * The main system actor that initializes and coordinates all other actors in the car production system.
 * This serves as the root actor of the actor hierarchy.
 */
public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Create> {

    //Simple message to trigger system initialization
    public static final class Create {}

    public static Behavior<Create> create() {
        return Behaviors.setup(AkkaMainSystem::new);
    }

    private final ActorContext<Create> context;

    /**
     * Constructor that initializes all system components
     */
    private AkkaMainSystem(ActorContext<Create> context) {
        super(context);
        this.context = context;

        // Initialize all actors
        var localStorage = context.spawn(LocalStorage.create(), "localStorage");

        // Create 4 worker actors with references to the storage
        var workers = new akka.actor.typed.ActorRef[4];
        for (int i = 0; i < 4; i++) {
            workers[i] = context.spawn(Worker.create("Worker-" + (i+1), localStorage), "worker-" + (i+1));
        }

        // Create 2 production lines with access to all workers
        var productionLines = new akka.actor.typed.ActorRef[2];
        for (int i = 0; i < 2; i++) {
            productionLines[i] = context.spawn(ProductionLine.create(workers), "productionLine-" + (i+1));
        }

        // Create the order book that manages production assignments
        var orderBook = context.spawn(OrderBook.create(productionLines), "orderBook");

        // Start generating orders
        context.spawn(OrderGenerator.create(orderBook), "orderGenerator");
    }

    @Override
    public Receive<Create> createReceive() {
        return newReceiveBuilder()
                .onMessage(Create.class, msg -> Behaviors.same())
                .build();
    }
}