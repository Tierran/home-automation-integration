package com.ninetailsoftware.ha.integration.utility;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.sql.DataSource;

public class DatasourceProducer {
	@Resource(lookup = "java:/SqliteDS")
	private DataSource dataSource;
	
	@Produces
	@Named("homeAutomationDS")
	public DataSource getDataSource() {
		return dataSource;
	}
}
