//package org.example;
//
//import akka.actor.typed.*;
//import akka.actor.typed.javadsl.*;
//
//import java.time.Duration;
//import java.util.*;
//
//public class LocalStorage extends AbstractBehavior<LocalStorage.Command> {
//
//    public interface Command {}
//
//    public static final class RequestSpecialRequests implements Command {
//        public final ActorRef<Worker.Command> worker;
//        public final String orderId;
//        public final ActorRef<ProductionLine.Command> productionLine;
//        public final int lineNumber;
//
//        public RequestSpecialRequests(ActorRef<Worker.Command> worker,
//                                      String orderId,
//                                      ActorRef<ProductionLine.Command> productionLine,
//                                      int lineNumber) {
//            this.worker = worker;
//            this.orderId = orderId;
//            this.productionLine = productionLine;
//            this.lineNumber = lineNumber;
//        }
//    }
//
//    private static final class RestockCompleted implements Command {
//        public final String item;
//
//        public RestockCompleted(String item) {
//            this.item = item;
//        }
//    }
//
//    private static final String[] SPECIAL_REQUESTS = {
//            "Ledersitze", "Klimaautomatik", "Elektrische Fensterheber", "Automatikgetriebe"
//    };
//
//    private final Map<String, Integer> inventory = new HashMap<>();
//    private final Random random = new Random();
//    private final TimerScheduler<Command> timers;
//
//    public static Behavior<Command> create() {
//        return Behaviors.setup(context ->
//                Behaviors.withTimers(timers -> new LocalStorage(context, timers))
//        );
//    }
//
//    private LocalStorage(ActorContext<Command> context, TimerScheduler<Command> timers) {
//        super(context);
//        this.timers = timers;
//        for (String item : SPECIAL_REQUESTS) {
//            inventory.put(item, 4);
//        }
//    }
//
//    @Override
//    public Receive<Command> createReceive() {
//        return newReceiveBuilder()
//                .onMessage(RequestSpecialRequests.class, this::onRequestSpecialRequests)
//                .onMessage(RestockCompleted.class, this::onRestockCompleted)
//                .build();
//    }
//
//    private Behavior<Command> onRequestSpecialRequests(RequestSpecialRequests msg) {
//        String item1 = SPECIAL_REQUESTS[random.nextInt(SPECIAL_REQUESTS.length)];
//        String item2;
//        do {
//            item2 = SPECIAL_REQUESTS[random.nextInt(SPECIAL_REQUESTS.length)];
//        } while (item2.equals(item1));
//
//        if (inventory.get(item1) > 0 && inventory.get(item2) > 0) {
//            inventory.put(item1, inventory.get(item1) - 1);
//            inventory.put(item2, inventory.get(item2) - 1);
//            msg.worker.tell(new Worker.SpecialRequestsResponse(
//                    Arrays.asList(item1, item2),
//                    msg.orderId,
//                    msg.productionLine,
//                    getContext().getSystem().ignoreRef(),
//                    msg.lineNumber
//            ));
//        } else {
//            getContext().getLog().warn("Items out of stock: {}, {}. Restocking...", item1, item2);
//            // Restock both items
//            int restockTime = 10 + random.nextInt(6); // 10-15 seconds
//            timers.startSingleTimer(
//                    "restock-" + item1,
//                    new RestockCompleted(item1),
//                    Duration.ofSeconds(restockTime)
//            );
//            timers.startSingleTimer(
//                    "restock-" + item2,
//                    new RestockCompleted(item2),
//                    Duration.ofSeconds(restockTime)
//            );
//        }
//        return this;
//    }
//
//    private Behavior<Command> onRestockCompleted(RestockCompleted msg) {
//        inventory.put(msg.item, inventory.getOrDefault(msg.item, 0) + 3);
//        getContext().getLog().info("Restocked {} to {}", msg.item, inventory.get(msg.item));
//        return this;
//    }
//}

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

public class LocalStorage extends AbstractBehavior<LocalStorage.Command> {

    public interface Command {}

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

    public static final class RestockCompleted implements Command {
        public final String[] itemNames;

        public RestockCompleted(String[] itemNames) {
            this.itemNames = itemNames;
        }
    }

    private static final String[] SPECIAL_ITEMS = {
            "Ledersitze", "Klimaautomatik", "Elektrische Fensterheber", "Automatikgetriebe"
    };

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new LocalStorage(context));
    }

    private final Map<String, Integer> inventory = new HashMap<>();
    private final ActorContext<Command> context;

    private LocalStorage(ActorContext<Command> context) {
        super(context);
        this.context = context;

        // Initialize inventory
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

    private Behavior<Command> onRequestSpecialItems(RequestSpecialItems msg) {
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
            // Deduct from inventory
            for (String item : requestedItems) {
                inventory.put(item, inventory.get(item) - 1);
            }
            context.getLog().info("Special items {} and {} provided for order {}", item1, item2, msg.orderNumber);

            // Notify worker immediately
            msg.worker.tell(new Worker.SpecialRequestsReceived(msg.orderNumber, msg.productionLine));
        } else {
            context.getLog().info("Special items not available, restocking for order {}", msg.orderNumber);

            // Restock all items
            int restockTime = ThreadLocalRandom.current().nextInt(10, 16);
            context.scheduleOnce(
                    Duration.ofSeconds(restockTime),
                    context.getSelf(),
                    new RestockCompleted(SPECIAL_ITEMS)
            );
        }

        return this;
    }

    private Behavior<Command> onRestockCompleted(RestockCompleted msg) {
        for (String item : msg.itemNames) {
            inventory.put(item, inventory.getOrDefault(item, 0) + 3);
        }
        context.getLog().info("Inventory restocked");
        return this;
    }
}