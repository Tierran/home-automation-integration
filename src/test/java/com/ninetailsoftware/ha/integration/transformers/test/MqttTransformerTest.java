package com.ninetailsoftware.ha.integration.transformers.test;

import org.junit.Assert;
import org.junit.Test;

import com.ninetailsoftware.ha.integration.transformers.MqttTransformer;
import com.ninetailsoftware.model.events.HaEvent;

public class MqttTransformerTest {

	@Test
	public void testAdtEventTransformer() {
		MqttTransformer mt = new MqttTransformer();
		
		String emailBody = "System Armed (home mode): Your SimpliSafe System was armed (home) at 812 Jefferson Ave on 11-5-19 at 8:06 pm";
		HaEvent event = mt.adtEventTransformer(emailBody);
		
		Assert.assertEquals("System Armed (home mode)", event.getValue());
	}
}
