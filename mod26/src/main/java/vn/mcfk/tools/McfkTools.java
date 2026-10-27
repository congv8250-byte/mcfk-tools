package vn.mcfk.tools;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class McfkTools implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.load();
        Breaker.init();
        Water.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> Cmds.register(dispatcher));
    }
}
