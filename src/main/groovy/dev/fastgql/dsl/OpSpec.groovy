package dev.fastgql.dsl

import java.util.function.Function

class OpSpec {

    enum RelationalOperator {
        eq,
        neq,
        lt,
        gt,
        lte,
        gte,
    }

    enum LogicalConnective {
        and,
        or
    }

    class Condition {
        final RelationalOperator relationalOperator
        final String value
        final Function<Map<String, String>, String> envFunction
        LogicalConnective logicalConnective
        final List<Condition> next

        Condition(RelationalOperator relationalOperator, String value) {
            this.relationalOperator = relationalOperator
            this.value = value
            this.envFunction = null
            this.next = new ArrayList<>()
        }

        Condition(RelationalOperator relationalOperator, Function<Map<String, String>, String> envFunction) {
            this.relationalOperator = relationalOperator
            this.value = null
            this.envFunction = envFunction
            this.next = new ArrayList<>()
        }

        private def logicalConnectiveMethod(LogicalConnective logicalConnective, Closure cl) {
            def conditionSpec = new ConditionSpec()
            def code = cl.rehydrate(conditionSpec, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            def condition = code() as Condition
            condition.logicalConnective = logicalConnective
            this.next.add(condition)
            return this
        }

        def and(Closure cl) {
            return logicalConnectiveMethod(LogicalConnective.and, cl)
        }

        def or(Closure cl) {
            return logicalConnectiveMethod(LogicalConnective.or, cl)
        }

        @Override
        String toString() {
            "Condition<operator: ${relationalOperator}, value: ${value}, function: ${envFunction != null}, connective: ${logicalConnective}, next: ${next}"
        }
    }

    class ConditionSpec {

        def methodMissing(String name, Object args) {
            def operator = name as RelationalOperator
            def argList = args as List<Object>
            def arg = argList.get(0)
            if (arg instanceof String) {
                return new Condition(operator, arg as String)
            } else if (arg instanceof Closure) {
                return new Condition(operator, arg as Function<Map<String, String>, String>)
            }
            throw new MissingMethodException(name, ConditionSpec.class, args)
        }

    }

    class Check {

        final String column
        Condition condition

        Check(String column) {
            this.column = column
        }

        def is(Closure cl) {
            def conditionSpec = new ConditionSpec()
            def code = cl.rehydrate(conditionSpec, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            def condition = code() as Condition
            this.condition = condition
        }

        @Override
        String toString() {
            "Check<column: ${column}, condition: ${condition}"
        }
    }

    class Preset {
        final String column
        String value
        Function<Map<String, String>, String> envFunction

        Preset(String column) {
            this.column = column
        }

        def to(String value) {
            this.value = value
        }

        def to(Function<Map<String, String>, String> envFunction) {
            this.envFunction = envFunction
        }

        @Override
        String toString() {
            "Preset<column: ${column}, value: ${value}, function: ${envFunction != null}>"
        }
    }

    final List<Check> checks = new ArrayList<>()
    final List<Preset> presets = new ArrayList<>()
    final List<String> allowed = new ArrayList<>()

    def check(String column) {
        def check = new Check(column)
        this.checks.add(check)
        return check
    }

    def allow(String... columns) {
        this.allowed.addAll(columns)
    }

    def preset(String column) {
        def preset = new Preset(column)
        this.presets.add(preset)
        return preset
    }

    @Override
    String toString() {
        "OpSpec<allowed: ${allowed}, checks: ${checks}, presets: ${presets}>"
    }
}
