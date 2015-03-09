package dsv.pis.gotag.util;

import java.util.Random;

/**
 * @author andrew, Innometrics
 */
public class Sleeper {
    private static Random generator = new Random();

    public static void sleep(int min, int max) {
        int plus = generator.nextInt(max - min);
        sleep(min + plus);
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
