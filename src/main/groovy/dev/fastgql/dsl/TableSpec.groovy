package dev.fastgql.dsl

class TableSpec {

    final Map<String, RoleSpec> roles = new HashMap<>()

    def role(String role, Closure cl) {
        def roleSpec = new RoleSpec()
        def code = cl.rehydrate(roleSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        roles.put(role, roleSpec)
    }

    @Override
    String toString() {
        "TableSpec<roles: ${roles.toString()}>"
    }
}
