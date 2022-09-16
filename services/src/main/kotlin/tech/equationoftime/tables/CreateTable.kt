package tech.equationoftime.tables

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.schema.Table

fun <E : Entity<E>>Database.createTable(table : Table<E>) {

    val tableSchema = buildString {
        append("CREATE TABLE ${table.tableName} ( ")

        for (column in table.columns)
        {
           append("${column.name} ${column.sqlType.typeName}, ")
        }

        append("PRIMARY KEY (")
        for (column in table.primaryKeys)
        {
            append("${column.name}")
        }
        append(") );")
    }

    this.useConnection {
        it.prepareStatement(tableSchema).use {
            it.execute()
        }
        it.close()
    }
}
