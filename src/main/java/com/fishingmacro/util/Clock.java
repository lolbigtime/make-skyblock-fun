package com.fishingmacro.util;

public class Clock {
    private long endTime;
    private long remainingTime;
    private boolean scheduled;
    private boolean paused;

    public void schedule(long milliseconds) {
        this.endTime = System.currentTimeMillis() + milliseconds;
        this.remainingTime = milliseconds;
        this.scheduled = true;
        this.paused = false;
    }

    public boolean passed() {
        if (!scheduled) return true;
        if (paused) return false;
        return System.currentTimeMillis() >= endTime;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public long getRemainingTime() {
        if (paused) return remainingTime;
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public void pause() {
        if (scheduled && !paused) {
            remainingTime = endTime - System.currentTimeMillis();
            paused = true;
        }
    }

    public void resume() {
        if (scheduled && paused) {
            endTime = System.currentTimeMillis() + remainingTime;
            paused = false;
        }
    }

    public void reset() {
        scheduled = false;
        paused = false;
        endTime = 0;
        remainingTime = 0;
    }
}
