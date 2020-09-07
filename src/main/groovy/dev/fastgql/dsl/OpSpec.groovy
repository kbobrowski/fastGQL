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
        final Object value
        LogicalConnective logicalConnective
        final List<Condition> next

        Condition(RelationalOperator relationalOperator, Object value) {
            this.relationalOperator = relationalOperator
            this.value = value
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

        def and(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ConditionSpec) Closure cl) {
            return logicalConnectiveMethod(LogicalConnective.and, cl)
        }

        def or(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ConditionSpec) Closure cl) {
            return logicalConnectiveMethod(LogicalConnective.or, cl)
        }

        @Override
        String toString() {
            "Condition<operator: ${relationalOperator}, value: ${value}, connective: ${logicalConnective}, next: ${next}>"
        }
    }

    class ConditionSpec {

        def op(RelationalOperator relationalOperator, Object arg) {
            if (arg instanceof Closure) {
                return new Condition(relationalOperator, arg as Function<Map<String, Object>, Object>)
            } else {
                return new Condition(relationalOperator, arg)
            }
        }

        def eq(Object arg) {
            return op(RelationalOperator.eq, arg)
        }

        def neq(Object arg) {
            return op(RelationalOperator.neq, arg)
        }

        def lt(Object arg) {
            return op(RelationalOperator.lt, arg)
        }

        def gt(Object arg) {
            return op(RelationalOperator.gt, arg)
        }

        def lte(Object arg) {
            return op(RelationalOperator.lte, arg)
        }

        def gte(Object arg) {
            return op(RelationalOperator.gte, arg)
        }
    }

    class Check {

        final String column
        Condition condition

        Check(String column) {
            this.column = column
        }

        def is(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=ConditionSpec) Closure cl) {
            def conditionSpec = new ConditionSpec()
            def code = cl.rehydrate(conditionSpec, this, this)
            code.resolveStrategy = Closure.DELEGATE_ONLY
            def condition = code() as Condition
            this.condition = condition
        }

        @Override
        String toString() {
            "Check<column: ${column}, condition: ${condition}>"
        }
    }

    class Preset {
        final String column
        Object value

        Preset(String column) {
            this.column = column
        }

        def to(Object value) {
            if (value instanceof Closure) {
                this.value = value as Function<Map<String, Object>, Object>
            } else {
                this.value = value
            }
        }

        @Override
        String toString() {
            "Preset<column: ${column}, value: ${value}>"
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
