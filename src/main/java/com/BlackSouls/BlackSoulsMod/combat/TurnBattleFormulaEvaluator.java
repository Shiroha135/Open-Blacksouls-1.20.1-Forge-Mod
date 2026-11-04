package com.BlackSouls.BlackSoulsMod.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

public final class TurnBattleFormulaEvaluator {
    private static final Pattern TARGET_STATE_CONDITION = Pattern.compile(
            "b\\.state\\?\\((\\d+)\\)\\s*\\?\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*:\\s*([-+]?\\d+(?:\\.\\d+)?)");
    private TurnBattleFormulaEvaluator() {
    }

    public static double evaluate(String formula, Context context) {
        if (formula == null || formula.isBlank()) {
            return 0.0D;
        }
        String normalized = TARGET_STATE_CONDITION.matcher(formula).replaceAll(match ->
                context.targetHasState().test(Integer.parseInt(match.group(1)))
                        ? match.group(2) : match.group(3));
        normalized = normalized.replaceAll("\\[([^,\\]]+),([^\\]]+)\\]\\.min", "min($1,$2)");
        try {
            double value = new Parser(normalized, context).parse();
            return Double.isFinite(value) ? value : 0.0D;
        } catch (RuntimeException ignored) {
            return 0.0D;
        }
    }

    public record Context(double attackerAttack, double attackerDefense, double attackerMagicAttack,
                          double attackerMagicDefense, double attackerAgility, double attackerLuck,
                          double attackerHealth, double attackerMaxHealth, double attackerMana,
                          double attackerMaxMana, double targetAttack, double targetDefense,
                          double targetMagicAttack, double targetMagicDefense, double targetAgility,
                          double targetLuck, double targetHealth, double targetMaxHealth,
                          double targetMana, double targetMaxMana, IntPredicate targetHasState) {
    }

    private record Value(double number, boolean integer) {
        private static Value attribute(double number) {
            return new Value(number, Double.isFinite(number) && number == Math.rint(number));
        }

        private Value add(Value other) {
            return new Value(number + other.number, integer && other.integer);
        }

        private Value subtract(Value other) {
            return new Value(number - other.number, integer && other.integer);
        }

        private Value multiply(Value other) {
            return new Value(number * other.number, integer && other.integer);
        }

        private Value divide(Value other) {
            boolean integerResult = integer && other.integer;
            if (integerResult && other.number == 0.0D) {
                throw new ArithmeticException("divided by 0");
            }
            double result = number / other.number;
            return new Value(integerResult ? Math.floor(result) : result, integerResult);
        }

        private Value modulo(Value other) {
            if (other.number == 0.0D) {
                throw new ArithmeticException("divided by 0");
            }
            double result = number % other.number;
            if (result != 0.0D && (result < 0.0D) != (other.number < 0.0D)) {
                result += other.number;
            }
            return new Value(result, integer && other.integer);
        }
    }

    private static final class Parser {
        private final String input;
        private final Context context;
        private int position;

        private Parser(String input, Context context) {
            this.input = input;
            this.context = context;
        }

        private double parse() {
            Value value = expression();
            skipSpaces();
            if (position != input.length()) {
                throw new IllegalArgumentException(input.substring(position));
            }
            return value.number;
        }

        private Value expression() {
            Value value = term();
            while (true) {
                skipSpaces();
                if (take('+')) {
                    value = value.add(term());
                } else if (take('-')) {
                    value = value.subtract(term());
                } else {
                    return value;
                }
            }
        }

        private Value term() {
            Value value = unary();
            while (true) {
                skipSpaces();
                if (take('*')) {
                    value = value.multiply(unary());
                } else if (take('/')) {
                    value = value.divide(unary());
                } else if (take('%')) {
                    value = value.modulo(unary());
                } else {
                    return value;
                }
            }
        }

        private Value unary() {
            skipSpaces();
            if (take('+')) {
                return unary();
            }
            if (take('-')) {
                Value value = unary();
                return new Value(-value.number, value.integer);
            }
            return primary();
        }

        private Value primary() {
            skipSpaces();
            if (take('(')) {
                Value value = expression();
                expect(')');
                return value;
            }
            if (position < input.length()
                    && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.')) {
                return number();
            }
            String identifier = identifier();
            skipSpaces();
            if (take('(')) {
                List<Value> arguments = new ArrayList<>();
                skipSpaces();
                if (!take(')')) {
                    do {
                        arguments.add(expression());
                        skipSpaces();
                    } while (take(','));
                    expect(')');
                }
                return function(identifier, arguments);
            }
            return variable(identifier);
        }

        private Value number() {
            int start = position;
            while (position < input.length()) {
                char character = input.charAt(position);
                if (!Character.isDigit(character) && character != '.') {
                    break;
                }
                position++;
            }
            String literal = input.substring(start, position);
            return new Value(Double.parseDouble(literal), literal.indexOf('.') < 0);
        }

        private String identifier() {
            int start = position;
            while (position < input.length()) {
                char character = input.charAt(position);
                if (!Character.isLetterOrDigit(character) && character != '.' && character != '_') {
                    break;
                }
                position++;
            }
            if (start == position) {
                throw new IllegalArgumentException(input.substring(position));
            }
            return input.substring(start, position);
        }

        private Value variable(String name) {
            return Value.attribute(switch (name) {
                case "a.atk" -> context.attackerAttack;
                case "a.def" -> context.attackerDefense;
                case "a.mat" -> context.attackerMagicAttack;
                case "a.mdf" -> context.attackerMagicDefense;
                case "a.agi" -> context.attackerAgility;
                case "a.luk" -> context.attackerLuck;
                case "a.hp" -> context.attackerHealth;
                case "a.mhp" -> context.attackerMaxHealth;
                case "a.mp" -> context.attackerMana;
                case "a.mmp" -> context.attackerMaxMana;
                case "b.atk" -> context.targetAttack;
                case "b.def" -> context.targetDefense;
                case "b.mat" -> context.targetMagicAttack;
                case "b.mdf" -> context.targetMagicDefense;
                case "b.agi" -> context.targetAgility;
                case "b.luk" -> context.targetLuck;
                case "b.hp" -> context.targetHealth;
                case "b.mhp" -> context.targetMaxHealth;
                case "b.mp" -> context.targetMana;
                case "b.mmp" -> context.targetMaxMana;
                default -> 0.0D;
            });
        }

        private Value function(String name, List<Value> arguments) {
            if ("min".equals(name) && arguments.size() == 2) {
                return arguments.get(0).number <= arguments.get(1).number
                        ? arguments.get(0) : arguments.get(1);
            }
            if ("max".equals(name) && arguments.size() == 2) {
                return arguments.get(0).number >= arguments.get(1).number
                        ? arguments.get(0) : arguments.get(1);
            }
            if ("abs".equals(name) && arguments.size() == 1) {
                Value value = arguments.get(0);
                return new Value(Math.abs(value.number), value.integer);
            }
            return new Value(0.0D, true);
        }

        private void skipSpaces() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean take(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            skipSpaces();
            if (!take(expected)) {
                throw new IllegalArgumentException(input.substring(position));
            }
        }
    }
}
