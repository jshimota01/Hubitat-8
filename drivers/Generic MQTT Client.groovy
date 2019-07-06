/**
 *  ****************  Generic MQTT Driver  ****************
 *
 *  importUrl: https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/Generic%20MQTT%20Client.groovy
 *
 *  Design Usage:
 *  This driver is a generic MQTT driver to pull and post to a MQTT broker.
 *
 *  Copyright 2019 Aaron Ward
 *  
 *  This driver is free and you may do as you likr with it.  
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *
 *  Changes:
 *
 *  1.0.1 - added importURL and updated to new MQTT client methods
 *  1.0.0 - Initial release
 */

metadata {
    definition (name: "Generic MQTT Driver", namespace: "aaronward", author: "Aaron Ward", importURL: "https://raw.githubusercontent.com/PrayerfulDrop/Hubitat/master/drivers/Generic%20MQTT%20Client.groovy") {
        capability "Initialize"
        command "publishMsg", ["String"]
		attribute "delay", "number"
		attribute "distance", "number"
	   }

    preferences {
        input name: "MQTTBroker", type: "text", title: "MQTT Broker Address:", required: true, displayDuringSetup: true
		input name: "username", type: "text", title: "MQTT Username:", description: "(blank if none)", required: false, displayDuringSetup: true
		input name: "password", type: "password", title: "MQTT Password:", description: "(blank if none)", required: false, displayDuringSetup: true
		input name: "topicSub", type: "text", title: "Topic to Subscribe:", description: "Example Topic (topic/device/#)", required: false, displayDuringSetup: true
		input name: "topicPub", type: "text", title: "Topic to Publish:", description: "Topic Value (topic/device/value)", required: false, displayDuringSetup: true
	    input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: true)
    }

}


def installed() {
    log.info "installed..."
}

// Parse incoming device messages to generate events
def parse(String description) {
	Date date = new Date(); 
	topic = interfaces.mqtt.parseMessage(description).topic
	topic = topic.substring(topic.lastIndexOf("/") + 1)
	payload = interfaces.mqtt.parseMessage(description).payload
	if (logEnable) log.debug topic
	if (logEnable) log.debug payload
	sendEvent(name: "${topic}", value: "${payload}", displayed: true)
	sendEvent(name: "Last Payload Received", value: "Topic: ${topic} - ${date.toString()}", displayed: true)
	state."${topic}" = "${payload}"
	state.lastpayloadreceived = "Topic: ${topic} : ${payload} - ${date.toString()}"

}

def publishMsg(String s) {
	if (logEnable) log.debug "Sent this: ${s} to ${settings?.topicPub}"
    interfaces.mqtt.publish(settings?.topicPub, s)
}

def updated() {
    if (logEnable) log.info "Updated..."
    initialize()
}

def uninstalled() {
    if (logEnable) log.info "Disconnecting from mqtt"
    interfaces.mqtt.disconnect()
}


def initialize() {
	if (logEnable) runIn(900,logsOff)
	try {
        //open connection
		mqttbroker = "tcp://" + settings?.MQTTBroker + ":1883"
        interfaces.mqtt.connect(mqttbroker, "hubitat", settings?.username,settings?.password)
        //give it a chance to start
        pauseExecution(1000)
        log.info "Connection established"
		if (logEnable) log.debug "Subscribed to: ${settings?.topicSub}"
        interfaces.mqtt.subscribe(settings?.topicSub)
    } catch(e) {
        if (logEnable) log.debug "Initialize error: ${e.message}"
    }
}

def mqttClientStatus(String status){
    if (logEnable) log.debug "MQTTStatus- error: ${status}"
}

def logsOff(){
    log.warn "Debug logging disabled."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
