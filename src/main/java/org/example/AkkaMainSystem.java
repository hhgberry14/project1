package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class AkkaMainSystem extends AbstractBehavior<AkkaMainSystem.Create> {

    public static final class Create {}

    public static Behavior<Create> create() {
        return Behaviors.setup(AkkaMainSystem::new);
    }

    private final ActorContext<Create> context;

    private AkkaMainSystem(ActorContext<Create> context) {
        super(context);
        this.context = context;

        // Initialize all actors
        var localStorage = context.spawn(LocalStorage.create(), "localStorage");
        var workers = new akka.actor.typed.ActorRef[4];
        for (int i = 0; i < 4; i++) {
            workers[i] = context.spawn(Worker.create("Worker-" + (i+1), localStorage), "worker-" + (i+1));
        }

        var productionLines = new akka.actor.typed.ActorRef[2];
        for (int i = 0; i < 2; i++) {
            productionLines[i] = context.spawn(ProductionLine.create(workers), "productionLine-" + (i+1));
        }

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