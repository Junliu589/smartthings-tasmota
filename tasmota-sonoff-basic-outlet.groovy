/**
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Author: Jun Liu
 *
 *  Date:09/02/2019
 */
 
 
metadata {
    definition (name: "Tasmota Sonoff Basic Wifi Outlet", namespace: "LJ", author: "LJ", vid:"tasmota-sonoff-basic-switch") {
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Momentary"
    }

    attribute "deviceStatus", "enum", ["offline", "on", "off"]
    
    tiles (scale: 2){      
        multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.deviceStatus", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"momentary.push", backgroundColor:"#00a0dc", icon: "st.switches.switch.on"
                attributeState "off", label:'${name}', action:"momentary.push", backgroundColor:"#ffffff", icon: "st.switches.switch.off"
                attributeState "offline", label:'${name}', backgroundColor:"#e86d13", icon: "st.switches.switch.off"
            }
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("On", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Turn On", action:"switch.on", icon:"st.switches.switch.on"
        }
        
        standardTile("Off", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Turn Off", action:"switch.off", icon:"st.switches.switch.off"
        }
    }  

    main(["switch"])
    details(["switch","refresh","On","Off"])
    
    preferences {
        input name: "ipAddr", type: "text", title: "IP Address", description: "IP Address of the device", required: true,displayDuringSetup: true
        input name: "port", type: "number", title: "Port", description: "Port of the device",  defaultValue: 80 ,displayDuringSetup: true
        input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: true, displayDuringSetup: true
        input name: "password", type: "password", title: "Password", description: "Username to manage the device", required: true, displayDuringSetup: true
    }

}

def installed() {
    initialize()
    runEvery1Minute(checkDevice)
}

def updated() {
    initialize()
}

def initialize() {
    state.responseReceived = true;
}

def convertIPtoHex(ipAddress) { 
    ipAddress.tokenize( '.' ).collect {String.format( '%02X', it.toInteger())}.join()
}

def convertPortToHex(port) {
    port.toString().format( '%04X', port.toInteger() )
}

def tasmotaHttpCmd(cmd){
    def hosthex = convertIPtoHex(ipAddr)
    def porthex = convertPortToHex(port)
    
    device.deviceNetworkId = "$hosthex:$porthex" 
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cm?user=${username}&password=${password}&cmnd=${cmd}",
        headers: [
            HOST: "${ipAddr}:${port}"
        ]
    )
    
    sendHubCommand(hubAction)
}

def parse(description) {
    def msg = parseLanMessage(description)
    def jsonStr = msg?.json
    log.debug "RECEIVING: $jsonStr"
    log.debug "Description: $description"
    state.responseReceived = true;
    
    if ((jsonStr?.POWER in ["ON", 1, "1"]) || (jsonStr?.Status?.Power in [1, "1"])) {
        sendEvent(name: "deviceStatus", value: "on")
        createEvent(name: "switch", value: "on")
    }
    else if ((jsonStr?.POWER in ["OFF", 0, "0"]) || (jsonStr?.Status?.Power in [0, "0"])) {
        sendEvent(name: "deviceStatus", value: "off")
        createEvent(name: "switch", value: "off")
    }
    else {
        log.error "Unknown message: $msg"
    }

}

def on() {
    log.debug "ON"
    tasmotaHttpCmd("Power%20On")
}

def off() {
    log.debug "OFF"
    tasmotaHttpCmd("Power%20Off")
}

def refresh() {
    log.debug "REFRESH"
    tasmotaHttpCmd("Power")
}

def push() {
    log.debug "PUSH"
    tasmotaHttpCmd("Power%20Toggle")
} 

def checkDevice() {
    log.debug "CHECK"
    
    if (!state.responseReceived)
    {
        //No response recevied from the last check command - it's offline
        sendEvent(name: "deviceStatus", value: "offline")
    }
    
    state.responseReceived = false;
    tasmotaHttpCmd("Power")
}

