package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;

public class OrderBook extends AbstractBehavior<OrderBook.Command> {

    public interface Command {}

    // Message: Add new order
    public static final class AddOrder implements Command {
        public final int orderNumber;

        public AddOrder(int orderNumber) {
            this.orderNumber = orderNumber;
        }
    }

    //Message: Try to assign orders
    public static final class AssignOrder implements Command {}

    //Message: Production line is available
    public static final class ProductionLineAvailable implements Command {
        public final akka.actor.typed.ActorRef<ProductionLine.Command> productionLine;

        public ProductionLineAvailable(akka.actor.typed.ActorRef<ProductionLine.Command> productionLine) {
            this.productionLine = productionLine;
        }
    }

    //Creates the OrderBook behavior
    public static Behavior<Command> create(akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers ->
                        new OrderBook(context, timers, productionLines)));
    }

    private final TimerScheduler<Command> timers;
    private final akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines;
    private final Queue<Integer> orders = new LinkedList<>();

    private OrderBook(ActorContext<Command> context,
                      TimerScheduler<Command> timers,
                      akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines) {
        super(context);
        this.timers = timers;
        this.productionLines = productionLines;

        getContext().getLog().info("OrderBook starts: ");
        timers.startTimerWithFixedDelay(new AssignOrder(), Duration.ofSeconds(10));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AddOrder.class, this::onAddOrder)
                .onMessage(AssignOrder.class, this::onAssignOrder)
                .onMessage(ProductionLineAvailable.class, this::onProductionLineAvailable)
                .build();
    }


    //Processes new orders
    private Behavior<Command> onAddOrder(AddOrder msg) {
        getContext().getLog().info("New order received: {}", msg.orderNumber);
        orders.add(msg.orderNumber);
        onAssignOrder(new AssignOrder());
        return this;
    }

    //Tries to assign orders
    private Behavior<Command> onAssignOrder(AssignOrder msg) {
        if (!orders.isEmpty()) {
            getContext().getLog().info("Trying to assign {} pending orders...", orders.size());

            // Asks all production lines for availability
            for (var productionLine : productionLines) {
                productionLine.tell(new ProductionLine.IsAvailable(getContext().getSelf().narrow()));
            }
        } else {
            getContext().getLog().debug("No orders to assign");
        }
        return this;
    }

    //Processes available production lines
        private Behavior<Command> onProductionLineAvailable(ProductionLineAvailable msg) {
        if (!orders.isEmpty()) {
            Integer order = orders.poll();
            getContext().getLog().info("Assigning order {} to production line", order);
            msg.productionLine.tell(new ProductionLine.StartProduction(order));
        }
        return this;
    }
}