metadata {
  definition (name: "Dummy Motion Sensor", namespace: "LJ", author: "Jun Liu") {
    capability "Motion Sensor"

    command "setStatus"
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
      tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
        attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
      }
    }

    main "motion"

    details(["motion"])
  }
}

def setStatus(String state) {
  // need to convert open to inactive and closed to active
  def eventMap = [
    'closed':"active",
    'open':"inactive"
  ]
  def newState = eventMap."${state}"

  sendEvent (name: "motion", value: "${newState}")
}