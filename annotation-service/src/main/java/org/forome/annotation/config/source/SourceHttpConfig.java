/*
 *  Copyright (c) 2020. Vladimir Ulitin, Partners Healthcare and members of Forome Association
 *
 *  Developed by Vladimir Ulitin and Michael Bouzinier
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.forome.annotation.config.source;

import net.minidev.json.JSONObject;
import org.forome.annotation.config.sshtunnel.SshTunnelConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class SourceHttpConfig {

	public final SshTunnelConfig sshTunnelConfig;
	public final URL url;

	public SourceHttpConfig(JSONObject parse) {
		if (parse.containsKey("ssh_tunnel")) {
			sshTunnelConfig = new SshTunnelConfig((JSONObject) parse.get("ssh_tunnel"));
		} else {
			sshTunnelConfig = null;
		}

		try {
			url = new URL(parse.getAsString("url"));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
