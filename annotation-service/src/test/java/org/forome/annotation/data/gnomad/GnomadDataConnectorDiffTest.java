/*
 Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association

 Developed by Vladimir Ulitin and Michael Bouzinier

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.forome.annotation.data.gnomad;

import org.forome.annotation.data.gnomad.old.GnomadDataConnectorOld;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnomadDataConnectorDiffTest {

	private final static Logger log = LoggerFactory.getLogger(GnomadDataConnectorDiffTest.class);

	@Test
	public void test() throws Exception {
		Assert.assertEquals("-CATCATCATCAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "C"));
		Assert.assertEquals("-CAT", GnomadDataConnectorOld.diff("CCATCAT", "CCAT"));
		Assert.assertEquals("-CATCATCAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "CCAT"));
		Assert.assertEquals("-CAT", GnomadDataConnectorOld.diff("CCATCAT", "CCAT"));
		Assert.assertEquals("-CATCAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "CCATCAT"));
		Assert.assertEquals("CAT", GnomadDataConnectorOld.diff("CCATCATCATCAT", "CCATCATCATCATCAT"));
	}

}
