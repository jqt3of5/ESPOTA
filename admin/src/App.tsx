import React, {useEffect, useState} from 'react';
import './App.css';
import {FirmwareList} from "./Views/FirmwaresList";
import {DeviceList} from "./Views/DeviceList";
import {BrowserRouter, Link, Route, Routes} from "react-router-dom";

function App() {

  return (
  <BrowserRouter>
    <div className="App">
        <nav className={"sidebar"}>
            <ul>
                <li>
                    <Link to={"/firmwares"}>Firmwares</Link>
                </li>
                <li>
                    <Link to={"/devices"}>Devices</Link>
                </li>
            </ul>
        </nav>

        {/*<div className={"content"}>*/}
            <Routes>
                <Route path={"/firmwares"} element={<FirmwareList/>}>
                </Route>
                <Route path={"devices"} element={<DeviceList/>}>
                </Route>
            </Routes>
        {/*</div>*/}
    </div>
  </BrowserRouter>
  );
}

export default App;
