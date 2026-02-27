package com.fishingmacro.feature;

import java.util.regex.Pattern;

public class ChatSeaCreatureDetector {
    private static final Pattern PLAYER_CHAT_PATTERN = Pattern.compile(
        "^(?:(?:Party|Guild|Co-op|Officer|Friend) > )?(?:From |To )?(?:\\[\\S+] )?\\w{3,16}: "
    );
    private static final String[] SEA_CREATURE_PATTERNS = {
        "appeared",
        "You caught a",
        "reveals a",
        "You stumbled upon",
        "disrupted the",
        "You reeled in",
        "has emerged",
        "Catfish",
        "Carrot King",
        "Agarimoo",
        "Sea Leech",
        "Guardian Defender",
        "Deep Sea Protector",
        "Water Hydra",
        "has come to test",
        "You have awoken",
        "from the depths",
        "Reindrake",
        "Yeti",
        "Great White Shark",
        "Phantom Fisher",
        "Grim Reaper",
        "Scarecrow",
        "Werewolf",
        "Sea Emperor",
        "Abyssal Miner",
        "Thunder",
        "Lord Jawbus",
        "Plhlegblast",
        // Bayou
        "Alligator",
        "Banshee",
        "Titanoboa",
        "Bayou Sludge",
        "Dumpster Diver",
        "Trash Gobbler",
        // Water Hotspot
        "Frog Man",
        "Snapping Turtle",
        "Blue Ringed Octopus",
        "Wiki Tiki"
    };

    private static final long ALERT_EXPIRY_MS = 5000;

    private boolean alertFired = false;
    private long alertTimestamp = 0;

    private boolean isPlayerChat(String message) {
        return PLAYER_CHAT_PATTERN.matcher(message).find();
    }

    public void onChatMessage(String message) {
        if (isPlayerChat(message)) return;
        for (String pattern : SEA_CREATURE_PATTERNS) {
            if (message.contains(pattern)) {
                alertFired = true;
                alertTimestamp = System.currentTimeMillis();
                return;
            }
        }
    }

    public boolean hasSeaCreatureAlert() {
        if (!alertFired) return false;
        if (System.currentTimeMillis() - alertTimestamp > ALERT_EXPIRY_MS) {
            alertFired = false;
            return false;
        }
        return true;
    }

    public void reset() {
        alertFired = false;
        alertTimestamp = 0;
    }
}
