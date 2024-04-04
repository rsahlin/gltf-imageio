package org.ktximageio.ktx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.ktximageio.ktx.HalfFloatImageBuffer.FP16Convert;

public class HalfFloatTest extends org.ktximageio.ktx.Test {

    @Test
    public void testPositiveFloatToHalfFloat() {

        FP16Convert convert = new FP16Convert(new short[1]);
        // Start testing with an exponent that will give delta of 1 / 1023, where 1023 takes 10 bits
        int exponent = -4;
        float min = 0.0f;
        while (exponent < 31) {
            float max = (float) (Math.pow(2, exponent));
            float precision = max / 1023;
            validate(convert, min, max, precision);
            exponent++;
            min = max;
        }
    }

    @Test
    public void testNegativeFloatToHalfFloat() {

        FP16Convert convert = new FP16Convert(new short[1]);
        // Start testing with an exponent that will give delta of 1 / 1023, where 1023 takes 10 bits
        int exponent = -4;
        float min = 0.0f;
        while (exponent < 31) {
            float max = (float) (Math.pow(2, exponent));
            float precision = max / 1023;
            validateNegative(convert, min, max, precision);
            exponent++;
            min = max;
        }
    }

    private void validate(FP16Convert convert, float min, float max, float precision) {
        for (float i = min; i < max; i += (precision / 1000)) {
            convert.convert(i);
            convert.reset();
            if (convert.source[0] >= FP16Convert.MAX_VALUE) {
                assertTrue(convert.halfFloat[0] == FP16Convert.MAX_VALUE);
            } else {
                assertTrue(Math.abs(convert.halfFloat[0] - convert.source[0]) <= (precision));
            }
        }
        System.out.println("Validated from " + min + ", to " + max + " with precision " + (precision));
    }

    private void validateNegative(FP16Convert convert, float min, float max, float precision) {
        for (float i = min; i < max; i += (precision / 1000)) {
            convert.convert(-i);
            convert.reset();
            if (convert.source[0] <= -FP16Convert.MAX_VALUE) {
                assertTrue(convert.halfFloat[0] == -FP16Convert.MAX_VALUE);
            } else {
                assertTrue(Math.abs(Math.abs(convert.halfFloat[0]) - Math.abs(convert.source[0])) <= (precision));
            }
        }
        System.out.println("Validated from " + -min + ", to " + -max + " with precision " + (precision));
    }

}
