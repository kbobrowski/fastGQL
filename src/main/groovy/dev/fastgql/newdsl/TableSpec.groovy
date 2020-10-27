package dev.fastgql.newdsl

class TableSpec {

    final String name
    SelectSpec selectSpec
    InsertSpec insertSpec
    InsertSpec updateSpec
    DeleteSpec deleteSpec

    TableSpec(String name) {
        this.name = name
    }

    def select(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=SelectSpec) Closure cl) {
        def opSpec = new SelectSpec()
        def code = cl.rehydrate(opSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        selectSpec = opSpec
    }

    def insert(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=InsertSpec) Closure cl) {
        def opSpec = new InsertSpec()
        def code = cl.rehydrate(opSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        insertSpec = opSpec
    }

    def update(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=InsertSpec) Closure cl) {
        def opSpec = new InsertSpec()
        def code = cl.rehydrate(opSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        updateSpec = opSpec
    }

    def delete(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=DeleteSpec) Closure cl) {
        def opSpec = new DeleteSpec()
        def code = cl.rehydrate(opSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        deleteSpec = opSpec
    }
}
