package vn.mcfk.tools;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class McfkTools implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.load();
        Breaker.init();
        Water.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> Cmds.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(Store::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Store.save());
    }
}
