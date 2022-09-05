package tech.equationoftime.models

@kotlinx.serialization.Serializable
data class FirmwareMetadata(val id : String, val name: String, val version : String, val platform: String)
