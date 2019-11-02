import groovy.json.*

metadata {
    definition (name: "Tasmota Sprinkler Switch", namespace: "LJ", author: "LJ", vid:"tasmota-sprinkler-switch") {
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
        capability "Refresh"
        capability "Momentary"
    }

    attribute "deviceStatus", "enum", ["offline", "on", "off"]
    attribute "deivceTime", "string"
    
    attribute "sun", "enum", ["1", "0"]
    attribute "mon", "enum", ["1", "0"]
    attribute "tus", "enum", ["1", "0"]
    attribute "wed", "enum", ["1", "0"]
    attribute "thu", "enum", ["1", "0"]
    attribute "fri", "enum", ["1", "0"]
    attribute "sat", "enum", ["1", "0"]
    
    /*command "sunon"
    command "sunoff"
    command "monon"
    command "monoff"
    command "tueon"
    command "tueoff"
    command "wedon"
    command "wedoff"
    command "thuon"
    command "thuoff"
    command "frion"
    command "frioff"
    command "saton"
    command "satoff"*/
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

        /*standardTile("sunday", "device.sun", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Sun", action:"sunoff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Sun", action:"sunon", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }
        
        standardTile("monday", "device.mon", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Mon", action:"monoff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Mon", action:"monon", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }

        standardTile("tuesday", "device.tue", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Tue", action:"tueoff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Tue", action:"tueon", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }

        standardTile("wednesday", "device.wed", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Wed", action:"wedoff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Wed", action:"wedon", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }

        standardTile("thursday", "device.thu", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Thu", action:"thuoff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Thu", action:"thuon", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }
        
        standardTile("friday", "device.fri", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Fri", action:"frioff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Fri", action:"frion", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }

        standardTile("saturday", "device.sat", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
            state "1", label:"Sat", action:"satoff", backgroundColor:"#00a0dc", icon: "st.Outdoor.outdoor12"
            state "0", label:"Sat", action:"saton", backgroundColor:"#ffffff", icon: "st.Outdoor.outdoor12"
        }*/
        
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

    }  

    main(["switch"])
    details(["switch","refresh","monday","tuesday","wednesday","thursday","friday","saturday","sunday","save"])
    
    preferences {
        input name: "ipAddr", type: "text", title: "IP Address", description: "IP Address of the device", required: true,displayDuringSetup: true
        input name: "port", type: "number", title: "Port", description: "Port of the device",  defaultValue: 80 ,displayDuringSetup: true
        input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "password", type: "password", title: "Password", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "beginTime", type: "text", title: "Watering Begin Time", description: "Watering Begin time", defaultValue: "19:50", required: true, displayDuringSetup: true
    }

}

def installed() {
    initialize()
    runEvery1Minute(checkDevice)
    runEvery1Hour(checkTimer)
}

def updated() {
    initialize()
}

def initialize() {
    state.responseReceived = true;

    def jsonstr1 = JsonOutput.toJson([Arm: 1, Mode: 0, Time: "00:00", Window: 0, Days: "1111111", Repeat: 1, Output: 1, Action: 1])
    
    log.debug "timer1 json: $jsonstr"
    
    tasmotaHttpCmd("Timer1%20$jsonstr1")
    
    def jsonstr2 = JsonOutput.toJson([Arm: 1, Mode: 0, Time: "12:00", Window: 0, Days: "1111111", Repeat: 1, Output: 1, Action: 0])
    
    log.debug "timer2 json: $jsonstr"
    
    tasmotaHttpCmd("Timer2%20$jsonstr2") 
    
    tasmotaHttpCmd("Timers%20on")
    
    tasmotaHttpCmd("TimeDST%200,2,3,1,2,-360")
    
    tasmotaHttpCmd("TimeSTD%200,1,11,1,2,-420")
    
    tasmotaHttpCmd("Timezone%2099")

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
    
    def days = jsonStr?.Timer3?.Days
    
    if ((jsonStr?.POWER in ["ON", 1, "1"]) || (jsonStr?.Status?.Power in [1, "1"])) {
        sendEvent(name: "deviceStatus", value: "on")
        createEvent(name: "switch", value: "on")
    }
    else if ((jsonStr?.POWER in ["OFF", 0, "0"]) || (jsonStr?.Status?.Power in [0, "0"])) {
        sendEvent(name: "deviceStatus", value: "off")
        createEvent(name: "switch", value: "off")
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
    else {
        log.error "Unknown message: $msg"
    }

    def timestr = jsonStr?.Time
    
    if (timestr)
    {
        sendEvent(name:"deviceTime", value: "$timestr", displayed: false)
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
        sendEvent(name: "deviceStatus", value: "offline")
    }
    
    state.responseReceived = false;
    tasmotaHttpCmd("State")
}

def checkTimer() {
    log.debug "CHECK_TIMER"
    tasmotaHttpCmd("Timer3")
}


/*
def sunoff() {
    log.debug "sunoff"
    sendEvent(name: "sun", value: "0")
}

def sunon() {
    log.debug "sunon"
    sendEvent(name: "sun", value: "1")
}

def monoff() {
    log.debug "monoff"
    sendEvent(name: "mon", value: "0")
}

def monon() {
    log.debug "monon"
    sendEvent(name: "mon", value: "1")
}

def tueoff() {
    log.debug "tueoff"
    sendEvent(name: "tue", value: "0")
}

def tueon() {
    log.debug "tueon"
    sendEvent(name: "tue", value: "1")
}

def wedoff() {
    log.debug "wedoff"
    sendEvent(name: "wed", value: "0")
}

def wedon() {
    log.debug "wedon"
    sendEvent(name: "wed", value: "1")
}
def thuoff() {
    log.debug "thuoff"
    sendEvent(name: "thu", value: "0")
}

def thuon() {
    log.debug "thuon"
    sendEvent(name: "thu", value: "1")
}
def frioff() {
    log.debug "frioff"
    sendEvent(name: "fri", value: "0")
}

def frion() {
    log.debug "frion"
    sendEvent(name: "fri", value: "1")
}
def satoff() {
    log.debug "satoff"
    sendEvent(name: "sat", value: "0")
}

def saton() {
    log.debug "saton"
    sendEvent(name: "sat", value: "1")
}*/

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
