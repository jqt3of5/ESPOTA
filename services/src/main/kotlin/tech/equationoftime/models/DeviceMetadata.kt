package tech.equationoftime.models

data class DeviceMetadata(val id : String, val name : String, val online : Boolean, val lastMessage : Int, val ip : String, val platform : String, val firmwareName : String, val firmwareVersion : String)
