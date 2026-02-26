package com.fishingmacro.macro;

public enum MacroState {
    IDLE("Idle"),
    CASTING("Casting rod"),
    WAITING_FOR_BITE("Waiting for bite"),
    REELING("Reeling in"),
    RE_CASTING("Re-casting"),
    SEA_CREATURE_DETECTED("Sea creature detected"),
    SWAPPING_TO_WEAPON("Swapping to weapon"),
    KILLING("Fighting"),
    SWAPPING_TO_ROD("Swapping to rod"),
    RETURNING_TO_SPOT("Returning to spot"),
    RESUMING("Resuming");

    private final String displayName;

    MacroState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
