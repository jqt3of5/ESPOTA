package tech.equationoftime.tables

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

val Database.firmwares get() = this.sequenceOf(FirmwareVersionTable)
val Database.families get() = this.sequenceOf(FirmwareMetadataTable)

interface FirmwareEntity : Entity<FirmwareEntity> {
    companion object : Entity.Factory<FirmwareEntity>()
    val id : Int
    var firmwareId : String
    var family : FirmwareFamilyEntity
    var version : String
    var platform: String
    var description : String
}

object FirmwareVersionTable : Table<FirmwareEntity>("t_firmware") {
    val id = int("id").primaryKey().bindTo { it.id }
    val firmwareId = varchar("firmware_id").bindTo { it.firmwareId }
    val familyId = int("family_id").references(FirmwareMetadataTable){ it.family}
    val version = varchar ("version").bindTo { it.version }
    val platform = varchar("plaform").bindTo { it.platform }
    val description = varchar("description").bindTo { it.description }
}

interface FirmwareFamilyEntity : Entity<FirmwareFamilyEntity> {
    companion object : Entity.Factory<FirmwareFamilyEntity>()
    var name : String
    val id : Int
}

object FirmwareMetadataTable : Table<FirmwareFamilyEntity>("t_firmware_metadata") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}