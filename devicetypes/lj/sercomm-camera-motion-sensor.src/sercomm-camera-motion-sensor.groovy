metadata {
  definition (name: "Sercomm Camera Motion Sensor", namespace: "LJ", author: "LJ", vid:"camera-motion-sensor") {
    capability "Motion Sensor"
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
      tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
        attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
        attributeState "offline", label:'${name}', icon: "st.motion.motion.inactive", backgroundColor:"#e86d13"
      }
      
      tileAttribute ("device.ipAddr", key: "SECONDARY_CONTROL") {
        attributeState "default", label:'${currentValue}'
      }
    }

    main "motion"

    details(["motion"])
  }
  
  preferences {
    input name: "macAddr", type: "text", title: "MAC Address", description: "MAC Address of the device", required: true,displayDuringSetup: true
    input name: "username", type: "text", title: "Username", description: "Username to manage the device", required: false, displayDuringSetup: true
    input name: "password", type: "password", title: "Password", description: "Username to manage the device", required: false, displayDuringSetup: true    
    input name: "emailnotification", type: "bool", title: "Email Notification", description: "Send a email notification with the captured video",  required: false, displayDuringSetup: true 
    input name: "emailaddr", type: "text", title: "Email Address", description: "Email Address for the notification", required: false, displayDuringSetup: true
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
 
  if (device.currentValue("ipAddr"))
  {
    //Schedule it, instead of run it immediately, because deviceNetworkId can't be set in this function.
    runIn(30,configDevice)
  }
  else
  {
    runIn(30, discover)
  }
  
  runEvery1Minute(checkDevice)
}

def configDevice() {
  if (macAddr) {
    device.deviceNetworkId = macAddr.tokenize( ':' ).collect{it.toUpperCase()}.join()
  }
  
  httpCmd("/adm/set_group.cgi?group=SYSTEM&time_zone=7&daylight_saving=1&pir_mode=1&pir_mot_mode=1&pir_mot_timer=1")
  httpCmd("/adm/set_group.cgi?group=UPNP&upnp_mode=0")
  def videosetting = "/adm/set_group.cgi?group=VIDEO&time_stamp=1&text_overlay=1&text=" + device.displayName.replaceAll("\\s","%20")
  httpCmd(videosetting)
  httpCmd("/adm/set_group.cgi?group=H264&mode2=0&mode=1&resolution=4&quality_level=5&frame_rate=30")
  httpCmd("/adm/set_group.cgi?group=JPEG&mode2=1&resolution2=4&quality_level2=5&frame_rate2=30")
  httpCmd("/adm/set_group.cgi?group=AUDIO&au_trigger_en=1&au_trigger_volume=30")
  httpCmd("/adm/set_group.cgi?group=MOTION&md_mode=1&md_window1=0,0,639,478")
  httpCmd("/adm/set_group.cgi?group=EVENT&event_trigger=1&event1_entry=is=1|es=0,|et=3|acts=op1:0;op2:0;email:1;ftpu:1;im:0;httpn:1;httppost:0;wlled:0;smbc:0;sd:0;op3:0;op4:0;smbc_rec:0;sd_rec:0|ei=0|ea=mp4,5,15,1|en=motion")
  //httpCmd("/adm/set_group.cgi?group=EVENT&event_trigger=1&event2_entry=is=1|es=0,|et=6|acts=op1:0;op2:0;email:0;ftpu:0;im:0;httpn:1;httppost:0;wlled:0;smbc:0;sd:0;op3:0;op4:0;smbc_rec:0;sd_rec:0|ei=600|ea=mp4,5,15,1|en=PeriodicalEvent")
  httpCmd("/adm/set_group.cgi?group=HTTP_NOTIFY&http_url=http://192.168.0.7:39500&event_data_flag=1")
  httpCmd("/adm/set_group.cgi?group=HTTP_EVENT&http_event_en=1")
  def ftpsetting = "/adm/set_group.cgi?group=FTP&ftp1=1&ftp1_server=192.168.0.33&ftp1_account=user1&ftp1_passwd=user1_ftp&ftp1_path=/sda1/" + device.displayName.replaceAll("\\s","")
  httpCmd(ftpsetting)
  def emailsetting = "/adm/set_group.cgi?group=EMAIL&smtp_server=192.168.0.33&smtp_port=25&from_addr=" + device.displayName.replaceAll("\\s","") + "@home.com&to_addr1=" + emailaddr + "&subject=video_captured"
  httpCmd(emailsetting)  
  if (emailnotification) {
      httpCmd("/adm/set_group.cgi?group=EMAIL&send_email=7")
  } else {
      httpCmd("/adm/set_group.cgi?group=EMAIL&send_email=0")
  }
  
}

def discover() {
  if (macAddr) {
    device.deviceNetworkId = macAddr?.tokenize( ':' ).collect{it.toUpperCase()}.join()
  }

  for (int i=2; i<100; i++) {
    log.debug "Sent to 192.168.0.${i}"
    
    def hubAction = new physicalgraph.device.HubAction(
      method: "GET",
      path: "/",
      headers: [
        HOST: "192.168.0.${i}:80"
      ]
    )
    
    sendHubCommand(hubAction)
  }
    
  //Try to config the device after the discovery
  runIn(60, configDevice)
}

def parse(description) {
  state.responseReceived = true
  
  def msg = parseLanMessage(description)
  log.debug "Msg: $msg"

  if (msg?.header.contains("?event=pir")) {
    log.debug "PIR Event received"
    sendEvent(name:"motion", value: "active", descriptionText: "Motion Active")
  }
  
  def ipStr = msg?.ip
    
  if (ipStr) {
    def ip = "";
    for (int i=0; i<8; i+=2) {
      if (ip != "")
        ip += "."
      ip += Integer.parseInt(ipStr.substring(i, i+2), 16).toString()
    }

    log.debug "Device IP: $ip"
    sendEvent(name:"ipAddr", value: "$ip", displayed: false)
  }
  
}

def checkDevice() {
  log.debug "CHECK"

  if (!state.responseReceived) {
    sendEvent(name:"motion", value: "offline", descriptionText: "Motion Sensor offline")
  } else {
    sendEvent(name:"motion", value: "inactive", descriptionText: "Motion Inactive")
  }

  state.responseReceived = false
  httpCmdNoAuth("/")
}

def httpCmd(cmd){
    def deviceIP = device.currentValue("ipAddr")
    
    def authinfo = "$username:$password".bytes.encodeBase64()
    log.debug "auth: $authinfo"
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "${cmd}",
        headers: [
            HOST: "$deviceIP:80",
            Authorization: "Basic $authinfo" 
        ]
    )
    
    sendHubCommand(hubAction)
}

def httpCmdNoAuth(cmd){
    def deviceIP = device.currentValue("ipAddr")
   
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "${cmd}",
        headers: [
            HOST: "$deviceIP:80",
        ]
    )
    
    sendHubCommand(hubAction)
}