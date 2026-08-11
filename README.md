# ProxyAutoEnd

A lightweight **Velocity 4.1+** plugin that automatically shuts down the proxy after **24 hours of uptime**, but **only** when the proxy has been completely empty (0 players) for at least **1 continuous minute**.

This is useful for self-hosted or resource-constrained setups where you want the proxy process to restart periodically (via systemd, Pterodactyl, Docker restart policies, etc.) without interrupting active players.

## Behavior

| Condition | Action |
|---------|--------|
| Uptime < 24 hours | Never shuts down |
| Any players online | Never shuts down |
| Uptime \u2265 24 h **and** empty for \u2265 1 minute | Runs `end` |

The 1-minute empty grace period prevents a brief disconnect/reconnect from triggering a restart right at the 24-hour mark.

## Requirements

- Velocity **4.1.0+** (requires **Java 25**)
- No other dependencies

## Building

```bash
./gradlew build
```

The jar will be produced at `build/libs/ProxyAutoEnd-1.1.0.jar`.

## Installation

1. Drop the jar into your Velocity `plugins/` folder.
2. Restart (or start) the proxy.
3. The plugin logs on startup:  
   `ProxyAutoEnd loaded. Will shut down after 24h uptime once empty for \u22651 minute.`

When the conditions are met you will see a log line similar to:

```
Proxy has been up for 24 hours and empty for 62 seconds. Executing 'end'...
```

The proxy then cleanly shuts down via the built-in `end` command.

## Notes

- Make sure your process manager (systemd, Pterodactyl egg, Docker, screen/tmux wrapper, etc.) is configured to restart the proxy after it exits.
- The plugin is intentionally minimal and has no configuration file. The thresholds are currently hard-coded (24 h / 1 min).
