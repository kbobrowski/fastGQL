
permissions {
    table ('test') {
        role ('default') {
            ops ([insert, select, delete]) {
                allow 'id', 'name', 'another', 'modified'
                check 'id' is { eq (env) -> env.id } or { eq 'anonymous' }
                check 'nested' is { eq 'nested1' or { lt 'nested2' and { gt 'nested3' } } } or { neq 'nested4' }
                preset 'another' to 'something'
                preset 'modified' to (env) -> System.currentTimeMillis()
            }
        }
    }
}
