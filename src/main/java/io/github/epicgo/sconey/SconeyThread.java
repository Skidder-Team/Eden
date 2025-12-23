package io.github.epicgo.sconey;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rip.diamond.practice.util.Util;

public class SconeyThread extends Thread {

    private final SconeyHandler sconeyHandler;
    private volatile boolean running = true;

    public SconeyThread(final SconeyHandler sconeyHandler) {
        super("Board - Thread tick");
        this.sconeyHandler = sconeyHandler;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                this.tick();
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                // Thread was interrupted, exit gracefully
                Thread.currentThread().interrupt(); // Restore interrupt status
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Cleanup logic if needed
        System.out.println("[Sconey] Scoreboard thread stopped gracefully.");
    }

    /**
     * Safe method to stop the thread
     */
    public void stopThread() {
        this.running = false;
        this.interrupt();
    }

    /**
     * Tick logic for thread.
     */
    private void tick() {
        for (final Player player : Util.getOnlinePlayers()) {
            try {
                final SconeyPlayer sconeyPlayer = this.sconeyHandler.getScoreboard(player);
                if (sconeyPlayer == null) continue; // Changed from return to continue

                sconeyPlayer.handleUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}