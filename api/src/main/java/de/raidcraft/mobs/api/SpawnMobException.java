package de.raidcraft.mobs.api;

import de.raidcraft.api.RaidCraftException;

public class SpawnMobException extends RaidCraftException {
    public SpawnMobException(String message) {
        super(message);
    }

    public SpawnMobException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpawnMobException(Throwable cause) {
        super(cause);
    }

    public SpawnMobException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
