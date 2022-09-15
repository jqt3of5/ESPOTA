package tech.equationoftime.models

@kotlinx.serialization.Serializable
data class FirmwareMetadataDTO(val id : String,
                               val name: String,
                               val version : String,
                               val platform: String,
                               val description : String)
