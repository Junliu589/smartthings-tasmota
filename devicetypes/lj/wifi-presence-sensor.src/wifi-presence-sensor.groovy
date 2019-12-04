metadata {
  definition (name: "Wifi Presence Sensor", namespace: "LJ", author: "LJ", vid:"wifi-presence-sensor") {
    capability "Contact Sensor"
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"zone", type: "generic", width: 6, height: 4){
      tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
        attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
        attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
      }
    }
  }
  
  main "zone"
  details(["zone"])
  
  preferences {
    input name: "macAddr", type: "text", title: "MAC Address", description: "MAC Address of the device", required: true,displayDuringSetup: true
  }
}

def installed() {
  initialize()
}

def updated() {
  initialize()
}

def initialize() {
  state.responseReceived = true
  state.firstRSSIs = []
  state.lastRSSIs = []
  
  //Schedule it, instead of run it immediately, because deviceNetworkId can't be set in this function.
  runIn(30,configDevice)

  runEvery1Minute(checkDevice)
}

def configDevice() {
  if (macAddr) {
    device.deviceNetworkId = macAddr.tokenize( ':' ).collect{it.toUpperCase()}.join()
  }
}

def parse(description) {
  state.responseReceived = true
  
  def msg = parseLanMessage(description)
  log.debug "Msg: $msg"
  def jsonStr = msg?.json
  log.debug "jsonStr : $jsonStr "
    
  def wifiRssi = jsonStr?.RSSI
  
  if (wifiRssi) {
    sendEvent(name:"contact", value: "open", descriptionText: "Arrived")
    
    if (state.firstRSSIs.size() < 3) {
      state.firstRSSIs.add(wifiRssi)
    }
    
    if (state.lastRSSIs.size() < 3) {
      state.lastRSSIs.add(wifiRssi)
    } else {
      state.lastRSSIs = state.lastRSSIs.drop(1)
      state.lastRSSIs.add(wifiRssi)
    }
  }
}

def checkDevice() {
  log.debug "CHECK"

  if (!state.responseReceived && state.firstRSSIs.size() && state.lastRSSIs.size()) {
    def rssis = "First: " + state.firstRSSIs.join(",") + "	Last: " + state.lastRSSIs.join(",")
    log.debug "$rssis"

    def avgFirst = state.firstRSSIs.sum()/state.firstRSSIs.size()
    def avgLast = state.lastRSSIs.sum()/state.lastRSSIs.size()
    
    log.debug "avgFirst: $avgFirst"
    log.debug "avgLast: $avgLast"
  
    state.firstRSSIs = []
    state.lastRSSIs = []
    if (avgFirst - avgLast >= 10)  {
      sendEvent(name:"contact", value: "closed", descriptionText: "Left")
    } 
    
    //for debug
    sendEvent(name:"rssi", value: "${rssis}")
  }

  state.responseReceived = false
}