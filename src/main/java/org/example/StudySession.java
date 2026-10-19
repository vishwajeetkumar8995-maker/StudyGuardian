package org.example;

public class StudySession {

    private final String name;
    private final String subject;
    private final int totalMinutes;
    private final int breakEveryMinutes; // 0 = koi break nahi
    private final int breakMinutes;
    private final long startTime;

    private int alertCount = 0;
    private int breaksTaken = 0;
    private int pausesTaken = 0;
    private long distractedMillis = 0;

    private boolean paused = false;
    private long pauseStart = 0;
    private long pausedMillis = 0;

    public StudySession(String name, String subject, int totalMinutes, int breakEveryMinutes, int breakMinutes) {
        this.name = name;
        this.subject = subject;
        this.totalMinutes = totalMinutes;
        this.breakEveryMinutes = breakEveryMinutes;
        this.breakMinutes = breakMinutes;
        this.startTime = System.currentTimeMillis();
    }

    private boolean breaksEnabled() {
        return breakEveryMinutes > 0 && breakMinutes > 0;
    }

    private long totalMillis() {
        return totalMinutes * 60_000L;
    }

    // Pause ke time ye value ruki rahegi
    public long elapsedMillis() {
        long reference = paused ? pauseStart : System.currentTimeMillis();
        return reference - startTime - pausedMillis;
    }

    public long remainingMillis() {
        return Math.max(0, totalMillis() - elapsedMillis());
    }

    public boolean isFinished() {
        return elapsedMillis() >= totalMillis();
    }

    public boolean isOnBreak() {
        if (!breaksEnabled()) return false;
        long every = breakEveryMinutes * 60_000L;
        long cycle = every + breakMinutes * 60_000L;
        return elapsedMillis() % cycle >= every;
    }

    public long studiedMillis() {
        long elapsed = Math.min(elapsedMillis(), totalMillis());
        if (!breaksEnabled()) return elapsed;
        long every = breakEveryMinutes * 60_000L;
        long cycle = every + breakMinutes * 60_000L;
        return (elapsed / cycle) * every + Math.min(elapsed % cycle, every);
    }

    public void pause() {
        if (!paused) {
            paused = true;
            pauseStart = System.currentTimeMillis();
            pausesTaken++;
        }
    }

    public void resume() {
        if (paused) {
            pausedMillis += System.currentTimeMillis() - pauseStart;
            paused = false;
        }
    }

    public boolean isPaused() { return paused; }

    public void addAlert() { alertCount++; }
    public void breakStarted() { breaksTaken++; }
    public void addDistracted(long millis) { distractedMillis += millis; }

    public String getName() { return name; }
    public String getSubject() { return subject; }
    public int getTotalMinutes() { return totalMinutes; }
    public int getAlertCount() { return alertCount; }
    public int getBreaksTaken() { return breaksTaken; }
    public int getPausesTaken() { return pausesTaken; }
    public long getDistractedSeconds() { return distractedMillis / 1000; }

    public long getPausedSeconds() {
        long total = pausedMillis + (paused ? System.currentTimeMillis() - pauseStart : 0);
        return total / 1000;
    }

    public static String format(long millis) {
        long totalSec = millis / 1000;
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }
}