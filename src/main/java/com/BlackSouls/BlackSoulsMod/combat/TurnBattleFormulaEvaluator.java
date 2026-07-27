package com.BlackSouls.BlackSoulsMod.combat;

import java.util.ArrayList;
import java.util.List;

public final class TurnBattleFormulaEvaluator {
    private TurnBattleFormulaEvaluator() {
    }

    public static double evaluate(String formula, Context context) {
        if (formula == null || formula.isBlank()) {
            return 0.0D;
        }
        String normalized = formula
                .replaceAll("\\[([^,\\]]+),([^\\]]+)\\]\\.min", "min($1,$2)")
                .replaceAll("b\\.state\\?\\([^)]*\\)\\?\\s*[-+0-9.]+\\s*:\\s*[-+0-9.]+", "1");
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
                          double targetMana, double targetMaxMana) {
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
            double value = expression();
            skipSpaces();
            if (position != input.length()) {
                throw new IllegalArgumentException(input.substring(position));
            }
            return value;
        }

        private double expression() {
            double value = term();
            while (true) {
                skipSpaces();
                if (take('+')) {
                    value += term();
                } else if (take('-')) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        private double term() {
            double value = unary();
            while (true) {
                skipSpaces();
                if (take('*')) {
                    value *= unary();
                } else if (take('/')) {
                    double divisor = unary();
                    value = Math.abs(divisor) < 1.0E-9D ? 0.0D : value / divisor;
                } else if (take('%')) {
                    double divisor = unary();
                    value = Math.abs(divisor) < 1.0E-9D ? 0.0D : value % divisor;
                } else {
                    return value;
                }
            }
        }

        private double unary() {
            skipSpaces();
            if (take('+')) {
                return unary();
            }
            if (take('-')) {
                return -unary();
            }
            return primary();
        }

        private double primary() {
            skipSpaces();
            if (take('(')) {
                double value = expression();
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
                List<Double> arguments = new ArrayList<>();
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

        private double number() {
            int start = position;
            while (position < input.length()) {
                char character = input.charAt(position);
                if (!Character.isDigit(character) && character != '.') {
                    break;
                }
                position++;
            }
            return Double.parseDouble(input.substring(start, position));
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

        private double variable(String name) {
            return switch (name) {
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
            };
        }

        private double function(String name, List<Double> arguments) {
            if ("min".equals(name) && arguments.size() == 2) {
                return Math.min(arguments.get(0), arguments.get(1));
            }
            if ("max".equals(name) && arguments.size() == 2) {
                return Math.max(arguments.get(0), arguments.get(1));
            }
            if ("abs".equals(name) && arguments.size() == 1) {
                return Math.abs(arguments.get(0));
            }
            return 0.0D;
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
