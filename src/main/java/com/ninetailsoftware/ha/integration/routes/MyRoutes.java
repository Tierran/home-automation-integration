package com.ninetailsoftware.ha.integration.routes;

import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ninetailsoftware.ha.integration.transformers.MqttTransformer;

public class MyRoutes extends RouteBuilder {

    Logger log = LoggerFactory.getLogger(MyRoutes.class);
    private String homeseerInputEndpoint;
    private String brmsInputEndpoint;
    private String cepOutputEndpoint;
    private String homeseerRestEndpoint;
    private String emailEndpoint;

	@Inject
	private MqttTransformer mqttTransformer;

	@Override
	public void configure() throws Exception {

        final InputStream is = this.getClass().getClassLoader().getResourceAsStream("home-automation-integration.properties");
        final Properties appProps = new Properties();
        appProps.load(is);
        this.homeseerInputEndpoint = appProps.getProperty("homeseerInputEndpoint");
        this.brmsInputEndpoint = appProps.getProperty("brmsInputEndpoint");
        this.cepOutputEndpoint = appProps.getProperty("cepOutputEndpoint");
        this.homeseerRestEndpoint = appProps.getProperty("homeseerRestEndpoint");
        this.emailEndpoint = appProps.getProperty("emailEndpoint");

        log.info("Connected to MQTT Endpoint : " + homeseerInputEndpoint);
        from(homeseerInputEndpoint).convertBodyTo(java.lang.String.class)
				.bean(mqttTransformer, "transformToCEPEvent(${header.CamelMQTTSubscribeTopic}, ${body})").marshal()
				.json(JsonLibrary.Jackson).log("${body}").setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).to(cepOutputEndpoint);

		from(brmsInputEndpoint).convertBodyTo(java.lang.String.class)
				.bean(mqttTransformer, "transformToDeviceControl(${header.CamelMQTTSubscribeTopic}, ${body})").marshal()
				.json(JsonLibrary.Jackson).log("${body}").setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).to(homeseerRestEndpoint);
		
		from(emailEndpoint).convertBodyTo(java.lang.String.class)
				.bean(mqttTransformer, "adtEventTransformer(${body})").marshal()
				.json(JsonLibrary.Jackson).log("${body}").setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.setHeader(Exchange.CONTENT_TYPE, constant("application/json")).to(cepOutputEndpoint);
	}
}
