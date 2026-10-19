package org.example;

public class DistractionTracker {

    private final long thresholdMillis;
    private long badSince = -1; // kab se lagataar "bad" chal raha hai

    public DistractionTracker(int thresholdSeconds) {
        this.thresholdMillis = thresholdSeconds * 1000L;
    }

    public boolean update(boolean isBad) {
        long now = System.currentTimeMillis();
        if (isBad) {
            if (badSince == -1) badSince = now;
            return now - badSince >= thresholdMillis;
        } else {
            badSince = -1;
            return false;
        }
    }

    public void reset() {
        badSince = -1;
    }
}