package com.hadroncfy.jline;
// @ref https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/util/TerminalConsoleWriterThread.java

import java.io.OutputStream;

import com.mojang.util.QueueLogAppender;

import org.apache.logging.log4j.LogManager;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;

import jline.console.ConsoleReader;

public class TerminalOutputThread extends Thread {
    public static final String EVENT_NAME = "Fabric-JLine-TerminalConsole";

    private final OutputStream output;
    private final ConsoleReader reader;

    public TerminalOutputThread(OutputStream output, ConsoleReader reader){
        super("Terminal output thread");
        this.output = output;
        this.reader = reader;
        setDaemon(true);
    }

    @Override
    public void run() {
        String msg;
        while (true){
            msg = QueueLogAppender.getNextLogEvent(EVENT_NAME);
            try {
                if (Mod.useJline){
                    reader.print(Ansi.ansi().eraseLine(Erase.ALL).toString() + ConsoleReader.RESET_LINE);
                    reader.flush();
                    output.write(msg.getBytes());
                    output.flush();

                    try {
                        reader.drawLine();
                    } catch(Throwable e){
                        reader.getCursorBuffer().clear();
                    }
                    reader.flush();
                }
                else {
                    output.write(msg.getBytes());
                    output.flush();
                }
            } catch(Throwable e){
                LogManager.getLogger(TerminalOutputThread.class.getName()).error(e);
            }
        }
    }
}