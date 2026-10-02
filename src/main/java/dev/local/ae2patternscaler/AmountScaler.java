package dev.local.ae2patternscaler;

public final class AmountScaler {
    public enum Direction {
        MULTIPLY,
        DIVIDE
    }

    public enum Failure {
        NONE,
        LIMIT_EXCEEDED,
        NOT_EXACT,
        WOULD_BECOME_ZERO
    }

    public record Result(long amount, Failure failure) {
        public boolean successful() {
            return failure == Failure.NONE;
        }
    }

    private AmountScaler() {
    }

    public static Result scale(long amount, long maxAmount, Direction direction, int factor) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (maxAmount <= 0) {
            throw new IllegalArgumentException("maxAmount must be positive");
        }
        if (factor <= 1) {
            throw new IllegalArgumentException("factor must be greater than one");
        }

        if (direction == Direction.MULTIPLY) {
            // Avoid overflow before multiplying.
            if (amount > maxAmount / factor) {
                return new Result(0, Failure.LIMIT_EXCEEDED);
            }
            return new Result(amount * factor, Failure.NONE);
        }

        if (amount % factor != 0) {
            return new Result(0, Failure.NOT_EXACT);
        }

        long result = amount / factor;
        if (result == 0) {
            return new Result(0, Failure.WOULD_BECOME_ZERO);
        }
        if (result > maxAmount) {
            return new Result(0, Failure.LIMIT_EXCEEDED);
        }
        return new Result(result, Failure.NONE);
    }
}
