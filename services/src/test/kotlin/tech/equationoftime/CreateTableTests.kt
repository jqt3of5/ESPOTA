package tech.equationoftime

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.update
import org.ktorm.schema.*
import tech.equationoftime.tables.createTable
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateTableTests {

    interface TestEntity: Entity<TestEntity> {
        companion object : Entity.Factory<TestEntity>()
        val id : Int
        var columnInt : Int
        var columnString : String
        var columnBoolean: Boolean
    }

    object TestTable: Table<TestEntity>("t_test") {
        var id = int("id").primaryKey().bindTo { it.id }
        var columnInt = int("columnInt").bindTo { it.columnInt }
        var columnString = varchar("columnString").bindTo { it.columnString }
        var columnBoolean = boolean("columnBoolean").bindTo { it.columnBoolean }
    }

    @Test
    fun testCreateTable() {
        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        val database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(TestTable)
        database.insert(TestTable) {
            set(it.columnBoolean, true)
            set(it.columnInt, 10)
            set(it.columnString, "string")
        }

        val rows = database.from(TestTable).select()
        assertEquals(1,  rows.totalRecords)

        keepAlive.close()
    }

    @Test
    fun testInsertUpdate() {
        val keepAlive = DriverManager.getConnection("jdbc:sqlite:file:test?mode=memory&cache=shared")
        val database = Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared")
        database.createTable(TestTable)

        database.insert(TestTable) {
            set(it.columnBoolean, true)
            set(it.columnInt, 10)
            set(it.columnString, "string")
        }

        val item = database.sequenceOf(TestTable).find { it.columnInt eq 10 }
        assertNotNull(item)
        assertNotNull(item.id)

        database.sequenceOf(TestTable).update(
            item.apply {
                columnString = "Update"
            }
        )

        val item2 = database.sequenceOf(TestTable).find { it.columnInt eq 10 }
        assertNotNull(item2)
        assertNotNull(item2.id)
        assertEquals(item2.columnString, "Update")

        keepAlive.close()
    }
}