package org.hobbit.benchmark.platform;

import java.util.Random;

public class StringGenerator {

    private final String baseString;
    private final int baseStringLength;
    private final Random random;

    public StringGenerator(String baseString, long seed) {
        this.baseString = baseString;
        baseStringLength = baseString.length();
        this.random = new Random(seed);
    }

    public String generateString(int length) {
        StringBuilder builder = new StringBuilder(length);
        int start = random.nextInt(baseStringLength);
        int end;
        while (length > 0) {
            end = Math.min(start + length, baseStringLength);
            builder.append(baseString.substring(start, end));
            length -= end - start;
            start = 0;
        }
        return builder.toString();
    }
}
