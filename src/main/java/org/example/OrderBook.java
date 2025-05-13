//package org.example;
//
//import akka.actor.typed.*;
//import akka.actor.typed.javadsl.*;
//
//import java.util.*;
//
//public class OrderBook extends AbstractBehavior<OrderBook.Command> {
//
//    public interface Command {}
//
//    // Converted to record
//    public record NewOrder(String orderId) implements Command {}
//
//    // Converted to record
//    public record MarkFree(int lineNumber) implements Command {}
//
//    public static Behavior<Command> create(ActorRef<ProductionLine.Command> line1,
//                                         ActorRef<ProductionLine.Command> line2) {
//        return Behaviors.setup(ctx -> new OrderBook(ctx, line1, line2));
//    }
//
//    private final Queue<String> orders = new LinkedList<>();
//    private final ActorRef<ProductionLine.Command> line1;
//    private final ActorRef<ProductionLine.Command> line2;
//    private boolean line1Busy = false;
//    private boolean line2Busy = false;
//
//    private OrderBook(ActorContext<Command> context,
//                     ActorRef<ProductionLine.Command> line1,
//                     ActorRef<ProductionLine.Command> line2) {
//        super(context);
//        this.line1 = line1;
//        this.line2 = line2;
//    }
//
//    @Override
//    public Receive<Command> createReceive() {
//        return newReceiveBuilder()
//            .onMessage(NewOrder.class, this::onNewOrder)
//            .onMessage(MarkFree.class, this::onMarkFree)
//            .build();
//    }
//
//    private Behavior<Command> onNewOrder(NewOrder msg) {
//        orders.add(msg.orderId());
//        getContext().getLog().info("New order received: {}", msg.orderId());
//        assignOrders();
//        return this;
//    }
//
//    private Behavior<Command> onMarkFree(MarkFree msg) {
//        if (msg.lineNumber() == 1) line1Busy = false;
//        else if (msg.lineNumber() == 2) line2Busy = false;
//        assignOrders();
//        return this;
//    }
//
//    private void assignOrders() {
//        if (!orders.isEmpty()) {
//            String order = orders.peek();
//            if (!line1Busy) {
//                orders.remove();
//                line1Busy = true;
//                line1.tell(new ProductionLine.StartProduction(order, 1));
//            } else if (!line2Busy) {
//                orders.remove();
//                line2Busy = true;
//                line2.tell(new ProductionLine.StartProduction(order, 2));
//            }
//        }
//    }
//}

//package org.example;
//
//import akka.actor.typed.Behavior;
//import akka.actor.typed.javadsl.AbstractBehavior;
//import akka.actor.typed.javadsl.ActorContext;
//import akka.actor.typed.javadsl.Behaviors;
//import akka.actor.typed.javadsl.Receive;
//import akka.actor.typed.javadsl.TimerScheduler;
//
//import java.time.Duration;
//import java.util.LinkedList;
//import java.util.Queue;
//
///**
// * The OrderBook actor manages incoming car orders and assigns them to available production lines.
// * It maintains a queue of pending orders and periodically checks for available production lines.
// */
//public class OrderBook extends AbstractBehavior<OrderBook.Command> {
//
//    // Interface for all possible messages this actor can receive
//    public interface Command {
//    }
//
//
//    //Message representing a new order to be added to the book
//    public static final class AddOrder implements Command {
//        public final int orderNumber;
//
//        public AddOrder(int orderNumber) {
//            this.orderNumber = orderNumber;
//        }
//    }
//
//    //Internal message to trigger order assignment attempts
//    public static final class AssignOrder implements Command {}
//
//    //Message from ProductionLine indicating it's available for work
//    public static final class ProductionLineAvailable implements Command {
//        public final akka.actor.typed.ActorRef<ProductionLine.Command> productionLine;
//
//        public ProductionLineAvailable(akka.actor.typed.ActorRef<ProductionLine.Command> productionLine) {
//            this.productionLine = productionLine;
//        }
//    }
//
//    /**
//     * Factory method to create the OrderBook actor
//     * @param productionLines Array of available production line actors
//     */
//    public static Behavior<Command> create(akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines) {
//        return Behaviors.setup(context -> Behaviors.withTimers(timers ->
//                new OrderBook(context, timers, productionLines)));
//    }
//
//    private final TimerScheduler<Command> timers;
//    private final akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines;
//    private final Queue<Integer> orders = new LinkedList<>();
//
//    private OrderBook(ActorContext<Command> context,
//                      TimerScheduler<Command> timers,
//                      akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines) {
//        super(context);
//        this.timers = timers;
//        this.productionLines = productionLines;
//
//        // Schedule periodic order assignment attempts every second
//        timers.startTimerWithFixedDelay(new AssignOrder(), Duration.ofSeconds(1));
//    }
//
//    @Override
//    public Receive<Command> createReceive() {
//        return newReceiveBuilder()
//                .onMessage(AddOrder.class, this::onAddOrder)
//                .onMessage(AssignOrder.class, this::onAssignOrder)
//                .onMessage(ProductionLineAvailable.class, this::onProductionLineAvailable)
//                .build();
//    }
//
//    //Handles new order additions to the book
//    private Behavior<Command> onAddOrder(AddOrder msg) {
//        getContext().getLog().info("Order added to book: {}", msg.orderNumber);
//        orders.add(msg.orderNumber);
//        return this;
//    }
//
//   //Handles periodic order assignment attempts
//    private Behavior<Command> onAssignOrder(AssignOrder msg) {
//        if (!orders.isEmpty()) {
//            for (var productionLine : productionLines) {
//                productionLine.tell(new ProductionLine.IsAvailable(getContext().getSelf().narrow()));
//            }
//        }
//        return this;
//    }
//
//    //Handles production line availability responses
//    private Behavior<Command> onProductionLineAvailable(ProductionLineAvailable msg) {
//        if (!orders.isEmpty()) {
//
//            // Assign the oldest order to the available production line
//            Integer order = orders.poll();
//            msg.productionLine.tell(new ProductionLine.StartProduction(order));
//            getContext().getLog().info("Order {} assigned to production line", order);
//        }
//        return this;
//    }
//}

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

/**
 * OrderBook - Verwaltet die Auftragsliste und weist Aufträge den Fertigungsstraßen zu
 *
 * OrderBook - Manages the order list and assigns orders to production lines
 */
public class OrderBook extends AbstractBehavior<OrderBook.Command> {

    // Nachrichtenschnittstelle / Message interface
    public interface Command {}

    /**
     * Nachricht: Neuer Auftrag hinzufügen
     * Message: Add new order
     */
    public static final class AddOrder implements Command {
        public final int orderNumber;

        public AddOrder(int orderNumber) {
            this.orderNumber = orderNumber;
        }
    }

    /**
     * Nachricht: Auftragszuweisung versuchen
     * Message: Try to assign orders
     */
    public static final class AssignOrder implements Command {}

    /**
     * Nachricht: Fertigungsstraße ist verfügbar
     * Message: Production line is available
     */
    public static final class ProductionLineAvailable implements Command {
        public final akka.actor.typed.ActorRef<ProductionLine.Command> productionLine;

        public ProductionLineAvailable(akka.actor.typed.ActorRef<ProductionLine.Command> productionLine) {
            this.productionLine = productionLine;
        }
    }

    /**
     * Erstellt das OrderBook-Verhalten
     * Creates the OrderBook behavior
     */
    public static Behavior<Command> create(akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers ->
                        new OrderBook(context, timers, productionLines)));
    }

    private final TimerScheduler<Command> timers;
    private final akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines;
    private final Queue<Integer> orders = new LinkedList<>();

    /**
     * Konstruktor
     * Constructor
     */
    private OrderBook(ActorContext<Command> context,
                      TimerScheduler<Command> timers,
                      akka.actor.typed.ActorRef<ProductionLine.Command>[] productionLines) {
        super(context);
        this.timers = timers;
        this.productionLines = productionLines;

        // Startet Timer für alle 10 Sekunden zur Auftragszuweisung
        // Starts timer for order assignment every 10 seconds
        timers.startTimerWithFixedDelay(new AssignOrder(), Duration.ofSeconds(10));

        getContext().getLog().info("OrderBook started with {} production lines", productionLines.length);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AddOrder.class, this::onAddOrder)
                .onMessage(AssignOrder.class, this::onAssignOrder)
                .onMessage(ProductionLineAvailable.class, this::onProductionLineAvailable)
                .build();
    }

    /**
     * Verarbeitet neue Aufträge
     * Processes new orders
     */
    private Behavior<Command> onAddOrder(AddOrder msg) {
        getContext().getLog().info("New order received: {}", msg.orderNumber);
        orders.add(msg.orderNumber);
        return this;
    }

    /**
     * Versucht Aufträge zuzuweisen (wird alle 10 Sekunden aufgerufen)
     * Tries to assign orders (called every 10 seconds)
     */
    private Behavior<Command> onAssignOrder(AssignOrder msg) {
        if (!orders.isEmpty()) {
            getContext().getLog().info("Trying to assign {} pending orders...", orders.size());

            // Fragt alle Fertigungsstraßen nach Verfügbarkeit
            // Asks all production lines for availability
            for (var productionLine : productionLines) {
                productionLine.tell(new ProductionLine.IsAvailable(getContext().getSelf().narrow()));
            }
        } else {
            getContext().getLog().debug("No orders to assign");
        }
        return this;
    }

    /**
     * Verarbeitet verfügbare Fertigungsstraßen
     * Processes available production lines
     */
    private Behavior<Command> onProductionLineAvailable(ProductionLineAvailable msg) {
        if (!orders.isEmpty()) {
            Integer order = orders.poll();
            getContext().getLog().info("Assigning order {} to production line", order);
            msg.productionLine.tell(new ProductionLine.StartProduction(order));
        } else {
            getContext().getLog().debug("Received availability but no orders pending");
        }
        return this;
    }
}