import React, {useEffect, useState} from 'react';
import logo from './logo.svg';
import './App.css';
import {FirmwareList} from "./Views/FirmwaresList";
import {DeviceList} from "./Views/DeviceList";

function App() {
    const [state, setState] = useState({view: ""})

  return (
    <div className="App">
        <div className={"sidebar"}>
            <header></header>
            <div className={"sidebarRow"} onClick={() => setState({...state, view:"firmware"})}>Firmwares</div>
            <div className={"sidebarRow"} onClick={() => setState({...state, view:"devices"})}>Devices</div>
        </div>

        <div className={"content"}>
            <header></header>
            {state.view == "firmware" && <FirmwareList/>}
            {state.view =="devices" && <DeviceList/>}
        </div>
    </div>
  );
}

export default App;
