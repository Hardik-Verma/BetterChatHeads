package com.pheonix.betterchatheads.mixininterface;

import com.pheonix.betterchatheads.BetterChatHeadsClient;

public interface ParsedChatLineHolder {
    void betterchatheads$setParsedChatLine(BetterChatHeadsClient.ParsedChatLine parsedChatLine);

    BetterChatHeadsClient.ParsedChatLine betterchatheads$getParsedChatLine();

    void betterchatheads$setLineIndexWithinEntry(int lineIndex);

    int betterchatheads$getLineIndexWithinEntry();
}
