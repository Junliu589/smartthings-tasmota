import groovy.json.*

metadata {
    definition (name: "Tasmota Sprinkler Switch", namespace: "LJ", author: "LJ", vid:"tasmota-sprinkler-switch") {
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Momentary"
    }

    attribute "deviceStatus", "enum", ["offline", "on", "off"]
    attribute "ipAddr", "string"
    
    attribute "sun", "enum", ["1", "0"]
    attribute "mon", "enum", ["1", "0"]
    attribute "tus", "enum", ["1", "0"]
    attribute "wed", "enum", ["1", "0"]
    attribute "thu", "enum", ["1", "0"]
    attribute "fri", "enum", ["1", "0"]
    attribute "sat", "enum", ["1", "0"]
    
    command "saveTimer"
    command "sunToggle"
    command "monToggle"
    command "tueToggle"
    command "wedToggle"
    command "thuToggle"
    command "friToggle"
    command "satToggle"

    tiles (scale: 2){      
        multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.deviceStatus", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"momentary.push", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
                attributeState "off", label:'${name}', action:"momentary.push", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
                attributeState "offline", label:'${name}', backgroundColor:"#e86d13", icon: "st.Outdoor.outdoor12"
            }
            
            tileAttribute ("device.deviceTime", key: "SECONDARY_CONTROL") {
                attributeState "default", label:'Current Time: ${currentValue}'
            }
        }
        
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        valueTile("sunday", "device.sun", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'SUN', action:"sunToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }
        
        valueTile("monday", "device.mon", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'MON', action:"monToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }

        valueTile("tuesday", "device.tue", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'TUE', action:"tueToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }
        
        valueTile("wednesday", "device.wed", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'WED', action:"wedToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }

        valueTile("thursday", "device.thu", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'THU', action:"thuToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }
        
        valueTile("friday", "device.fri", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'FRI', action:"friToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }
        
        valueTile("saturday", "device.sat", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        state "val", label:'SAT', action:"satToggle", defaultState: true, backgroundColors: [
            [value: 0, color: "#ffffff"],
            [value: 1, color: "#00a0dc"]
        ]
        }        
                
        valueTile("save", "", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "default", label:"SAVE", action:"saveTimer", backgroundColor:"#e86d13"
        }

        valueTile("wifi", "device.deviceWIFI", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}', defaultState: true
        }

        valueTile("ip", "device.ipAddr", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "default", label:'IP: ${currentValue}', defaultState: true
        }
    }  

    main(["switch"])
    details(["switch","refresh","monday","tuesday","wednesday","thursday","friday","saturday","sunday","save","wifi","ip"])
    
    preferences {
        input name: "macAddr", type: "text", title: "MAC Address", description: "MAC Address of the device", required: true,displayDuringSetup: true
        input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "password", type: "password", title: "Password", description: "Password to manage the device", required: false, displayDuringSetup: true
        input name: "beginTime", type: "text", title: "Watering Begin Time", description: "Watering Begin time", defaultValue: "19:50", required: true, displayDuringSetup: true
        input name: "winterMode", type: "bool", title: "Winter Mode", description: "Winter Mode - timers disabled",  required: true
    }

}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    state.responseReceived = true;
    state.offlineMinutes = 0

    if (device.currentValue("ipAddr"))
    {
        runIn(30, configDevice)
    }
    else
    {
        runIn(30, discover)
    }

    runEvery1Minute(checkDevice)
    runEvery1Hour(checkTimer)
}

def configDevice() {
    if (macAddr)
    {
        device.deviceNetworkId = macAddr.tokenize( ':' ).collect{it.toUpperCase()}.join()
    }

    def jsonstr1 = JsonOutput.toJson([Arm: 1, Mode: 0, Time: "00:00", Window: 0, Days: "1111111", Repeat: 1, Output: 1, Action: 1])
    
    log.debug "timer1 json: $jsonstr1"
    
    tasmotaHttpCmd("Timer1%20$jsonstr1")
    
    def jsonstr2 = JsonOutput.toJson([Arm: 1, Mode: 0, Time: "12:00", Window: 0, Days: "1111111", Repeat: 1, Output: 1, Action: 0])
    
    log.debug "timer2 json: $jsonstr2"
    
    tasmotaHttpCmd("Timer2%20$jsonstr2") 
    
    log.debug "wintermode: $winterMode"

    if (winterMode) 
    {
        tasmotaHttpCmd("Timers%20off")
    }
    else
    {
        tasmotaHttpCmd("Timers%20on")
    }
    
    tasmotaHttpCmd("TimeDST%200,2,3,1,2,-360")
    
    tasmotaHttpCmd("TimeSTD%200,1,11,1,2,-420")
    
    tasmotaHttpCmd("Timezone%2099")

    tasmotaHttpCmd("Longitude%20-104.73823299")
    
    tasmotaHttpCmd("Latitude%2038.94552741")
}

def discover() {
    if (macAddr)
    {
         device.deviceNetworkId = macAddr.tokenize( ':' ).collect{it.toUpperCase()}.join()
    }

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

    //Try to config the device after the discovery
    runIn(60, configDevice)
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
    log.debug "Message: $msg"
    def jsonStr = msg?.json

    state.responseReceived = true;
    
    def days = jsonStr?.Timer3?.Days
    
    if ((jsonStr?.POWER in ["ON", 1, "1"]) || (jsonStr?.Status?.Power in [1, "1"])) {
        sendEvent(name: "deviceStatus", value: "on")
        sendEvent(name: "switch", value: "on")
    }
    else if ((jsonStr?.POWER in ["OFF", 0, "0"]) || (jsonStr?.Status?.Power in [0, "0"])) {
        sendEvent(name: "deviceStatus", value: "off")
        sendEvent(name: "switch", value: "off")
    }
    else if (days) {
        log.debug "Timer3 days: $days"
        
        //Sunday
        if (days[0] in ["1", "S"]) {
            sendEvent(name: "sun", value: "1", displayed: false)
        }
        else if (days[0] in ["0", "-"]) {
            sendEvent(name: "sun", value: "0", displayed: false)
        }

        //Monday
        if (days[1] in ["1", "M"]) {
            sendEvent(name: "mon", value: "1", displayed: false)
        }
        else if (days[1] in ["0", "-"]) {
            sendEvent(name: "mon", value: "0", displayed: false)
        }

        //Tuesday
        if (days[2] in ["1", "T"]) {
            sendEvent(name: "tue", value: "1", displayed: false)
        }
        else if (days[2] in ["0", "-"]) {
            sendEvent(name: "tue", value: "0", displayed: false)
        }

        //Wednesday
        if (days[3] in ["1", "W"]) {
            sendEvent(name: "wed", value: "1", displayed: false)
        }
        else if (days[3] in ["0", "-"]) {
            sendEvent(name: "wed", value: "0", displayed: false)
        }

        //Thursday
        if (days[4] in ["1", "T"]) {
            sendEvent(name: "thu", value: "1", displayed: false)
        }
        else if (days[4] in ["0", "-"]) {
            sendEvent(name: "thu", value: "0", displayed: false)
        }
        
        //Friday
        if (days[5] in ["1", "F"]) {
            sendEvent(name: "fri", value: "1", displayed: false)
        }
        else if (days[5] in ["0", "-"]) {
            sendEvent(name: "fri", value: "0", displayed: false)
        }
        
        //Saturday
        if (days[6] in ["1", "S"]) {
            sendEvent(name: "sat", value: "1", displayed: false)
        }
        else if (days[6] in ["0", "-"]) {
            sendEvent(name: "sat", value: "0", displayed: false)
        }

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
    tasmotaHttpCmd("Timer3")
}

def push() {
    log.debug "PUSH"
    tasmotaHttpCmd("Power%20Toggle")
} 

def checkDevice() {
    log.debug "CHECK_DEVICE"
    
    if (!state.responseReceived)
    {
        //No response recevied from the last check command - it's offline
        state.offlineMinutes = state.offlineMinutes + 1
        
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

def checkTimer() {
    log.debug "CHECK_TIMER"
    tasmotaHttpCmd("Timer3")
}

def saveTimer() {
    log.debug "saveTimer"
    
    def daysValue = device.currentValue("sun") + device.currentValue("mon") + device.currentValue("tue") + \
                    device.currentValue("wed") + device.currentValue("thu") + device.currentValue("fri") + device.currentValue("sat")
    
    log.debug "days to be set: $daysValue"
    
    def jsonstr = JsonOutput.toJson([Arm: 1, Mode: 0, Time: "${beginTime}", Window: 0, Days: "$daysValue", Repeat: 1, Output: 1, Action: 1])
    
    log.debug "timer json: $jsonstr"
    
    tasmotaHttpCmd("Timer3%20$jsonstr")
}

def sunToggle() {
    log.debug "sunToggle"
    
    if (device.currentValue("sun") == "0")
    {
        sendEvent(name: "sun", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "sun", value: "0", displayed: false)
    }
}

def monToggle() {
    log.debug "monToggle"
    
    if (device.currentValue("mon") == "0")
    {
        sendEvent(name: "mon", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "mon", value: "0", displayed: false)
    }
}

def tueToggle() {
    log.debug "tueToggle"
    
    if (device.currentValue("tue") == "0")
    {
        sendEvent(name: "tue", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "tue", value: "0", displayed: false)
    }
}

def wedToggle() {
    log.debug "wedToggle"
    
    if (device.currentValue("wed") == "0")
    {
        sendEvent(name: "wed", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "wed", value: "0", displayed: false)
    }
}

def thuToggle() {
    log.debug "thuToggle"
    
    if (device.currentValue("thu") == "0")
    {
        sendEvent(name: "thu", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "thu", value: "0", displayed: false)
    }
}

def friToggle() {
    log.debug "friToggle"
    
    if (device.currentValue("fri") == "0")
    {
        sendEvent(name: "fri", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "fri", value: "0", displayed: false)
    }
}

def satToggle() {
    log.debug "satToggle"
    
    if (device.currentValue("sat") == "0")
    {
        sendEvent(name: "sat", value: "1", displayed: false)
    }
    else
    {
        sendEvent(name: "sat", value: "0", displayed: false)
    }
}
