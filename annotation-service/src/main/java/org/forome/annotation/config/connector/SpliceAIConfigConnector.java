package org.forome.annotation.config.connector;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.connector.database.DatabaseConfigConnector;

public class SpliceAIConfigConnector extends DatabaseConfigConnector {

	public SpliceAIConfigConnector(JSONObject parse) {
		super(parse);
	}
}