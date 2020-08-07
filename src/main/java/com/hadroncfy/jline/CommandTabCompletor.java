package com.hadroncfy.jline;

import java.util.List;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import jline.console.completer.Completer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public class CommandTabCompletor implements Completer {
    private final MinecraftServer server;
    public CommandTabCompletor(MinecraftServer server){
        this.server = server;
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        StringReader stringReader = new StringReader(buffer);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }

        ParseResults<ServerCommandSource> parseResults = server.getCommandManager().getDispatcher().parse(stringReader, server.getCommandSource());
        Suggestions ret = server.getCommandManager().getDispatcher().getCompletionSuggestions(parseResults).join();
        for (Suggestion s: ret.getList()){
            candidates.add(s.getText());
        }
        return ret.getRange().getStart();
    }
    
}