import {ItemRow} from "../Components/ItemRow";
import {ChangeEvent, useEffect, useState} from "react";
import '../App.css';

export interface FirmwareMetadata {
    name : string
    version : string
    platform : string
    description : string
}
interface FirmwareListProps {
}
interface FirmwareListState {
    firmware: FirmwareMetadata []
    families: string []
    family: string | null
    file : File | undefined | null
    newFirmwareVersion : string
    newFirmwareName : string
    newFirmwarePlatform : string
    newFamilyName: string
}
export function FirmwareList(props : FirmwareListProps) {
    const [state, setState] = useState<FirmwareListState>
    ({
        firmware: [],
        families: [],
        family: null,
        file: null,
        newFirmwareName: "",
        newFirmwarePlatform: "ESP",
        newFirmwareVersion: "",
        newFamilyName : ""
    })

    useEffect(() => {
        fetch("http://localhost:80/families")
            .then(res => res.json())
            .then(res => {
                setState({...state, families: res})
            }, error => {})
    }, [])

    useEffect(() => {
        if (state.family == null)
        {
            setState({...state, firmware: []})
            return
        }
        fetch(`http://localhost:80/firmware/${state.family}`)
            .then(res => res.json())
            .then(res => {
                setState(s => {return {...s, firmware: res}})
            }, error => {})
    }, [state.family])

    function onFileChange(event: ChangeEvent<HTMLInputElement>){
        var file = event.target.files?.item(0)
        setState(s => {return {...s, file:file}})
    }

    function onFileUpload(event: React.MouseEvent<HTMLButtonElement>) {
       const formData = new FormData()
        if (state.file != null)
        {
            formData.append("filename", state.file, state.file?.name)
            fetch(`http://localhost:80/firmware/${state.newFirmwarePlatform}/${state.newFirmwareName}/${state.newFirmwareVersion}`,
                {method: 'POST', body:formData})
        }
    }
    function onFamilySelected(family : string) {
       setState({...state, family:family})
    }
    function onNewFamily(family : string){
        fetch(`http://localhost:80/families/${family}`, {method: 'POST'})
            .then(res => {
                setState(s => {return {...s, family: family}})
            }, error => {})
    }

    return <div className={"firmware-container"}>
        <div className={"family-list"}>
            {state.families.map ((f,i) =>
                <div key={i} className={"family-item"} onClick={() => onFamilySelected(f)}>
                   <span>{f}</span>
                </div>
            )}
            <div className={"new-firmware-family"}>
                <input type={"text"} value={state.newFamilyName} onChange={(e) => setState({...state, newFamilyName: e.target.value})}/>
                <button onClick={() => onNewFamily(state.newFamilyName)}>Add</button>
            </div>
        </div>
        <div className={"firmware-list"} >
            {state.firmware.map((f,i) =>
                <div className={"itemRow"}>
                    <div className={"metadata"}>
                        <span className={"title"}>{f.version}</span>
                        <div className={"platform"}>{f.platform}</div>
                    </div>
                    <div className={"description"}>{f.description}</div>
                </div>
            )}
            <div className={"add-new-firmware"}>
                <input type={"text"} name={"firmwareName"} placeholder={"firmware name"} value={state.newFirmwareName}
                       onChange={(e) => setState({...state, newFirmwareName: e.target.value})}/>
                <input type={"text"} name={"firmwareVersion"} placeholder={"firmware version"} value={state.newFirmwareVersion}
                       onChange={(e) => setState({...state, newFirmwareVersion: e.target.value})}/>
                <select name={"firmwarePlatform"} value={state.newFirmwarePlatform}
                        onChange={(e) => setState({...state, newFirmwarePlatform:e.target.value})}>
                    <option>ESP</option>
                </select>
                <input id="firmwareFile" type={"file"} onChange={onFileChange} />
                <button onClick={onFileUpload}>Upload</button>
            </div>
        </div>

    </div>
}