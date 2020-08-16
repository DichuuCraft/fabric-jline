package com.hadroncfy.jline;

import java.util.List;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public class CommandTabCompletor implements Completer {
    private final MinecraftServer server;

    public CommandTabCompletor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        StringReader stringReader = new StringReader(line.line());
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        ParseResults<ServerCommandSource> parseResults = server.getCommandManager().getDispatcher().parse(stringReader,
                server.getCommandSource());
        Suggestions ret = server.getCommandManager().getDispatcher().getCompletionSuggestions(parseResults).join();
        for (Suggestion s : ret.getList()) {
            candidates.add(new Candidate(s.getText()));
        }
    }
    
}