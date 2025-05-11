package org.example;

import akka.actor.typed.ActorSystem;
import java.io.IOException;

public class AkkaStart {
    public static void main(String[] args) {
        final ActorSystem<AkkaMainSystem.Create> system =
            ActorSystem.create(AkkaMainSystem.create(), "carFactorySystem");

        system.tell(new AkkaMainSystem.Create());

        try {
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
        } catch (IOException ignored) {
        } finally {
            system.terminate();
        }
    }
}