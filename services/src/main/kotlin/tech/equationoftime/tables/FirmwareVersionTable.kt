package tech.equationoftime.tables

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

val Database.firmwares get() = this.sequenceOf(FirmwareVersionTable)
val Database.families get() = this.sequenceOf(FirmwareMetadataTable)

interface FirmwareVersionEntity : Entity<FirmwareVersionEntity> {
    companion object : Entity.Factory<FirmwareVersionEntity>()
    var id : Int
    var firmwareId : String
    var family : FirmwareMetadataEntity
    var version : String
    var platform: String
    var description : String
}

object FirmwareVersionTable : Table<FirmwareVersionEntity>("t_firmware") {
    val id = int("id").primaryKey().bindTo { it.id }
    val familyId = int("family_id").references(FirmwareMetadataTable){ it.family}
    val version = varchar ("version").bindTo { it.version }
    val platform = varchar("plaform").bindTo { it.platform }
    val description = varchar("description").bindTo { it.description }
}

interface FirmwareMetadataEntity : Entity<FirmwareMetadataEntity> {
    companion object : Entity.Factory<FirmwareMetadataEntity>()
    var id : Int
    var name : String
}

object FirmwareMetadataTable : Table<FirmwareMetadataEntity>("t_firmware_metadata") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}