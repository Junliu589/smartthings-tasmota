metadata {
    definition (name: "Honeywell VISTA", namespace: "LJ", author: "LJ", vid:"honeywell-vista-security-partition") {
        capability "Actuator"
        capability "Sensor"
        capability "Button"
        capability "Contact Sensor"
    }

    attribute "partitionStatus", "enum", ["offline", "armedaway", "armedstay", "exitdelay", "ready", "notready", "alarmed", "alarming"]
    attribute "chimeStatus", "enum", ["on", "off"]
    
    command "armStay"
    command "armAway"
    command "disarm"
    command "chime"
    
    tiles (scale: 2){      
        multiAttributeTile(name:"Status", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.partitionStatus", key: "PRIMARY_CONTROL") {
                attributeState "armedaway", label:'armed-away', backgroundColor:"#00a0dc", icon: "st.Home.home3"
                attributeState "armedstay", label:'armed-stay', backgroundColor:"#00a0dc", icon: "st.Home.home3"
                attributeState "exitdelay", label: 'exit-delay', backgroundColor: "#ffcc00", icon:"st.Home.home3"
                attributeState "ready", label:'ready', backgroundColor:"#79b821", icon: "st.Home.home2"
                attributeState "notready", label:'not-ready', backgroundColor:"#ffffff", icon: "st.Home.home2"
                attributeState "offline", label:'offline', backgroundColor:"#e86d13", icon: "st.Home.home2"
                attributeState "alarmed", label: 'Alarm-in-Mem', backgroundColor: "#ffcc00", icon:"st.Home.home2"
                attributeState "alarming", label: 'Alarming', backgroundColor: "#ff0000", icon:"st.Home.home3"
            }
            tileAttribute ("keypadText", key: "SECONDARY_CONTROL") {
                attributeState "keypadText", label:'${currentValue}'
            }
        }
        
        standardTile("ArmAway", "device.partitionStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Away", action:"armAway", icon:"st.security.alarm.on", backgroundColor: "#C0C0C0"
        }

        standardTile("ArmStay", "device.partitionStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Stay", action:"armStay", icon:"st.security.alarm.on", backgroundColor: "#C0C0C0"
        }

        standardTile("Disarm", "device.partitionStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Disarm", action:"disarm", icon:"st.security.alarm.off", backgroundColor: "#C0C0C0"
        }

        standardTile("Chime", "device.chimeStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "on", label:"Chime", action:"chime", icon:"st.security.alarm.off", backgroundColor: "#79b821"
            state "off", label:"Chime", action:"chime", icon:"st.security.alarm.off", backgroundColor: "#C0C0C0"
        }

    }

    main(["Status"])
    details(["Status","ArmAway","ArmStay","Disarm","Chime"])
    
    preferences {
        input name: "macAddr", type: "text", title: "MAC Address", description: "MAC Address of the device", required: true,displayDuringSetup: true
        input name: "ipAddr", type: "text", title: "IP Address", description: "IP Address of the device", required: true,displayDuringSetup: true
        input name: "port", type: "number", title: "Port", description: "Port of the device",  defaultValue: 80 ,displayDuringSetup: true
        input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "password", type: "password", title: "Password", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "enIpAddr", type: "text", title: "ENVL Board IP Address", description: "IP Address of EVL Board", required: true,displayDuringSetup: true
        input name: "vistaPasscode", type: "password", title: "VISTA Passcode", description: "User Passcode of Honeywell Vista Pannel", required: false,displayDuringSetup: true
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
    device.deviceNetworkId = "${macAddr}" 
    state.responseReceived = true
    configEvl()
}

def convertIPtoHex(ipAddress) { 
    ipAddress.tokenize( '.' ).collect {String.format( '%02X', it.toInteger())}.join()
}

def convertPortToHex(port) {
    port.toString().format( '%04X', port.toInteger() )
}

def configEvl(){
    def hosthex = convertIPtoHex(ipAddr)
    def porthex = convertPortToHex(port)
    
    def hub = location.hubs[0]
    
    //device.deviceNetworkId = "$hosthex:$porthex" 
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/en?user=${username}&password=${password}&enip=${enIpAddr}&hubip=${hub.localIP}",
        headers: [
            HOST: "${ipAddr}:${port}"
        ]
    )
    
    sendHubCommand(hubAction)
}


def httpCmd(cmd){
    def hosthex = convertIPtoHex(ipAddr)
    def porthex = convertPortToHex(port)
    
    //device.deviceNetworkId = "$hosthex:$porthex" 
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/ec?user=${username}&password=${password}&cmd=${cmd}",
        headers: [
            HOST: "${ipAddr}:${port}"
        ]
    )
    
    sendHubCommand(hubAction)
}

def parse(description) {
    state.responseReceived = true;
    def msg = parseLanMessage(description)
    log.debug "msg: $msg"

    def body = msg?.body
    def startIndex = body.indexOf('%')
    def endIndex = body.indexOf('$')
    
    if (startIndex > 0 && endIndex > startIndex) {
        def pannelMsg = body.substring(startIndex+1, endIndex)
        log.debug "$pannelMsg"
        
        //The ENVL message should be like: "00,01,0008,05,04,FAULT 05"
        def fields = pannelMsg.split(',')
        
        //Only check the command code "00" and Partition "01"
        if (fields[0] == "00" && fields[1] == "01")
        {
            def bitfield = Integer.decode("0x" + fields[2]);
            log.debug "bitfield: $bitfield"
            
            def BIT_ARMEDSTAY     = 0x8000
            def BIT_LOWBATTERY    = 0x4000
            def BIT_FIRE          = 0x2000
            def BIT_READY         = 0x1000
            def BIT_TROUBLE       = 0x0200
            def BIT_FIREALARM     = 0x0100
            def BIT_ARMEDINSTANT  = 0x0080
            def BIT_CHIME         = 0x0020
            def BIT_BYPASS        = 0x0010
            def BIT_ACPRESENT     = 0x0008
            def BIT_ARMEDAWAY     = 0x0004
            def BIT_ALARMINMEM    = 0x0002
            def BIT_ALARM         = 0x0001
            
            if (bitfield & BIT_ARMEDSTAY)
            {
                sendEvent(name: "partitionStatus", value: "armedstay")
                sendEvent(name: "contact", value: "closed")
            }
            else if (bitfield & BIT_READY)
            {
                sendEvent(name: "partitionStatus", value: "ready")
                sendEvent(name: "contact", value: "open")
            }
            else if (bitfield & BIT_ARMEDINSTANT)
            {
                sendEvent(name: "partitionStatus", value: "armedstay")
                sendEvent(name: "contact", value: "closed")
            }
            else if (bitfield & BIT_ARMEDAWAY)
            {
                sendEvent(name: "partitionStatus", value: "armedaway")
                sendEvent(name: "contact", value: "closed")
            }
            else if (bitfield & BIT_ALARMINMEM)
            {
                sendEvent(name: "partitionStatus", value: "alarmed")
            }
            else if (bitfield & BIT_ALARM)
            {
                sendEvent(name: "partitionStatus", value: "alarming")
            }
            else
            {
                sendEvent(name: "partitionStatus", value: "notready")
            }
            
            if (bitfield & BIT_CHIME)
            {
                sendEvent(name: "chimeStatus", value: "on")
            }
            else
            {
                sendEvent(name: "chimeStatus", value: "off")
            }
            
            if (fields[5])
            {
                sendEvent(name: "keypadText", value: "${fields[5]}", displayed: false)
            }
        
        }
        

    }
    
}


def armAway() {
    log.debug "ARMAWAY"

    httpCmd("${vistaPasscode}" + "2")
}

def armStay() {
    log.debug "ARMSTAY"

    httpCmd("${vistaPasscode}" + "3")
}

def disarm() {
    log.debug "DISARM"

    httpCmd("${vistaPasscode}" + "1")
}

def chime() {
    log.debug "CHIME"
    
    httpCmd("${vistaPasscode}" + "9")
}

def checkDevice() {
    log.debug "CHECK"

    if (!state.responseReceived)
    {
        sendEvent(name: "partitionStatus", value: "offline")
        device.deviceNetworkId = "${macAddr}"
        configEvl()
    }
    state.responseReceived = false;
}
