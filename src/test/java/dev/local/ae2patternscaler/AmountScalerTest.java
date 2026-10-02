package dev.local.ae2patternscaler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AmountScalerTest {
    @Test
    void multipliesWithinTheNormalLimit() {
        var result = AmountScaler.scale(15, 999_999, AmountScaler.Direction.MULTIPLY, 64);

        assertTrue(result.successful());
        assertEquals(960, result.amount());
    }

    @Test
    void rejectsMultiplicationAtTheLimitWithoutOverflowing() {
        var ordinaryLimit = AmountScaler.scale(999_999, 999_999, AmountScaler.Direction.MULTIPLY, 2);
        var longLimit = AmountScaler.scale(Long.MAX_VALUE, Long.MAX_VALUE, AmountScaler.Direction.MULTIPLY, 2);

        assertEquals(AmountScaler.Failure.LIMIT_EXCEEDED, ordinaryLimit.failure());
        assertEquals(AmountScaler.Failure.LIMIT_EXCEEDED, longLimit.failure());
    }

    @Test
    void preservesMillibucketPrecision() {
        var result = AmountScaler.scale(200, 999_999_000, AmountScaler.Direction.DIVIDE, 8);

        assertTrue(result.successful());
        assertEquals(25, result.amount());
    }

    @Test
    void supportsTheLargestDivisionButton() {
        var result = AmountScaler.scale(4_096, 999_999, AmountScaler.Direction.DIVIDE, 64);

        assertTrue(result.successful());
        assertEquals(64, result.amount());
    }

    @Test
    void rejectsFractionalMillibucketsOrItems() {
        var result = AmountScaler.scale(25, 999_999_000, AmountScaler.Direction.DIVIDE, 2);

        assertEquals(AmountScaler.Failure.NOT_EXACT, result.failure());
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> AmountScaler.scale(0, 10, AmountScaler.Direction.DIVIDE, 2));
        assertThrows(IllegalArgumentException.class,
                () -> AmountScaler.scale(1, 0, AmountScaler.Direction.MULTIPLY, 2));
        assertThrows(IllegalArgumentException.class,
                () -> AmountScaler.scale(1, 10, AmountScaler.Direction.MULTIPLY, 1));
    }
}
