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
    private boolean isWaitingForProductionLine = false;

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
        // Versuche sofort Aufträge zuzuweisen, wenn eine Produktionslinie verfügbar ist
        onAssignOrder(new AssignOrder());
        return this;
    }

    /**
     * Versucht Aufträge zuzuweisen
     * Tries to assign orders
     */
    private Behavior<Command> onAssignOrder(AssignOrder msg) {
        if (!orders.isEmpty()) {
            getContext().getLog().info("Trying to assign {} pending orders...", orders.size());

            // Fragt alle Fertigungsstraßen nach Verfügbarkeit
            // Asks all production lines for availability
            for (var productionLine : productionLines) {
                productionLine.tell(new ProductionLine.IsAvailable(getContext().getSelf().narrow()));
            }
            //isWaitingForProductionLine = true; // Setze den Status auf "Warten"
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
        getContext().getLog().debug("Current orders: {}", orders);
        if (!orders.isEmpty()) {
            Integer order = orders.poll();
            getContext().getLog().info("Assigning order {} to production line", order);
            msg.productionLine.tell(new ProductionLine.StartProduction(order));
        }
        else {
            // getContext().getLog().debug("Received availability but no orders pending");
        }
        return this;
    }
}