package com.hadroncfy.jline.mixin;

import com.hadroncfy.jline.CommandTabCompletor;
import com.hadroncfy.jline.Mod;
import com.hadroncfy.jline.TerminalOutputThread;
import com.hadroncfy.jline.interfaces.IDedicatedServer;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.util.QueueLogAppender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import jline.console.ConsoleReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.UserCache;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;

@Mixin(MinecraftDedicatedServer.class)
public abstract class MixinMinecraftDedicatedServer extends MinecraftServer implements IDedicatedServer {
    public MixinMinecraftDedicatedServer(File gameDir, Proxy proxy, DataFixer dataFixer, CommandManager commandManager,
            YggdrasilAuthenticationService authService, MinecraftSessionService sessionService,
            GameProfileRepository gameProfileRepository, UserCache userCache,
            WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory, String levelName) {
        super(gameDir, proxy, dataFixer, commandManager, authService, sessionService, gameProfileRepository, userCache,
                worldGenerationProgressListenerFactory, levelName);
    }

    private static Logger LOGGER = LogManager.getLogger();
    private ConsoleReader reader;

    @Inject(method = "setupServer", at = @At("HEAD"))
    private void initLogging(CallbackInfoReturnable<Void> ci){
        Mod.useJline = true;

        if (System.console() == null && System.getProperty("jline.terminal") == null) {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            Mod.useJline = false;
        }

        try {
            reader = new ConsoleReader(System.in, System.out);
            reader.setExpandEvents(false);
        } catch (Throwable e) {
            try {
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                Mod.useJline = false;
                reader = new ConsoleReader(System.in, System.out);
                reader.setExpandEvents(false);
            } catch (IOException e2) {
                LOGGER.info("Cannot initialize jline", e2);
            }
        }

        if (reader != null){
            reader.addCompleter(new CommandTabCompletor(this));
        }

        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
        for (Appender ap: logger.getAppenders().values()){
            if (ap instanceof ConsoleAppender){
                logger.removeAppender(ap);
            }
        }
        PatternLayout layout = PatternLayout.newBuilder().withPattern("[%d{HH:mm:ss}] [%t/%level]: %msg%n").build();
        Appender consoleAppender = QueueLogAppender.createAppender(TerminalOutputThread.EVENT_NAME, "false", layout, null, null);
        consoleAppender.start();
        logger.addAppender(consoleAppender);

        new TerminalOutputThread(System.out, reader).start();
    }

    @Override
    public ConsoleReader getReader() {
        return reader;
    }
}