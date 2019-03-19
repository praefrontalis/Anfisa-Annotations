package org.forome.annotation.struct;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class Variant {

	public static final ImmutableList<String> CSQ_DAMAGING = ImmutableList.of(
			"transcript_ablation",
			"splice_acceptor_variant",
			"splice_donor_variant",
			"stop_gained",
			"frameshift_variant",
			"stop_lost",
			"start_lost",
			"transcript_amplification",
			"inframe_insertion",
			"inframe_deletion"
	);

	public static final ImmutableList<String> CSQ_MISSENSE = ImmutableList.of(
			"missense_variant"
	);

	public static final ImmutableList<String> CSQ_BENIGN1 = ImmutableList.of(
			"splice_region_variant",
			"synonymous_variant"
			);

	public static final ImmutableList<String> CSQ_BENIGN2 = ImmutableList.of(
			"5_prime_UTR_variant",
			"3_prime_UTR_variant",
			"non_coding_transcript_exon_variant",
			"non_coding_transcript_exon_variant",
			"intron_variant",
			"upstream_gene_variant",
			"downstream_gene_variant",
			"regulatory_region_variant"
	);

	public static final ImmutableList<List<String>> SEVERITY = ImmutableList.of(
			CSQ_DAMAGING, CSQ_MISSENSE, CSQ_BENIGN1, CSQ_BENIGN2
	);

	public static final ImmutableList<String> CONSEQUENCES = ImmutableList.copyOf(
			SEVERITY.stream().flatMap(strings -> strings.stream()).iterator()
	);

	public static final ImmutableList<String> CALLERS = ImmutableList.of(
			"BGM_AUTO_DOM", "BGM_DE_NOVO", "BGM_HOM_REC", "BGM_CMPD_HET",
			"BGM_BAYES_DE_NOVO", "BGM_BAYES_CMPD_HET", "BGM_BAYES_HOM_REC",
			"BGM_PIPELINE_A", "BGM_PIPELINE", "LMM", "SANGER"
	);
}
