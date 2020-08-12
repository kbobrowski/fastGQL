package dev.fastgql.dsl

class RoleSpec {

    enum OpType {
        select,
        insert,
        delete
    }

    final OpType insert = OpType.insert
    final OpType select = OpType.select
    final OpType delete = OpType.delete

    final Map<List<OpType>, OpSpec> opSpecs = new HashMap<>()

    def ops(List<OpType> ops, Closure cl) {
        def opSpec = new OpSpec()
        def code = cl.rehydrate(opSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        opSpecs.put(ops, opSpec)
    }

    @Override
    String toString() {
        "RoleSpec<opSpecs: ${opSpecs}"
    }
}
