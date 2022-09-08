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
    file : File | null
}
export function FirmwareList(props : FirmwareListProps) {
    const [state, setState] = useState<FirmwareListState>({firmware: [], file: null})

    useEffect(() => {
        fetch("http://localhost:80/firmware")
            .then(res => res.json())
            .then(res => {
                setState(s => {return {...s, firmware: res}})
            }, error => {

            })
    }, [])

    function onFileChange(event: ChangeEvent<HTMLInputElement>){
        var files = event.target.files
        if (files != null)
        {
            setState(s => {return {...s, file: files?.item(0)}})
        }
    }
    function onFileUpload(event) {

    }

    return <div className={"firmwareList"}>
        {state.firmware.map((f,i) =>
            <ItemRow key={i} firmware={f}></ItemRow>
        )}
        <div className={"add-new-firmware"}>
            <input id="firmwareFile" type={"file"} onChange={onFileChange} />
            <button onClick={onFileUpload}>Upload</button>
        </div>
    </div>
}