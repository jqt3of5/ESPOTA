import {FirmwareMetadata} from "../Views/FirmwaresList";
import '../App.css';

interface FirmwareProps {
    firmware : FirmwareMetadata
}

export function ItemRow(props : FirmwareProps) {

    return <div className={"itemRow"}>
       <div className={"metadata"}>
           <div className={"title-container"}>
               <span className={"title"}>{props.firmware.name}</span> <span className={"subtitle"}>{props.firmware.version}</span>
           </div>
           <div className={"platform"}>{props.firmware.platform}</div>
       </div>
        <div className={"description"}>{props.firmware.description}</div>
    </div>
}
