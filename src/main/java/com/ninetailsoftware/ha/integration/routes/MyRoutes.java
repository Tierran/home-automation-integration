package com.ninetailsoftware.ha.integration.routes;

import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.language.SimpleExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ninetailsoftware.ha.integration.transformers.MqttTransformer;

public class MyRoutes extends RouteBuilder {

	Logger log = LoggerFactory.getLogger(MyRoutes.class);
	private String homeseerInputEndpoint;
	private String brmsInputEndpoint;
	private String cepOutputEndpoint;
	private String homeseerRestEndpoint;
	private String statusUpdateRequestEndpoint;
	private String emailEndpoint;

	@Inject
	private MqttTransformer mqttTransformer;

	@Resource(lookup = "java:/SqliteDS", name = "dataSource")
	private DataSource dataSource;

	@Override
	public void configure() throws Exception {

		final InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream("home-automation-integration.properties");
		final Properties appProps = new Properties();
		appProps.load(is);
		this.homeseerInputEndpoint = appProps.getProperty("homeseerInputEndpoint");
		this.brmsInputEndpoint = appProps.getProperty("brmsInputEndpoint");
		this.cepOutputEndpoint = appProps.getProperty("cepOutputEndpoint");
		this.homeseerRestEndpoint = appProps.getProperty("homeseerRestEndpoint");
		this.statusUpdateRequestEndpoint = appProps.getProperty("statusUpdateRequestEndpoint");
		this.emailEndpoint = appProps.getProperty("emailEndpoint");

		from(homeseerInputEndpoint).convertBodyTo(java.lang.String.class)
				.bean(mqttTransformer, "transformToCEPEvent(${header.CamelMqttTopic}, ${body})")
					.to("seda:updateDeviceStatus")
				.marshal().json(JsonLibrary.Jackson)
				.setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.to(cepOutputEndpoint);

		from(brmsInputEndpoint).convertBodyTo(java.lang.String.class)
				.bean(mqttTransformer, "transformToDeviceControl(${header.CamelMqttTopic}, ${body})").marshal()
				.json(JsonLibrary.Jackson).log("${body}").setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).to(homeseerRestEndpoint);

		/**
		 * This endpoint can be configured to pick up email events from ADT Pulse.  Pulse can not generally be integrated with
		 * other Home Automation systems but as of right now, can send updates via email on device status.  This is useful for 
		 * recognizing what the alarm status is and can be used with ADT cameras to detect motion.  This is very failure prone
		 * due to email not always arriving when actions happen.
		 * 
		 * Currently the adtEventTransformer method only recognizes alarm changes but could be easily updated to accept other types
		 * of events.
		 */
		
		from(emailEndpoint).convertBodyTo(java.lang.String.class).bean(mqttTransformer, "adtEventTransformer(${body})")
				.marshal().json(JsonLibrary.Jackson).log("${body}").setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).to(cepOutputEndpoint);

		from(statusUpdateRequestEndpoint)
				.setBody(new SimpleExpression("select * from DeviceTable where source = 'homeseer' and device_id = '${body}'"))
				.to("jdbc:dataSource?outputClass=com.ninetailsoftware.model.events.HaEvent&useHeadersAsParameters=true")
				.choice()
					.when(simple("${body.size()} > 0"))
						.bean(mqttTransformer, "returnSingleEvent(${body})").marshal().json(JsonLibrary.Jackson)
						.setHeader(Exchange.HTTP_METHOD, constant("POST"))
						.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
						.to(cepOutputEndpoint)
					.otherwise()
						.log("No status found for homeseer device")
				.endChoice();

		/**
		 * This route asynchronously takes status updates on devices and inserts or updates the sqlite database
		 * with the new status. 
		 */
		from("seda:updateDeviceStatus").log("Async call received: ${body.deviceId}")
				.setHeader("deviceid", simple("${body.deviceId}"))
				.setHeader("source", simple("${body.source}"))
				.setHeader("value", simple("${body.value}"))
				.setBody(simple("insert into DeviceTable (device_id, source, value) values (:?deviceid, :?source, :?value) ON CONFLICT (device_id, source) DO UPDATE SET value = excluded.value"))
				.log("${body}")
				.to("jdbc:dataSource?outputClass=com.ninetailsoftware.model.events.HaEvent&useHeadersAsParameters=true");
	}
}
