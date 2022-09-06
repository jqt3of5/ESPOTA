import {ItemRow} from "../Components/ItemRow";
import {useEffect, useState} from "react";
import '../App.css';

export interface FirmwareMetadata {
    name : string
    version : string
    platform : string
    description : string
}
interface FirmwareListProps {
}
export function FirmwareList(props : FirmwareListProps) {
    const [state, setState] = useState({firmware: []})

    useEffect(() => {
        fetch("http://localhost:80/firmware")
            .then(res => res.json())
            .then(res => {
                setState(s => {return {...s, firmware: res}})
            }, error => {

            })
    }, [])

    return <div className={"firmwareList"}>
        {state.firmware.map((f,i) =>
            <ItemRow key={i} firmware={f}></ItemRow>
        )}
    </div>
}