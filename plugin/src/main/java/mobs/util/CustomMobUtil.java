package mobs.util;

/**
 * @author Silthus
 */
public class CustomMobUtil {

    public static double getMaxHealth(int level) {

        return 1.5126 * (level ^ 2) + 15.946 * level + 80;
    }
}
