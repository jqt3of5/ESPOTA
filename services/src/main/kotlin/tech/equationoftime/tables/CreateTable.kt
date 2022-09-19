package tech.equationoftime.tables

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.schema.Table

fun <E : Entity<E>>Database.createTable(table : Table<E>) {

    val tableSchema = buildString {
        append("CREATE TABLE IF NOT EXISTS ${table.tableName} ( ")

        for (column in table.columns)
        {
            if (table.primaryKeys.contains(column)) {
                append("${column.name} ${if (column.sqlType.typeName.lowercase() == "int") "INTEGER" else column.sqlType.typeName} PRIMARY KEY")
            }
            else {
                append("${column.name} ${column.sqlType.typeName}")
            }

            if (column != table.columns.last())
            {
                append(", ")
            }
        }
        append(" );")
    }

    this.useConnection {
        it.prepareStatement(tableSchema).use {
            it.execute()
        }
        it.close()
    }
}
