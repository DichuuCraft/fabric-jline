package com.hadroncfy.jline.mixin;

import java.io.IOException;

import com.hadroncfy.jline.Mod;
import com.hadroncfy.jline.interfaces.IDedicatedServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import jline.console.ConsoleReader;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;

@Mixin(targets = "net.minecraft.server.dedicated.MinecraftDedicatedServer$2")
public abstract class MixinMinecraftDedicatedServer$2 {
    private static final Logger LOGGER = LogManager.getLogger();

    private boolean stopped = false;

    @Shadow @Final
    MinecraftDedicatedServer field_13822;

    // @ref
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/nms-patches/MinecraftServer.patch
    // @ref
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/nms-patches/DedicatedServer.patch
    @Inject(method = "run()V", at = @At("HEAD"))
    private void run(CallbackInfo ci) {
        ConsoleReader reader = ((IDedicatedServer)field_13822).getReader();

        try {
            System.in.available();
        } catch (IOException e) {
            return;
        }

        try {
            String s;
            while (!field_13822.isStopped() && field_13822.isRunning()) {
                if (Mod.useJline) {
                    s = reader.readLine(">", null);
                } else {
                    s = reader.readLine();
                }

                if (s == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
                s = s.trim();
                if (s.length() > 0){
                    field_13822.enqueueCommand(s, field_13822.getCommandSource());
                }
            }
        } catch(IOException e){
            LOGGER.error("Exception handling console input", e);
        }

        stopped = true;
    }

    @Redirect(method = "run()V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/dedicated/MinecraftDedicatedServer;isStopped()Z"
    ))
    private boolean cancelRunLoop(MinecraftDedicatedServer cela){
        return stopped;
    }
}