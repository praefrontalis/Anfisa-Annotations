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

package org.forome.annotation.annotator.struct;

import com.google.common.base.Strings;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.forome.annotation.data.anfisa.AnfisaConnector;
import org.forome.annotation.data.conservation.ConservationData;
import org.forome.annotation.struct.SourceMetadata;
import org.forome.annotation.struct.mcase.Cohort;
import org.forome.annotation.struct.mcase.MCase;
import org.forome.annotation.struct.mcase.Sample;
import org.forome.annotation.struct.mcase.Sex;
import org.forome.annotation.utils.AppVersion;
import org.forome.core.struct.Assembly;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatorResultMetadata {

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
			.withZone(ZoneId.systemDefault());

	public enum Mode {

		HG19("hg19"),
		HG38("hg38");

		private final String value;

		Mode(String value) {
			this.value = value;
		}
	}

	public enum DataSchema {

		CASE("CASE"),
		FAVOR("FAVOR");

		private final String value;

		DataSchema(String value) {
			this.value = value;
		}
	}

	public static class Versions {

		private static Pattern PATTERN_GATK = Pattern.compile(
				"^<ID=(.*)>$", Pattern.CASE_INSENSITIVE
		);

		private static Pattern PATTERN_VEP_VERSION = Pattern.compile(
				"^v(.*?) (.*)$", Pattern.CASE_INSENSITIVE
		);

		public final Instant pipelineDate;
		public final String pipeline;
		public final String annotations;
		public final String annotationsBuild;
		public final String reference;

		public final List<SourceMetadata> metadataDatabases;

		private final String toolGatk;
		private final String toolGatksCombineVariants;
		private final String toolGatksApplyRecalibration;
		private final String toolGatkSelectVariants;
		private final String bcftoolsAnnotateVersion;
		private final String vepVersion;

		public Versions(Path pathVcf, AnfisaConnector anfisaConnector) {
			annotations = AppVersion.getVersionFormat();
			annotationsBuild = AppVersion.getVersion();
			if (pathVcf != null) {
				VCFFileReader vcfFileReader = new VCFFileReader(pathVcf, false);
				VCFHeader vcfHeader = vcfFileReader.getFileHeader();

				VCFHeaderLine hlPipeline = vcfHeader.getOtherHeaderLine("source");
				pipeline = (hlPipeline != null) ? hlPipeline.getValue() : null;

				VCFHeaderLine hlReference = vcfHeader.getOtherHeaderLine("reference");
				reference = (hlReference != null) ? hlReference.getValue() : null;

				VCFHeaderLine hlPipelineDate = vcfHeader.getOtherHeaderLine("fileDate");
				if (hlPipelineDate != null) {
					try {
						pipelineDate = new SimpleDateFormat("yyyyMMdd").parse(hlPipelineDate.getValue()).toInstant();
					} catch (ParseException e) {
						throw new RuntimeException(e);
					}
				} else {
					pipelineDate = null;
				}

				toolGatk = getGatkVersion(vcfHeader.getMetaDataLine("GATKCommandLine"));

				toolGatksCombineVariants = getGatkVersion(vcfHeader.getMetaDataLine("GATKCommandLine.CombineVariants"));

				toolGatksApplyRecalibration = getGatkVersion(vcfHeader.getMetaDataLine("GATKCommandLine.ApplyRecalibration"));

				toolGatkSelectVariants = getGatkVersion(vcfHeader.getMetaDataLine("GATKCommandLine.SelectVariants"));

				VCFHeaderLine hlBCFAnnotateVersion = vcfHeader.getMetaDataLine("bcftools_annotateVersion");
				if (hlBCFAnnotateVersion != null) {
					bcftoolsAnnotateVersion = hlBCFAnnotateVersion.getValue();
				} else {
					bcftoolsAnnotateVersion = null;
				}

				VCFHeaderLine hlVepVersion = vcfHeader.getMetaDataLine("VEP");
				if (hlVepVersion != null) {
					Matcher matcher = PATTERN_VEP_VERSION.matcher(hlVepVersion.getValue());
					if (!matcher.matches()) {
						throw new RuntimeException("Not support format VEP version: " + hlVepVersion.getValue());
					}
					vepVersion = matcher.group(1);
				} else {
					vepVersion = null;
				}

			} else {
				pipeline = null;
				reference = null;
				pipelineDate = null;
				toolGatk = null;
				toolGatksApplyRecalibration = null;
				toolGatksCombineVariants = null;
				toolGatkSelectVariants = null;
				bcftoolsAnnotateVersion = null;
				vepVersion = null;
			}

			metadataDatabases = new ArrayList<>();
			metadataDatabases.addAll(anfisaConnector.clinvarConnector.getSourceMetadata());
			metadataDatabases.addAll(anfisaConnector.hgmdConnector.getSourceMetadata());
			metadataDatabases.addAll(anfisaConnector.spliceAIConnector.getSourceMetadata());
			metadataDatabases.addAll(ConservationData.getSourceMetadata());
			metadataDatabases.addAll(anfisaConnector.gnomadConnector.getSourceMetadata());
			metadataDatabases.addAll(anfisaConnector.gtexConnector.getSourceMetadata());
			metadataDatabases.addAll(anfisaConnector.pharmGKBConnector.getSourceMetadata());
			metadataDatabases.sort(Comparator.comparing(o -> o.product));
		}

		private JSONObject toJSON() {
			JSONObject out = new JSONObject();
			if (pipelineDate != null) {
				out.put("pipeline_date", DATE_TIME_FORMATTER.format(pipelineDate));
			}
			out.put("annotations_date", DATE_TIME_FORMATTER.format(Instant.now()));
			out.put("pipeline", pipeline);
			out.put("annotations", annotations);
			out.put("annotations_build", annotationsBuild);
			out.put("reference", reference);
			for (SourceMetadata metadata : metadataDatabases) {
				StringBuilder value = new StringBuilder();
				if (metadata.version != null) {
					value.append(metadata.version);
					if (metadata.date != null) {
						value.append(" | ");
					}
				}
				if (metadata.date != null) {
					value.append(DATE_TIME_FORMATTER.format(metadata.date));
				}
				out.put(metadata.product, value.toString());
			}
			if (!Strings.isNullOrEmpty(toolGatk)) {
				out.put("gatk", toolGatk);
			}
			if (!Strings.isNullOrEmpty(toolGatksApplyRecalibration)) {
				out.put("gatks_apply_recalibration", toolGatksApplyRecalibration);
			}
			if (!Strings.isNullOrEmpty(toolGatksCombineVariants)) {
				out.put("gatks_combine_variants", toolGatksCombineVariants);
			}
			if (!Strings.isNullOrEmpty(toolGatkSelectVariants)) {
				out.put("gatk_select_variants", toolGatkSelectVariants);
			}
			if (!Strings.isNullOrEmpty(bcftoolsAnnotateVersion)) {
				out.put("bcftools_annotate_version", bcftoolsAnnotateVersion);
			}
			if (!Strings.isNullOrEmpty(vepVersion)) {
				out.put("vep_version", vepVersion);
			}
			return out;
		}
	}

	public final String recordType = "metadata";
	public final String caseSequence;
	public final MCase mCase;
	public final Versions versions;

	public AnnotatorResultMetadata(String caseSequence, Path pathVcf, MCase mCase, AnfisaConnector anfisaConnector) {
		this.caseSequence = caseSequence;
		this.mCase = mCase;
		this.versions = new Versions(pathVcf, anfisaConnector);
	}

	public static AnnotatorResultMetadata build(String caseSequence, Path pathVepVcf, MCase mCase, AnfisaConnector anfisaConnector) {
		return new AnnotatorResultMetadata(caseSequence, pathVepVcf, mCase, anfisaConnector);
	}

	public JSONObject toJSON() {
		JSONObject out = new JSONObject();
		out.put("data_schema", DataSchema.CASE.value);
		out.put("case", caseSequence);
		out.put("record_type", recordType);
		out.put("modes", new JSONArray() {{
			if (mCase.assembly == Assembly.GRCh37) {
				add(Mode.HG19.value);
			} else if (mCase.assembly == Assembly.GRCh38) {
				add(Mode.HG38.value);
			} else {
				throw new RuntimeException("Unknown assembly: " + mCase.assembly);
			}
		}});
		out.put("versions", versions.toJSON());
		if (mCase.proband != null) {
			out.put("proband", mCase.proband.id);
		} else {
			out.put("proband", null);
		}
		out.put("samples", new JSONObject() {{
			for (Sample sample : mCase.samples.values()) {
				put(sample.name, build(sample));
			}
		}});
		out.put("cohorts", new JSONArray() {{
			for (Cohort cohort : mCase.cohorts) {
				add(new JSONObject() {{
					put("name", cohort.name);
					put("members", new JSONArray() {{
						for (Sample sample : cohort.getSamples()) {
							add(sample.name);
						}
					}});
				}});
			}
		}});
		return out;
	}

	public static JSONObject build(Sample sample) {
		JSONObject out = new JSONObject();
		out.put("affected", sample.affected);
		out.put("name", sample.name);
		out.put("family", sample.family);
		out.put("father", sample.father);
		if (sample.sex == Sex.UNKNOWN) {
			out.put("sex", 0);
		} else if (sample.sex == Sex.MALE) {
			out.put("sex", 1);
		} else if (sample.sex == Sex.FEMALE) {
			out.put("sex", 2);
		} else {
			throw new RuntimeException("Unknown sex: " + sample.sex);
		}
		out.put("mother", sample.mother);
		out.put("id", sample.id);
		return out;
	}

	private static String getGatkVersion(VCFHeaderLine headerLine) {
		if (headerLine != null) {
			Matcher matcher = Versions.PATTERN_GATK.matcher(headerLine.getValue());
			if (!matcher.matches()) {
				throw new RuntimeException("Not support format GATK version: " + headerLine.getValue());
			}
			String[] values = matcher.group(1).split(",");
			for (String item : values) {
				if (!item.startsWith("Version=")) continue;
				String version = item.substring("Version=".length());
				if (version.charAt(0) == '"' && version.charAt(version.length() - 1) == '"') {
					version = version.substring(1, version.length() - 1);
				}
				return version;
			}
			return null;
		} else {
			return null;
		}
	}
}
