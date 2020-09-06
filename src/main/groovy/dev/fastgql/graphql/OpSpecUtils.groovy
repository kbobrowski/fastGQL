package dev.fastgql.graphql

import dev.fastgql.dsl.OpSpec

class OpSpecUtils {
    static void checkColumnIsEqValue(OpSpec opSpec, String columnName, Object value) {
        opSpec.check(columnName).is { eq value }
    }
}
