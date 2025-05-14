package org.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages inventory of special car parts/features that workers can install.
 * Handles both part distribution and restocking when inventory runs low.
 */
public class LocalStorage extends AbstractBehavior<LocalStorage.Command> {


     //Interface for all storage commands
    public interface Command {}


     //Message requesting special items for a car order
    public static final class RequestSpecialItems implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<Worker.Command> worker;
        public final akka.actor.typed.ActorRef<ProductionLine.Command> productionLine;

        public RequestSpecialItems(int orderNumber,
                                   akka.actor.typed.ActorRef<Worker.Command> worker,
                                   akka.actor.typed.ActorRef<ProductionLine.Command> productionLine) {
            this.orderNumber = orderNumber;
            this.worker = worker;
            this.productionLine = productionLine;
        }
    }

    //Message indicating restocking has been completed
    public static final class RestockCompleted implements Command {
        public final String[] itemNames;

        public RestockCompleted(String[] itemNames) {
            this.itemNames = itemNames;
        }
    }

    // Available special features in the system
    private static final String[] SPECIAL_ITEMS = {
            "Ledersitze", "Klimaautomatik", "Elektrische Fensterheber", "Automatikgetriebe"
    };

    // Factory method to create storage actor
    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new LocalStorage(context));
    }

    private final Map<String, Integer> inventory = new HashMap<>();
    private final ActorContext<Command> context;

    //Constructor that initializes inventory
    private LocalStorage(ActorContext<Command> context) {
        super(context);
        this.context = context;

        // Initialize with 4 units of each special item
        for (String item : SPECIAL_ITEMS) {
            inventory.put(item, 4);
        }
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RequestSpecialItems.class, this::onRequestSpecialItems)
                .onMessage(RestockCompleted.class, this::onRestockCompleted)
                .build();
    }

    //Handles requests for special items from workers
    private Behavior<Command> onRequestSpecialItems(RequestSpecialItems msg) {

        // Randomly select two distinct special items
        String item1 = SPECIAL_ITEMS[ThreadLocalRandom.current().nextInt(SPECIAL_ITEMS.length)];
        String item2;
        do {
            item2 = SPECIAL_ITEMS[ThreadLocalRandom.current().nextInt(SPECIAL_ITEMS.length)];
        } while (item2.equals(item1));

        boolean allAvailable = true;
        String[] requestedItems = {item1, item2};

        // Check availability
        for (String item : requestedItems) {
            if (inventory.getOrDefault(item, 0) <= 0) {
                allAvailable = false;
                break;
            }
        }

        if (allAvailable) {
            // Deduct from inventory and fulfill request
            for (String item : requestedItems) {
                inventory.put(item, inventory.get(item) - 1);
            }
            context.getLog().info("Special items {} and {} provided for order {}", item1, item2, msg.orderNumber);

            // Notify worker immediately
            msg.worker.tell(new Worker.SpecialRequestsReceived(msg.orderNumber, msg.productionLine));
        } else {
            context.getLog().info("Special items not available, restocking for order {}", msg.orderNumber);

            // Initiate restocking (takes 10-15 seconds)
            int restockTime = ThreadLocalRandom.current().nextInt(10, 16);
            context.scheduleOnce(
                    Duration.ofSeconds(restockTime),
                    context.getSelf(),
                    new RestockCompleted(SPECIAL_ITEMS)
            );
        }

        return this;
    }

    //Handles completion of restocking operation
    private Behavior<Command> onRestockCompleted(RestockCompleted msg) {
        for (String item : msg.itemNames) {
            inventory.put(item, inventory.getOrDefault(item, 0) + 3);
        }
        context.getLog().info("Inventory restocked");
        return this;
    }
}