package dev.fastgql.dsl

abstract class RolesPermissionsConfig extends Script {

    def permissions(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PermissionsSpec) Closure cl) {
        def permissionsSpec = new PermissionsSpec()
        def code = cl.rehydrate(permissionsSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        permissionsSpec
    }
}
