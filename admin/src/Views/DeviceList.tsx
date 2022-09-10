import {useEffect, useState} from "react";

interface DeviceMetadata {
    id : string
    name : string
    online : boolean
    ssid : string
    lastMessage : Number
    ip : string
    platform : string
    firmwareName : string
    firmwareVersion : string
}
interface DeviceListProps {

}
interface DeviceListState {
   devices : DeviceMetadata []
}
export function DeviceList(props : DeviceListProps) {
   const [state, setState] = useState<DeviceListState>()

    useEffect(() => {
       fetch("http://localhost:80/devices")
           .then(res => res.json())
           .then(res => setState({...state, devices: res}),
               error => {})
    }, [])

    return <div className={"deviceList"}>
        {state?.devices.map(d => {
           return <div className={"device-row"}>
              <div className={"device-name"} >
                  <span>{d.name}</span>
                  <span>{d.platform}</span>
              </div>
               <div className={"device-details"}>
                   <div className={"firmware-details"}>
                       <span>{d.firmwareName}</span>
                       <span>{d.firmwareVersion}</span>
                   </div>

               </div>
           </div>
        })}
    </div>
}