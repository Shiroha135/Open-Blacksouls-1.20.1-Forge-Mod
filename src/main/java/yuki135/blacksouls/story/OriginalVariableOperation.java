package yuki135.blacksouls.story;

public final class OriginalVariableOperation {
    private OriginalVariableOperation() {
    }

    public static long apply(long before, int operation, long operand) {
        return switch (operation) {
            case 0 -> operand;
            case 1 -> Math.addExact(before, operand);
            case 2 -> Math.subtractExact(before, operand);
            case 3 -> Math.multiplyExact(before, operand);
            case 4 -> {
                if (before == Long.MIN_VALUE && operand == -1) throw new ArithmeticException("original_variable_division_overflow");
                yield operand == 0 ? 0 : Math.floorDiv(before, operand);
            }
            case 5 -> operand == 0 ? 0 : Math.floorMod(before, operand);
            default -> throw new UnsupportedOperationException("variable_operation:" + operation);
        };
    }
}
