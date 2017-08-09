package org.hobbit.benchmark.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StringGeneratorTest {

    private static final String BASE_STRING = "ABCDEFGHIJabcdefghij";

    @Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testConfigs = new ArrayList<Object[]>();
        testConfigs.add(new Object[] { 3, 100 });
        testConfigs.add(new Object[] { BASE_STRING.length(), 100 });
        testConfigs.add(new Object[] { 100, 100 });
        return testConfigs;
    }

    private int length;
    private int count;

    public StringGeneratorTest(int length, int count) {
        this.length = length;
        this.count = count;
    }

    @Test
    public void test() {
        StringGenerator generator = new StringGenerator(BASE_STRING, System.currentTimeMillis());
        String generatedString;
        for (int i = 0; i < count; ++i) {
            generatedString = generator.generateString(length);
            Assert.assertNotNull(generatedString);
            // All strings should have the expected length
            Assert.assertEquals(length, generatedString.length());
        }
    }
}
