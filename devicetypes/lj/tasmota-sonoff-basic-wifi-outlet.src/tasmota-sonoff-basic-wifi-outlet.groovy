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
            
            tileAttribute ("device.deviceTime", key: "SECONDARY_CONTROL") {
                attributeState "default", label:'Current Time: ${currentValue}'
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

        valueTile("wifi", "device.deviceWIFI", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}', defaultState: true
        }

        valueTile("ip", "device.ipAddr", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label:'IP: ${currentValue}', defaultState: true
        }
    }  

    main(["switch"])
    details(["switch","refresh","On","Off","wifi","ip"])
    
    preferences {
        input name: "macAddr", type: "text", title: "MAC Address", description: "MAC Address of the device", required: true,displayDuringSetup: true
        input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "password", type: "password", title: "Password", description: "Username to manage the device", required: false, displayDuringSetup: true
    }

}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    device.deviceNetworkId = macAddr.tokenize( ':' ).collect{it.toUpperCase()}.join()
    state.responseReceived = false
    state.offlineMinutes = 0

    if (device.currentValue("ipAddr"))
    {
        configDevice()
    }
    else
    {
        runIn(60, discover())
    }
    
    runEvery1Minute(checkDevice)
}

def configDevice() {
    tasmotaHttpCmd("TimeDST%200,2,3,1,2,-360")
    
    tasmotaHttpCmd("TimeSTD%200,1,11,1,2,-420")
    
    tasmotaHttpCmd("Timezone%2099")
    
    tasmotaHttpCmd("Longitude%20-104.73823299")
    
    tasmotaHttpCmd("Latitude%2038.94552741")
}

def discover() {
    for (int i=2; i<100; i++) {
    
    log.debug "Sent to 192.168.0.${i}"
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cm?user=${username}&password=${password}&cmnd=State",
        headers: [
            HOST: "192.168.0.${i}:80"
        ]
    )
    
    sendHubCommand(hubAction)
    }
}

def convertIPtoHex(ipAddress) { 
    ipAddress.tokenize( '.' ).collect {String.format( '%02X', it.toInteger())}.join()
}

def convertPortToHex(port) {
    port.toString().format( '%04X', port.toInteger() )
}

def tasmotaHttpCmd(cmd){
    def deviceIP = device.currentValue("ipAddr")
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/cm?user=${username}&password=${password}&cmnd=${cmd}",
        headers: [
            HOST: "$deviceIP:80"
        ]
    )
    
    sendHubCommand(hubAction)
}

def parse(description) {
    def msg = parseLanMessage(description)
    log.debug "Msg: $msg"
    def jsonStr = msg?.json
    
    state.responseReceived = true;
    
    if ((jsonStr?.POWER in ["ON", 1, "1"]) || (jsonStr?.Status?.Power in [1, "1"])) {
        sendEvent(name: "deviceStatus", value: "on")
        createEvent(name: "switch", value: "on")
    }
    else if ((jsonStr?.POWER in ["OFF", 0, "0"]) || (jsonStr?.Status?.Power in [0, "0"])) {
        sendEvent(name: "deviceStatus", value: "off")
        createEvent(name: "switch", value: "off")
    }

    def timestr = jsonStr?.Time
    
    if (timestr)
    {
        sendEvent(name:"deviceTime", value: "$timestr", displayed: false)
    }
    
    def wifiSsid = jsonStr?.Wifi?.SSId
    def wifiRssi = jsonStr?.Wifi?.RSSI
    
    if (wifiSsid && wifiRssi)
    {
        sendEvent(name:"deviceWIFI", value: "WIFI: $wifiSsid   RSSI: $wifiRssi", displayed: false)
    }
    
    def ipStr = msg?.ip
    
    if (ipStr)
    {
        def ip = "";
        for (int i=0; i<8; i+=2)
        {
            if (ip != "")
                ip += "."
            ip += Integer.parseInt(ipStr.substring(i, i+2), 16).toString()
        }

        log.debug "Device IP: $ip"
        sendEvent(name:"ipAddr", value: "$ip", displayed: false)
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
    tasmotaHttpCmd("State")
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
        state.offlineMinutes++
        
        //When no response >=2 mins, set the status to Offline
        if (state.offlineMinutes >= 2)
        {
            sendEvent(name: "deviceStatus", value: "offline")
        }
        
        //When no response > 60 mins, suspect the IP is changed, trying to discover again
        if (state.offlineMinutes >= 60)
        {
            discover()
            state.offlineMinutes = 0
        }
    }
    else
    {
        state.offlineMinutes = 0
    }
    
    state.responseReceived = false
    tasmotaHttpCmd("State")
}