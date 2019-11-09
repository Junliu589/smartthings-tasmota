metadata {
    definition (name: "Honeywell VISTA", namespace: "LJ", author: "LJ", vid:"honeywell-vista-security-partition") {
        capability "Contact Sensor"
        capability "Switch"
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

        standardTile("ArmMax", "device.partitionStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Max", action:"armMax", icon:"st.security.alarm.on", backgroundColor: "#C0C0C0"
        }
        
        standardTile("ArmInstant", "device.partitionStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Instant", action:"armInstant", icon:"st.security.alarm.on", backgroundColor: "#C0C0C0"
        }
        
        standardTile("Chime", "device.chimeStatus", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "on", label:"Chime", action:"chime", icon:"st.security.alarm.off", backgroundColor: "#79b821"
            state "off", label:"Chime", action:"chime", icon:"st.security.alarm.off", backgroundColor: "#C0C0C0"
        }
    }

    main(["Status"])
    details(["Status","ArmAway","ArmStay","Disarm","ArmMax","ArmInstant","Chime","wifi","ip"])
    
    preferences {
        input name: "macAddr", type: "text", title: "MAC Address", description: "MAC Address of the device", required: true,displayDuringSetup: true
        input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "password", type: "password", title: "Password", description: "Username to manage the device", required: false, displayDuringSetup: true
        input name: "enIpAddr", type: "text", title: "ENVL Board IP Address", description: "IP Address of EVL Board", required: true,displayDuringSetup: true
        input name: "vistaPasscode", type: "password", title: "VISTA Passcode", description: "User Passcode of Honeywell Vista Pannel", required: false,displayDuringSetup: true
        input name: "contactZones", type: "text", title: "Contact Sensor Zones", description: "Comma-separated Door/Windows Contact Sensor Zones, e.g. 1,3,7", required: false,displayDuringSetup: true
        input name: "motionZones", type: "text", title: "Motion Sensor Zones", description: "Comma-separated Montion Sensor Zones, e.g. 2,4,5,6", required: false,displayDuringSetup: true
    }
}

def installed() {
    initialize()
    createChildWifiOutlet()
    createChildZones()
}

def updated() {
    initialize()
    removeChildZones()
    createChildZones()
}

def initialize() {
    state.responseReceived = true
    state.offlineMinutes = 0
    state.notreadyCount = 0
    state.activeZoneList = []
    
    if (device.currentValue("ipAddr"))
    {
        //Schedule it, instead of run it immediately, because deviceNetworkId can't be set in this function.
        runIn(30, configEvl)
    }
    else
    {
        runIn(30, discover)
    }
    
    runEvery1Minute(checkDevice)
}

def configEvl(){
    if (macAddr)
    {
        device.deviceNetworkId = macAddr.tokenize( ':' ).collect{it.toUpperCase()}.join()
    }

    def hub = location.hubs[0]
    def deviceIP = device.currentValue("ipAddr")
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/en?user=${username}&password=${password}&enip=${enIpAddr}&hubip=${hub.localIP}",
        headers: [
            HOST: "$deviceIP:80"
        ]
    )
    
    sendHubCommand(hubAction)
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
    runIn(60, configEvl)
}

def httpCmd(cmd){
    def deviceIP = device.currentValue("ipAddr")
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/ec?user=${username}&password=${password}&cmd=${cmd}",
        headers: [
            HOST: "$deviceIP:80"
        ]
    )
    
    sendHubCommand(hubAction)
}

def parse(description) {
    def msg = parseLanMessage(description)
    log.debug "msg: $msg"

    if (msg?.json) {
        //Tasmota Outlet related message - Pass to the child outlet
        def children = getChildDevices()
        children.each { child ->
            //device.displayName is the same as device.label - the user defined name. 
            //device.name is the internal name when the device is create.
            //device.deviceNetworkId could be modified through the MAC config in the UI
            if (child.name == "Tasmota Wifi Outlet") {
                child.parse(description)
            }
        }
    } else {
        //Vista Pannel Messages
        parseVistaPannelMsg(msg)
    }
    
    //Get the device IP
    def ipStr = msg?.ip
    
    if (ipStr)
    {
        def ip = ""
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

def parseVistaPannelMsg(msg) {
    def body = msg?.body
    def startIndex = body.indexOf('%')
    def endIndex = body.indexOf('$')
    
    if (startIndex > 0 && endIndex > startIndex) {
        def pannelMsg = body.substring(startIndex+1, endIndex)
        log.debug "$pannelMsg"
        
        state.responseReceived = true;
        //The ENVL message should be like: "00,01,0008,05,04,FAULT 05"
        def fields = pannelMsg.split(',')
        
        //check the command code "00" and Partition "01"
        if (fields[0] == "00" && fields[1] == "01")
        {
            def bitfield = Integer.decode("0x" + fields[2]);
            
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
                sendEvent(name: "switch", value: "on")
            }
            else if (bitfield & BIT_READY)
            {
                sendEvent(name: "partitionStatus", value: "ready")
                sendEvent(name: "contact", value: "open")
                sendEvent(name: "switch", value: "off")
                closeAllZones()
                state.notreadyCount = 0
            }
            else if (bitfield & BIT_ARMEDINSTANT)
            {
                sendEvent(name: "partitionStatus", value: "armedstay")
                sendEvent(name: "contact", value: "closed")
                sendEvent(name: "switch", value: "on")
            }
            else if (bitfield & BIT_ARMEDAWAY)
            {
                sendEvent(name: "partitionStatus", value: "armedaway")
                sendEvent(name: "contact", value: "closed")
                sendEvent(name: "switch", value: "on")
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
                //NotReady - increase the count
                state.notreadyCount = state.notreadyCount + 1
                if (state.notreadyCount > 3) 
                {
                    sendEvent(name: "partitionStatus", value: "notready")
                }
                sendEvent(name: "contact", value: "open")
                sendEvent(name: "switch", value: "off")
            }
            
            if (bitfield & BIT_CHIME)
            {
                sendEvent(name: "chimeStatus", value: "on")
            }
            else
            {
                sendEvent(name: "chimeStatus", value: "off")
            }
            
            def zoneNum = fields[3]
            
            if (fields[5])
            {
                sendEvent(name: "keypadText", value: "${fields[5]}", displayed: false)

                Integer currentZone = 0

                //Now parse the "CHECK/FAULT zone" text info
                if (fields[5].matches("(.*)CHECK ${zoneNum}(.*)")) {
                    //Update the Zone status
                    currentZone = zoneNum.toInteger()
                    getChildDevices()?.each { 
                        if (it.deviceNetworkId == "Honeywell-Zone-${currentZone}") {
                            log.debug "Set Zone ${currentZone} in CHECK state"
                            it.zone("check")
                            it.label = fields[5].replaceAll("(.*)CHECK", "Zone")
                        }
                    }
                } else if (fields[5].matches("(.*)FAULT ${zoneNum}(.*)")) {
                    //Update the Zone status
                    currentZone = zoneNum.toInteger()
                    getChildDevices()?.each { 
                        if (it.deviceNetworkId == "Honeywell-Zone-${i}") {
                            log.debug "Set Zone ${i} in Open state"
                            it.zone("open")
                            it.label = fields[5].replaceAll("(.*)FAULT", "Zone")
                        }
                    }
                }

                if (currentZone) {
                    //Check whether the keypad zone info update is displayed from the beginning (the smallest zone)
                    if (!state.activeZoneList.isEmpty()) {
                        def lastZone = state.activeZoneList.pop()
                        state.activeZoneList.add(lastZone)                        
                        if (currentZone < lastZone) {
                            //The zone info is displayed from the beginning. 
                            //All the other zones which are not in this list should be set to closed.

                            getChildDevices()?.each { 
                                if (it.deviceNetworkId.matches("Honeywell-Zone-(.*)")) {
                                    def z = it.deviceNetworkId.substring(15).toInteger()
                                    if (z && (!state.activeZoneList.contains(z))) {
                                        log.debug("Set Zone ${z} Closed")
                                        it.zone("closed")
                                    }
                                }
                            }

                            //Clear the list
                            while (!state.activeZoneList.isEmpty())
                            {
                                state.activeZoneList.pop()
                            }
                        }
                    }
 
                    //Add current zone to the list
                    state.activeZoneList.add(currentZone)
                }

            }
        
        } else if (fields[0] == "01") {
            //Zone Status Command - only parse the first 8 bytes (64 zones)
            def statusBits = Long.reverseBytes(Long.parseLong(fields[1].substring(0,16), 16))
            def statusMap = [
                '0' : "closed",
                '1' : "open",
                '2' : "closed",
                '3' : "alarm"
            ]
            
            getChildDevices()?.each { 
                if (it.deviceNetworkId.matches("Honeywell-Zone-(.*)")) {
                    log.debug "Updating Zone: ${it.deviceNetworkId}"
                    def zoneNum = it.deviceNetworkId.substring(15).toInteger()
                    if (zoneNum) {
                        it.zone(statusMap."${((statusBits >> (zoneNum - 1)) & 0x1) + ((device.currentValue("partitionStatus") == "alarming") ? 2 : 0)}")
                    }
                }
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

def armMax() {
    log.debug "ARMMAX"

    httpCmd("${vistaPasscode}" + "4")
}

def armInstant() {
    log.debug "ARMINSTANT"

    httpCmd("${vistaPasscode}" + "7")
}

def disarm() {
    log.debug "DISARM"

    httpCmd("${vistaPasscode}" + "1")
}

def chime() {
    log.debug "CHIME"
    
    httpCmd("${vistaPasscode}" + "9")
}

def on() {
    armAway()
}

def off() {
    disarm()
}
    
    
def checkDevice() {
    log.debug "CHECK"

    if (!state.responseReceived)
    {
        //No response recevied from the last check command - it's offline
        state.offlineMinutes = state.offlineMinutes + 1
        sendEvent(name: "partitionStatus", value: "offline")
        
        //Try to config the ENVL 
        configEvl()
        
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
}

private void createChildWifiOutlet() {
    addChildDevice("LJ", "Tasmota Wifi Outlet", "Honeywell DNI", null, 
                    [completedSetup: true, label: "Wifi Outlet"])
}

private void createChildZones() {
    contactZones?.split(',')?.each { zoneNum ->
        if (zoneNum.toInteger()) {
            log.debug "Create Contact Zone ${zoneNum}"
            addChildDevice("redloro-smartthings", "Honeywell Zone Contact", "Honeywell-Zone-${zoneNum}", null, 
                           [completedSetup: true, label: "Zone ${zoneNum}"])
        }
    }

    motionZones?.split(',')?.each { zoneNum ->
        if (zoneNum.toInteger()) {
            log.debug "Create Motion Zone ${zoneNum}"
            addChildDevice("redloro-smartthings", "Honeywell Zone Motion", "Honeywell-Zone-${zoneNum}", null, 
                           [completedSetup: true, label: "Zone ${zoneNum}"])
        }
    }
}

private void removeChildZones() {
    getChildDevices()?.each { 
        if (it.name != "Tasmota Wifi Outlet") {
            log.debug "removing ${it.name}"
            deleteChildDevice(it.deviceNetworkId) 
        }
    }
}

private void closeAllZones() {
    getChildDevices()?.each { 
        if (it.deviceNetworkId.matches("Honeywell-Zone-(.*)")) {
            it.zone("closed")
        }
    }
}