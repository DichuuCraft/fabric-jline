package com.hadroncfy.jline.mixin;

import com.hadroncfy.jline.CommandTabCompletor;
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
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.UserCache;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;

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
    private Terminal terminal;
    private LineReader reader;

    @Inject(method = "setupServer", at = @At("HEAD"))
    private void initLogging(CallbackInfoReturnable<Void> ci){
        if (!"jline.UnsupportedTerminal".equals(System.getProperty("jline.terminal"))){
            try {
                terminal = TerminalBuilder.builder()
                    .encoding(StandardCharsets.UTF_8).build();
                reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new CommandTabCompletor(this)).build();
            } catch(IOException e){
                LOGGER.warn("Exception creating terminal", e);
            }
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

        new TerminalOutputThread(System.out, terminal, reader).start();
    }

    @Override
    public LineReader getReader() {
        return reader;
    }
}