import React, {useEffect, useState} from 'react';
import logo from './logo.svg';
import './App.css';
import {FirmwareList} from "./Views/FirmwaresList";

function App() {
    const [state, setState] = useState({})

  return (
    <div className="App">
        <div className={"sidebar"}>
            <header></header>
            <div className={"sidebarRow"}>Firmwares</div>
            <div className={"sidebarRow"}>Devices</div>
        </div>

        <div className={"content"}>
            <header></header>
            <FirmwareList/>
        </div>
    </div>
  );
}

export default App;
