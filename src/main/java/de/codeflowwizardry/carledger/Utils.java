package de.codeflowwizardry.carledger;

public class Utils {
    public static boolean atLeastTwoNotNull(Object a, Object b, Object c) {
        int count = 0;
        if (a != null) count++;
        if (b != null) count++;
        if (c != null) count++;
        return count >= 2;
    }
}
