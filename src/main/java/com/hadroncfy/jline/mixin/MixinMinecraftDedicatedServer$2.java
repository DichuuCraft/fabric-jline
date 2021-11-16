package com.hadroncfy.jline.mixin;

import com.hadroncfy.jline.interfaces.IDedicatedServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;

@Mixin(targets = "net.minecraft.server.dedicated.MinecraftDedicatedServer$2")
public abstract class MixinMinecraftDedicatedServer$2 {
    private static final Logger LOGGER = LogManager.getLogger();

    private boolean stopped = false;

    @Shadow @Final MinecraftDedicatedServer field_13822;

    private void readLine(LineReader reader){
        try {
            String s = reader.readLine(">");
            if (s == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
            } else {
                s = s.trim();
                if (s.length() > 0){
                    field_13822.enqueueCommand(s, field_13822.getCommandSource());
                }
            }
        } catch(UserInterruptException e){
            field_13822.stop(false);
        } catch(EndOfFileException e){
            // ignore
        }
    }

    // @ref
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/nms-patches/MinecraftServer.patch
    // @ref
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/nms-patches/DedicatedServer.patch
    @Inject(method = "run()V", at = @At("HEAD"))
    private void run(CallbackInfo ci) {
        LineReader reader = ((IDedicatedServer)field_13822).getReader();

        if (reader != null){
            while (!field_13822.isStopped() && field_13822.isRunning()) {
                try {
                    this.readLine(reader);
                } catch(Exception e){
                    LOGGER.error("Exception handling console input", e);
                }
            }
            stopped = true;
        } else {
            LOGGER.warn("Line reader not initialized, falling back to default console handler");
        }
    }

    @Redirect(method = "run()V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/dedicated/MinecraftDedicatedServer;isStopped()Z"
    ))
    private boolean cancelRunLoop(MinecraftDedicatedServer cela){
        return stopped || field_13822.isStopped();
    }
}