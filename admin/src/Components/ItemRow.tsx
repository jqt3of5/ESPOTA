import {FirmwareMetadata} from "../Views/FirmwaresList";
import '../App.css';

interface FirmwareProps {
    firmware : FirmwareMetadata
}

export function ItemRow(props : FirmwareProps) {

    return <div className={"itemRow"}>
       <div className={"metadata"}>
           <div className={"name"}>{props.firmware.name} <span>{props.firmware.version}</span></div>
           <div className={"platform"}>Platform: {props.firmware.platform}</div>
       </div>
        <div className={"description"}>{props.firmware.description}</div>
    </div>
}
