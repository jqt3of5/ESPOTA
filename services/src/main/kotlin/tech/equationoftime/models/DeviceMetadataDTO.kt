package tech.equationoftime.models

@kotlinx.serialization.Serializable
data class DeviceMetadataDTO(val id : String,
                             val name : String,
                             val online : Boolean,
                             val ssid : String,
                             val lastMessage : Long,
                             val ip : String,
                             val platform : String,
                             val firmwareName : String,
                             val firmwareVersion : String)
