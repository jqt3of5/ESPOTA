package tech.equationoftime.tables

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.select
import org.ktorm.entity.*
import tech.equationoftime.models.DeviceMetadataDTO
import java.time.Instant

interface IDeviceRepo {
    val devices : List<DeviceMetadataEntity>
    val firmware : List<FirmwareFamilyEntity>

    fun getDevice(deviceId : String) : DeviceMetadataEntity?
    fun getFirmwareVersions(firmwareName : String) : List<FirmwareEntity>

    fun addOrUpdateDevice(entity : DeviceMetadataEntity)
    fun addOrUpdateFirmwareVersion(entity : FirmwareEntity)
    fun addOrUpdateFirmwareFamily(entity : FirmwareFamilyEntity)

    fun deleteDevice(deviceId : String)
    fun deleteFirmware(familyName : String, version: String, platform : String)
    fun deleteFirmwareFamily(familyName : String)
}

class DeviceRepo (val database : Database) : IDeviceRepo{

    override val devices: List<DeviceMetadataEntity>
        get() = database.sequenceOf(DeviceTable).toList()
    override val firmware: List<FirmwareFamilyEntity>
        get() = database.sequenceOf(FirmwareMetadataTable).toList()

    override fun getDevice(deviceId : String) : DeviceMetadataEntity? {
       return database.sequenceOf(DeviceTable).find { it.deviceId eq deviceId }
    }

    override fun getFirmwareVersions(firmwareName: String): List<FirmwareEntity> {
        val familyEntity = database
            .sequenceOf(FirmwareMetadataTable)
            .find {
            it.name eq firmwareName
        } ?: return listOf<FirmwareEntity>()

       return database
           .sequenceOf(FirmwareVersionTable)
           .filter {
          it.familyId eq familyEntity.id
       }.toList()
    }

    override fun addOrUpdateDevice(entity : DeviceMetadataEntity){

        addOrUpdateFirmwareVersion(entity.firmware)

        val device = database.devices.find { it.deviceId eq entity.deviceId }
        if (device == null) {
            database.devices.add(entity)
        }
        else {
            database.devices.update(entity.apply {
                id = device.id
            })
        }
    }

    override fun addOrUpdateFirmwareFamily(entity: FirmwareFamilyEntity) {
        val familyEntity = database.families.find {  it.name eq entity.name}
        if (familyEntity == null) {
            database.families.add(entity)
        }
        else {
            database.families.update(entity.apply {
                id = familyEntity.id
            })
        }
    }

    override fun addOrUpdateFirmwareVersion(entity: FirmwareEntity) {
        addOrUpdateFirmwareFamily(entity.family)

        val firmwareEntity = database.firmwares
            .filter{ it.familyId eq entity.family.id}
            .filter { it.version eq entity.version }
            .find { it.platform eq entity.platform }

        if (firmwareEntity == null) {
            database.firmwares.add(entity)
        } else {
           database.firmwares.update(entity.apply {
               id = firmwareEntity.id
           })
        }
    }

    override fun deleteDevice(deviceId: String) {

        val device = database.devices.find { it.deviceId eq deviceId }
        if (device != null){
            database.devices.removeIf { it.id eq device.id }
        }
    }

    override fun deleteFirmware(familyName: String, version: String, platform: String) {
        val familyEntity = database.families.find { it.name eq familyName }
        if (familyEntity != null)
        {
            database.firmwares.removeIf {
                it.familyId eq familyEntity.id and (it.version eq version) and (it.platform eq platform)
            }
        }
    }

    override fun deleteFirmwareFamily(familyName: String) {

        database.families.find { it.name eq familyName }?.let {e ->
            database.firmwares.removeIf {
                it.familyId eq e.id
            }
            database.families.removeIf {
                it.name eq familyName
            }
        }
    }

}