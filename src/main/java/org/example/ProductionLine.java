//package org.example;
//
//import akka.actor.typed.*;
//import akka.actor.typed.javadsl.*;
//
//import java.util.*;
//
//public class ProductionLine extends AbstractBehavior<ProductionLine.Command> {
//
//    public interface Command {}
//
//    public static final class StartProduction implements Command {
//        public final String orderId;
//        public final int lineNumber;
//
//        public StartProduction(String orderId, int lineNumber) {
//            this.orderId = orderId;
//            this.lineNumber = lineNumber;
//        }
//    }
//
//    public static final class RegisterWorkers implements Command {
//        public final List<ActorRef<Worker.Command>> workers;
//
//        @SafeVarargs
//        public RegisterWorkers(ActorRef<Worker.Command>... workers) {
//            this.workers = Arrays.asList(workers);
//        }
//    }
//
//    public static final class WorkerAvailable implements Command {
//        public final ActorRef<Worker.Command> worker;
//        public final int lineNumber;
//
//        public WorkerAvailable(ActorRef<Worker.Command> worker, int lineNumber) {
//            this.worker = worker;
//            this.lineNumber = lineNumber;
//        }
//    }
//
//    public static Behavior<Command> create(ActorRef<LocalStorage.Command> localStorage) {
//        return Behaviors.setup(ctx -> new ProductionLine(ctx, localStorage));
//    }
//
//    private final List<ActorRef<Worker.Command>> workers = new ArrayList<>();
//    private final Set<ActorRef<Worker.Command>> busyWorkers = new HashSet<>();
//    private final ActorRef<LocalStorage.Command> localStorage;
//
//    private ProductionLine(ActorContext<Command> context, ActorRef<LocalStorage.Command> localStorage) {
//        super(context);
//        this.localStorage = localStorage;
//    }
//
//    @Override
//    public Receive<Command> createReceive() {
//        return newReceiveBuilder()
//                .onMessage(StartProduction.class, this::onStartProduction)
//                .onMessage(RegisterWorkers.class, this::onRegisterWorkers)
//                .onMessage(WorkerAvailable.class, this::onWorkerAvailable)
//                .build();
//    }
//
//    private Behavior<Command> onRegisterWorkers(RegisterWorkers msg) {
//        workers.addAll(msg.workers);
//        getContext().getLog().info("Registered {} workers", workers.size());
//        return this;
//    }
//
//    private Behavior<Command> onStartProduction(StartProduction msg) {
//        Optional<ActorRef<Worker.Command>> worker = getAvailableWorker();
//        if (worker.isPresent()) {
//            busyWorkers.add(worker.get());
//            getContext().getLog().info("Starting production for {} on line {}", msg.orderId, msg.lineNumber);
//            worker.get().tell(new Worker.BuildCarBody(
//                    msg.orderId,
//                    getContext().getSelf(),
//                    localStorage,
//                    msg.lineNumber
//            ));
//        } else {
//            getContext().getLog().warn("No available workers for order {}", msg.orderId);
//            // Retry after delay
//            getContext().getSelf().tell(msg);
//        }
//        return this;
//    }
//
//    private Optional<ActorRef<Worker.Command>> getAvailableWorker() {
//        List<ActorRef<Worker.Command>> available = new ArrayList<>(workers);
//        available.removeAll(busyWorkers);
//        if (available.isEmpty()) return Optional.empty();
//        Collections.shuffle(available);
//        return Optional.of(available.get(0));
//    }
//
//    private Behavior<Command> onWorkerAvailable(WorkerAvailable msg) {
//        busyWorkers.remove(msg.worker);
//        getContext().getLog().info("Worker available on line {}", msg.lineNumber);
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
import java.util.concurrent.ThreadLocalRandom;

public class ProductionLine extends AbstractBehavior<ProductionLine.Command> {

    public interface Command {}

    public static final class IsAvailable implements Command {
        public final akka.actor.typed.ActorRef<OrderBook.Command> replyTo;

        public IsAvailable(akka.actor.typed.ActorRef<OrderBook.Command> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class StartProduction implements Command {
        public final int orderNumber;

        public StartProduction(int orderNumber) {
            this.orderNumber = orderNumber;
        }
    }

    public static final class BodyBuilt implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<Worker.Command> worker;

        public BodyBuilt(int orderNumber, akka.actor.typed.ActorRef<Worker.Command> worker) {
            this.orderNumber = orderNumber;
            this.worker = worker;
        }
    }

    public static final class SpecialRequestsInstalled implements Command {
        public final int orderNumber;
        public final akka.actor.typed.ActorRef<Worker.Command> worker;

        public SpecialRequestsInstalled(int orderNumber, akka.actor.typed.ActorRef<Worker.Command> worker) {
            this.orderNumber = orderNumber;
            this.worker = worker;
        }
    }

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

    private Behavior<Command> onIsAvailable(IsAvailable msg) {
        if (isAvailable) {
            msg.replyTo.tell(new OrderBook.ProductionLineAvailable(getContext().getSelf()));
        }
        return this;
    }

    private Behavior<Command> onStartProduction(StartProduction msg) {
        if (isAvailable) {
            isAvailable = false;
            getContext().getLog().info("Starting production for order {}", msg.orderNumber);

            // Assign a random worker
            int workerIndex = ThreadLocalRandom.current().nextInt(workers.length);
            var worker = workers[workerIndex];

            // Schedule body building
            int buildTime = ThreadLocalRandom.current().nextInt(5, 11);
            getContext().scheduleOnce(
                    Duration.ofSeconds(buildTime),
                    getContext().getSelf(),
                    new BodyBuilt(msg.orderNumber, worker)
            );
        }
        return this;
    }

    private Behavior<Command> onBodyBuilt(BodyBuilt msg) {
        getContext().getLog().info("Order {}: Body built by {}, now installing special requests",
                msg.orderNumber, msg.worker.path().name());

        msg.worker.tell(new Worker.InstallSpecialRequests(msg.orderNumber, getContext().getSelf()));
        return this;
    }

    private Behavior<Command> onSpecialRequestsInstalled(SpecialRequestsInstalled msg) {
        getContext().getLog().info("Order {} completed by {}, production line now available",
                msg.orderNumber, msg.worker.path().name());

        isAvailable = true;
        return this;
    }
}