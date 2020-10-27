package dev.fastgql.newdsl

class TestNewDsl {
    static void main(String[] args) {
        def permissions = new PermissionsSpec()
        permissions.role('default') {
            table('test') {
                select {
                    rowCheck([
                            id: [_eq: { it.id}],
                            address: [_lt: 5],
                            address_ref: [
                                    id: [_eq: 5]
                            ]
                    ])
                    rowLimit 5
                    allowedColumns 'id', 'address'
                }
                insert {
                    rowCheck([
                            id: [_eq: { it.id}],
                            address: [_lt: 5],
                            address_ref: [
                                    id: [_eq: 5]
                            ]
                    ])
                    columnPresets([
                            id: { it.id},
                            address: 'default'
                    ])
                    allowedColumns 'id', 'address'
                }
                update {
                    rowCheck([
                            id: [_eq: { it.id}],
                            address: [_lt: 5],
                            address_ref: [
                                    id: [_eq: 5]
                            ]
                    ])
                    columnPresets([
                            id: { it.id},
                            address: 'default'
                    ])
                    allowedColumns 'id', 'address'
                }
                delete {
                    rowCheck([
                            id: [_eq: { it.id}],
                            address: [_lt: 5],
                            address_ref: [
                                    id: [_eq: 5]
                            ]
                    ])
                }
            }
        }
    }
}


