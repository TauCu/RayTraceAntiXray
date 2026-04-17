package com.vanillage.raytraceantixray.tasks;

import com.google.common.base.Stopwatch;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.util.TimeFormatter;

import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class RayTraceTimerTask extends TimerTask {

    private final RayTraceAntiXray plugin;
    private final Stopwatch watch = Stopwatch.createUnstarted();
    private long timerRuns;
    private Instant lastNotify = Instant.MIN;

    public RayTraceTimerTask(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            if (plugin.isTimingsEnabled()) {
                watch.start();
            } else if (watch.isRunning()) {
                watch.reset();
                timerRuns = 0;
                lastNotify = Instant.MIN;
            }

            plugin.getExecutorService().invokeAll(plugin.getPlayerData().values().stream().map(pd -> pd.getCallable()).toList());

            if (watch.isRunning()) {
                watch.stop();
                timerRuns++;
                long nanoTime = watch.elapsed(TimeUnit.NANOSECONDS);
                String formatted = TimeFormatter.STANDARD.format(TimeUnit.NANOSECONDS, nanoTime / timerRuns, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS);
                // print every second
                if (lastNotify.isBefore(Instant.now())) {
                    plugin.getLogger().info(formatted + " avg per raytrace tick.");
                    lastNotify = Instant.now().plusSeconds(1);
                }
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (RejectedExecutionException e) {

        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Error thrown while raytracing: ", t);
        }
    }
}
