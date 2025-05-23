package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The ProductionLine actor manages the car production process for a single assembly line.
 * It coordinates workers to build car bodies and install special requests.
 */
public class ProductionLine extends AbstractBehavior<ProductionLine.Command> {

    // Interface for all possible messages this actor can receive
    public interface Command {}

    //Message from OrderBook checking if this production line is available
    public static final class IsAvailable implements Command {
        public final akka.actor.typed.ActorRef<OrderBook.Command> replyTo;

        public IsAvailable(akka.actor.typed.ActorRef<OrderBook.Command> replyTo) {
            this.replyTo = replyTo;
        }
    }

    //Message to start production of a specific order
    public static final class StartProduction implements Command {
        public final int orderNumber;

        public StartProduction(int orderNumber) {
            this.orderNumber = orderNumber;
        }
    }

    //Message indicating the car body has been built
    public static final class BodyBuilt implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<Worker.Command> worker;

        public BodyBuilt(int orderNumber, akka.actor.typed.ActorRef<Worker.Command> worker) {
            this.orderNumber = orderNumber;
            this.worker = worker;
        }
    }

    //Message indicating special requests have been installed
    public static final class SpecialRequestsInstalled implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<Worker.Command> worker;

        public SpecialRequestsInstalled(int orderNumber, akka.actor.typed.ActorRef<Worker.Command> worker) {
            this.orderNumber = orderNumber;
            this.worker = worker;
        }
    }

    /**
     * Factory method to create the ProductionLine actor
     * @param workers Array of available worker actors
     */
    public static Behavior<Command> create(akka.actor.typed.ActorRef<Worker.Command>[] workers) {
        return Behaviors.setup(context -> new ProductionLine(context, workers));
    }

    private final akka.actor.typed.ActorRef<Worker.Command>[] workers;
    private boolean isAvailable = true;

    private ProductionLine(ActorContext<Command> context,
                           akka.actor.typed.ActorRef<Worker.Command>[] workers) {
        super(context);
        this.workers = workers;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(IsAvailable.class, this::onIsAvailable)
                .onMessage(StartProduction.class, this::onStartProduction)
                .onMessage(BodyBuilt.class, this::onBodyBuilt)
                .onMessage(SpecialRequestsInstalled.class, this::onSpecialRequestsInstalled)
                .build();
    }

    // Handles availability check requests from OrderBook
//    private Behavior<Command> onIsAvailable(IsAvailable msg) {
//        if (isAvailable) {
//            msg.replyTo.tell(new OrderBook.ProductionLineAvailable(getContext().getSelf()));
//            System.out.print("hihi");
//        }
//        return this;
//    }

    private Behavior<Command> onIsAvailable(IsAvailable msg) {
        if (isAvailable) {
            msg.replyTo.tell(new OrderBook.ProductionLineAvailable(getContext().getSelf()));
            System.out.println("hihi");
        }
        else {
            System.out.println("haha");
        }
        return this;
    }

    //Starts production for a new order
    private Behavior<Command> onStartProduction(StartProduction msg) {
        if (isAvailable) {
            isAvailable = false;
            getContext().getLog().info("Starting production for order {}", msg.orderNumber);

            // Assign a random worker
            int workerIndex = ThreadLocalRandom.current().nextInt(workers.length);
            var worker = workers[workerIndex];

            // Schedule body building (takes 5-10 seconds)
            int buildTime = ThreadLocalRandom.current().nextInt(5, 11);
            getContext().scheduleOnce(
                    Duration.ofSeconds(buildTime),
                    getContext().getSelf(),
                    new BodyBuilt(msg.orderNumber, worker)
            );
        }
        return this;
    }

    //Handles completion of car body construction
    private Behavior<Command> onBodyBuilt(BodyBuilt msg) {
        getContext().getLog().info("Order {}: Body built by {}, now installing special requests",
                msg.orderNumber, msg.worker.path().name());

        // Tell worker to install special requests
        msg.worker.tell(new Worker.InstallSpecialRequests(msg.orderNumber, getContext().getSelf()));
        return this;
    }

    //Handles completion of special requests installation
    private Behavior<Command> onSpecialRequestsInstalled(SpecialRequestsInstalled msg) {
        getContext().getLog().info("Order {} completed by {}, production line now available",
                msg.orderNumber, msg.worker.path().name());

        isAvailable = true; // Mark as available for new orders
        return this;
    }
}