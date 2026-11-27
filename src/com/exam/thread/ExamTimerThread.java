package com.exam.thread;


public class ExamTimerThread implements Runnable {
    private final String sessionId;
    private final int durationSeconds;
    private int secondsRemaining;
    private boolean submitted = false;
    private boolean expired = false;

    public ExamTimerThread(String sessionId, int durationMinutes) {
        this.sessionId = sessionId;
        this.durationSeconds = durationMinutes * 60;
        this.secondsRemaining = this.durationSeconds;
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (this) {
                    if (submitted || secondsRemaining <= 0) {
                        if (!submitted && secondsRemaining <= 0) {
                            expired = true;
                        }
                        this.notifyAll();
                        break;
                    }
                    secondsRemaining--;
                }
                Thread.sleep(1000); 
            }
        } catch (InterruptedException e) {
            // Normal exit when student submits early
            Thread.currentThread().interrupt();
        }
    }

   
    public synchronized int markSubmitted() {
        if (submitted) {
            return -1;
        }
        submitted = true;
        this.notifyAll();
        return durationSeconds - secondsRemaining;
    }

    public synchronized boolean isExpired() {
        return expired || secondsRemaining <= 0;
    }

    public synchronized boolean isSubmitted() {
        return submitted;
    }

    public synchronized int getSecondsRemaining() {
        return secondsRemaining;
    }

    public String getSessionId() {
        return sessionId;
    }
}
