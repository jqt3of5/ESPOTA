package tech.equationoftime.models

@kotlinx.serialization.Serializable
data class FirmwareVersionMetadataDTO(val id : String,
                                      val name: String,
                                      val version : String,
                                      val platform: String,
                                      val description : String)

@kotlinx.serialization.Serializable
data class FirmwareFamilyMetadataDTO(val name: String)
