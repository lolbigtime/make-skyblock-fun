package com.fishingmacro.feature;

public class ChatSeaCreatureDetector {
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
        "Plhlegblast"
    };

    private static final long ALERT_EXPIRY_MS = 5000;

    private boolean alertFired = false;
    private long alertTimestamp = 0;

    public void onChatMessage(String message) {
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
