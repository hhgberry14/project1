package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a worker who performs car assembly tasks including:
 * - Building car bodies
 * - Installing special requests/features
 * Each worker can only work on one task at a time.
 */
public class Worker extends AbstractBehavior<Worker.Command> {

    //Interface for all worker commands
    public interface Command {}

    /**
     * Message instructing the worker to install special requests for an order
     */
    public static final class InstallSpecialRequests implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<ProductionLine.Command> productionLine;

        public InstallSpecialRequests(int orderNumber,
                                      akka.actor.typed.ActorRef<ProductionLine.Command> productionLine) {
            this.orderNumber = orderNumber;
            this.productionLine = productionLine;
        }
    }

    /**
     * Message indicating special requests have been received from storage
     */
    public static final class SpecialRequestsReceived implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<ProductionLine.Command> productionLine;

        public SpecialRequestsReceived(int orderNumber,
                                       akka.actor.typed.ActorRef<ProductionLine.Command> productionLine) {
            this.orderNumber = orderNumber;
            this.productionLine = productionLine;
        }
    }


    /**
     * Factory method to create a worker
     * @param name Worker identifier
     * @param localStorage Reference to the parts storage system
     */
    public static Behavior<Command> create(String name,
                                           akka.actor.typed.ActorRef<LocalStorage.Command> localStorage) {
        return Behaviors.setup(context -> new Worker(context, name, localStorage));
    }

    private final String name;
    private final akka.actor.typed.ActorRef<LocalStorage.Command> localStorage;


    /**
     * Worker constructor
     */
    private Worker(ActorContext<Command> context, String name,
                   akka.actor.typed.ActorRef<LocalStorage.Command> localStorage) {
        super(context);
        this.name = name;
        this.localStorage = localStorage;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InstallSpecialRequests.class, this::onInstallSpecialRequests)
                .onMessage(SpecialRequestsReceived.class, this::onSpecialRequestsReceived)
                .build();
    }

    /**
     * Handles request to install special features by fetching them from storage
     */
    private Behavior<Command> onInstallSpecialRequests(InstallSpecialRequests msg) {
        getContext().getLog().info("{} fetching special requests for order {}", name, msg.orderNumber);

        // Request parts from storage
        localStorage.tell(new LocalStorage.RequestSpecialItems(msg.orderNumber, getContext().getSelf(), msg.productionLine));
        return this;
    }


    /**
     * Handles completion of special requests installation
     */
    private Behavior<Command> onSpecialRequestsReceived(SpecialRequestsReceived msg) {
        getContext().getLog().info("{} installed special requests for order {}", name, msg.orderNumber);

        // Notify production line that work is done
        msg.productionLine.tell(new ProductionLine.SpecialRequestsInstalled(msg.orderNumber, getContext().getSelf()));
        return this;
    }
}