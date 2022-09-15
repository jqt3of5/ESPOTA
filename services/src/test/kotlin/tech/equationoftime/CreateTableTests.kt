package tech.equationoftime

import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select
import org.ktorm.entity.Entity
import org.ktorm.schema.*
import tech.equationoftime.tables.DeviceTable.bindTo
import tech.equationoftime.tables.DeviceTable.primaryKey
import tech.equationoftime.tables.DeviceTable.references
import tech.equationoftime.tables.FirmwareEntity
import tech.equationoftime.tables.FirmwareTable
import tech.equationoftime.tables.createTable
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateTableTests {

    interface TestEntity: Entity<TestEntity> {
        companion object : Entity.Factory<TestEntity>()
        val id : Int
        var columnInt : Int
        var columnString : String
        var columnBoolean: Boolean
    }

    object TestTable: Table<TestEntity>("t_test") {
        val id = int("id").primaryKey()
        var columnInt = int("columnInt").bindTo { it.columnInt }
        var columnString = varchar("columnString").bindTo { it.columnString }
        var columnBoolean = boolean("columnBoolean").bindTo { it.columnBoolean }
    }

    @Test
    fun testCreateTable() {
        val database = Database.connect("jdbc:sqlite::memory:")
        database.createTable(TestTable)
        database.insert(TestTable) {
            set(it.columnBoolean, true)
            set(it.columnInt, 10)
            set(it.columnString, "string")
        }

        val rows = database.from(TestTable).select()
        assertEquals(1,  rows.totalRecords)
    }
}