package tech.equationoftime.tables

import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.*

interface IDeviceRepo {
    val devices : List<DeviceMetadataEntity>
    val firmware : List<FirmwareMetadataEntity>

    fun getDevice(deviceId : String) : DeviceMetadataEntity?
    fun getFamily(familyName: String) : FirmwareMetadataEntity?
    fun getFirmware(familyName:String, version: String, platform: String) : FirmwareVersionEntity?
    fun getFirmware(firmwareId :String) : FirmwareVersionEntity?

    fun getFirmwareVersions(firmwareName : String) : List<FirmwareVersionEntity>?

    fun addOrUpdateDevice(entity : DeviceMetadataEntity)
    fun addOrUpdateFirmwareVersion(entity : FirmwareVersionEntity)
    fun addOrUpdateFirmwareFamily(entity : FirmwareMetadataEntity)

    fun deleteDevice(deviceId : String)
    fun deleteFirmware(familyName : String, version: String, platform : String)
    fun deleteFirmwareFamily(familyName : String)
}

class DeviceRepo (val database : Database) : IDeviceRepo{

    init {
        database.createTable(FirmwareVersionTable)
        database.createTable(FirmwareMetadataTable)
        database.createTable(DeviceTable)
    }

    override val devices: List<DeviceMetadataEntity>
        get() = database.sequenceOf(DeviceTable).toList()
    override val firmware: List<FirmwareMetadataEntity>
        get() = database.sequenceOf(FirmwareMetadataTable).toList()

    override fun getDevice(deviceId : String) : DeviceMetadataEntity? {
       return database.sequenceOf(DeviceTable).find { it.deviceId eq deviceId }
    }

    override fun getFamily(familyName: String): FirmwareMetadataEntity? {
        return database.sequenceOf(FirmwareMetadataTable).find {
            it.name eq familyName
        }
    }

    override fun getFirmware(familyName: String, version: String, platform: String): FirmwareVersionEntity? {
        return database.sequenceOf(FirmwareMetadataTable).find {
            it.name eq familyName
        }?.let {m ->
            database.sequenceOf(FirmwareVersionTable).find {
                it.familyId eq m.id and (it.version eq version) and (it.platform eq platform)
            }
        }
    }

    override fun getFirmware(firmwareId: String): FirmwareVersionEntity? {
        return database.sequenceOf(FirmwareVersionTable).find {
            it.id eq firmwareId.toInt()
        }
    }

    override fun getFirmwareVersions(firmwareName: String): List<FirmwareVersionEntity>? {
        val familyEntity = database
            .sequenceOf(FirmwareMetadataTable)
            .find {
            it.name eq firmwareName
        } ?: return null

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

    override fun addOrUpdateFirmwareFamily(entity: FirmwareMetadataEntity) {
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

    override fun addOrUpdateFirmwareVersion(entity: FirmwareVersionEntity) {
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