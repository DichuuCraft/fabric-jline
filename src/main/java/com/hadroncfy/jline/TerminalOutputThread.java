package com.hadroncfy.jline;
// @ref https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/util/TerminalConsoleWriterThread.java

import java.io.OutputStream;

import com.mojang.util.QueueLogAppender;

import org.apache.logging.log4j.LogManager;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

public class TerminalOutputThread extends Thread {
    public static final String EVENT_NAME = "Fabric-JLine-TerminalConsole";

    private final OutputStream output;
    private final Terminal terminal;
    private final LineReader reader;

    public TerminalOutputThread(OutputStream output, Terminal terminal, LineReader reader){
        super("Terminal output thread");
        this.output = output;
        this.terminal = terminal;
        this.reader = reader;
        setDaemon(true);
    }

    @Override
    public void run() {
        String msg;
        while (true){
            msg = QueueLogAppender.getNextLogEvent(EVENT_NAME);
            if (msg == null){
                continue;
            }
            try {
                if (terminal != null){
                    if (reader != null){
                        reader.printAbove(msg);
                    }
                    else {
                        terminal.writer().println(msg);
                    }
                }
                else {
                    output.write(msg.getBytes());
                    output.flush();
                }
            } catch(Exception e){
                LogManager.getLogger(TerminalOutputThread.class.getName()).error(e);
            }
        }
    }
}