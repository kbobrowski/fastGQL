package dev.fastgql.dsl

abstract class RolesPermissionsConfig extends Script {

    def permissions(Closure cl) {
        def permissionsSpec = new PermissionsSpec()
        def code = cl.rehydrate(permissionsSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        permissionsSpec
    }
}
