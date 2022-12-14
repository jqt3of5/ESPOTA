package tech.equationoftime.tables

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

val Database.devices get() = this.sequenceOf(DeviceTable)

interface DeviceMetadataEntity : Entity<DeviceMetadataEntity> {
    companion object : Entity.Factory<DeviceMetadataEntity>()
    var id : Int
    var deviceId : String
    var name : String
    var online : Boolean
    var ssid : String
    var lastMessage : Long
    var ip : String
    var platform : String
    var firmware: FirmwareVersionEntity
}

object DeviceTable : Table<DeviceMetadataEntity>("t_device") {
    val id = int("id").primaryKey().bindTo { it.id }
    var deviceId =varchar("deviceId").bindTo { it.deviceId}
    var name = varchar("name").bindTo { it.name }
    var online = boolean("online").bindTo { it.online }
    var ssid = varchar("ssid").bindTo { it.ssid }
    var lastMessage = long("lastMessage").bindTo { it.lastMessage }
    var ip = varchar("ip").bindTo { it.ip }
    var platform = varchar("platform").bindTo { it.platform }
    var firmwareId = int("firmwareId").references(FirmwareVersionTable) { it.firmware }
}