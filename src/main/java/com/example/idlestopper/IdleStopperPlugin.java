package com.example.idlestopper;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(
        id = "proxyautoend",
        name = "ProxyAutoEnd",
        version = "1.1.0",
        description = "Shuts down the proxy after 24h uptime once it has been empty for 1 minute",
        authors = {"Grok", "wilderop"}
)
public class IdleStopperPlugin {

    private final ProxyServer server;
    private final Logger logger;

    // When the proxy started
    private long startTimeMs;

    // When the last player left (or start time if never had players)
    // 0 means players are currently online
    private final AtomicLong lastEmptyTimeMs = new AtomicLong();

    // 24 hours
    private static final long UPTIME_THRESHOLD_MS = TimeUnit.HOURS.toMillis(24);

    // Must be empty for this long before we allow shutdown
    private static final long EMPTY_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(1);

    // How often we check (every 30 seconds is plenty)
    private static final long CHECK_INTERVAL_SECONDS = 30;

    private ScheduledTask checkTask;

    @Inject
    public IdleStopperPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        startTimeMs = System.currentTimeMillis();
        lastEmptyTimeMs.set(startTimeMs); // start empty

        logger.info("ProxyAutoEnd loaded. Will shut down after 24h uptime once empty for \u22651 minute.");

        checkTask = server.getScheduler()
                .buildTask(this, this::checkShutdownCondition)
                .repeat(Duration.ofSeconds(CHECK_INTERVAL_SECONDS))
                .schedule();
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        // Someone joined \u2192 no longer empty
        lastEmptyTimeMs.set(0); // 0 = currently has players
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Only mark as empty if this was the last player
        if (server.getPlayerCount() == 0) {
            lastEmptyTimeMs.set(System.currentTimeMillis());
            logger.debug("Proxy is now empty. Starting empty timer.");
        }
    }

    private void checkShutdownCondition() {
        long now = System.currentTimeMillis();
        long uptime = now - startTimeMs;

        // Not yet 24 hours old \u2192 do nothing
        if (uptime < UPTIME_THRESHOLD_MS) {
            return;
        }

        long emptySince = lastEmptyTimeMs.get();

        // Currently has players
        if (emptySince == 0) {
            return;
        }

        long emptyDuration = now - emptySince;

        // Has been empty long enough
        if (emptyDuration >= EMPTY_THRESHOLD_MS) {
            logger.info("Proxy has been up for {} hours and empty for {} seconds. Executing 'end'...",
                    TimeUnit.MILLISECONDS.toHours(uptime),
                    TimeUnit.MILLISECONDS.toSeconds(emptyDuration));

            // Cancel the repeating task so we don't try again
            if (checkTask != null) {
                checkTask.cancel();
            }

            // Execute Velocity's shutdown command
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(), "end");
        }
    }
}