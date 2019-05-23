package com.ninetailsoftware.ha.integration.transformers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ninetailsoftware.model.control.ControlByRef;
import com.ninetailsoftware.model.events.HaEvent;

public class MqttTransformer {
	
	Logger log = LoggerFactory.getLogger(MqttTransformer.class);

	public HaEvent transformToCEPEvent(String topic, String body) {
		HaEvent _retValue = new HaEvent();

		String inbound = topic + " " + body;
		
		log.info("Begining transform: " + inbound);

		String delims = "[ /]+";
		String[] tokens = inbound.split(delims);
		String source = tokens[1];
		
		switch (source) {
			case "homeseer":
				_retValue = homeSeerEvent(tokens);
				break;
		}
		
		return _retValue;
	}
	
	public HaEvent adtEventTransformer(String body){
		HaEvent _retValue = new HaEvent();
		
		log.info("Begining Transform: " +  body);
		
		String[] lines = body.split("\\n");
		String line2 = lines[1];
		
		_retValue.setSource("adt");
		_retValue.setValue(line2);
		
		return _retValue;		
	}
	
	public ControlByRef transformToDeviceControl(String topic, String body){
		String inbound = topic + " " + body;
		
		log.info("Begining transform: " + inbound);

		String delims = "[ ,]+";
		String[] tokens = inbound.split(delims);
		String source = tokens[1];
		
		log.info("Sending Message To HomeSeer");
		log.info("Source: " + tokens[0]);
		log.info("Device Id: " + tokens[1]);
		log.info("Value: " + tokens[2]);
		
		ControlByRef _retValue = new ControlByRef();
		
		_retValue.setAction("controlbyvalue");
		_retValue.setDeviceref(tokens[1]);
		_retValue.setValue(tokens[2]);
		
		return _retValue;
	}

	private HaEvent homeSeerEvent(String[] tokens) {
		log.info("Creating New Event");
		log.info("Source: " + tokens[1]);
		log.info("Device Id: " + tokens[2]);
		log.info("Value: " + tokens[4]);
		
		HaEvent _retValue = new HaEvent();

		_retValue.setSource(tokens[1]);
		_retValue.setDeviceId(tokens[2]);
		_retValue.setValue(tokens[4]);

		return _retValue;
	}
	
	public HaEvent returnSingleEvent(List<HaEvent> list) {
		return list.get(0);
	}
}
