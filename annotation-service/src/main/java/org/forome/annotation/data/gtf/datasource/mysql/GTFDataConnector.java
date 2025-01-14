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

package org.forome.annotation.data.gtf.datasource.mysql;

import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.anfisa.struct.AnfisaExecuteContext;
import org.forome.annotation.data.gtf.datasource.GTFDataSource;
import org.forome.annotation.data.gtf.mysql.struct.GTFResult;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRow;
import org.forome.annotation.data.gtf.mysql.struct.GTFTranscriptRowExternal;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.struct.variant.Variant;
import org.forome.core.struct.Assembly;
import org.forome.core.struct.Position;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class GTFDataConnector implements GTFDataSource {

	private static final long GENE_BUCKET_SIZE = 1000000L;

	private final DatabaseConnector databaseConnector;

	public GTFDataConnector(DatabaseConnector databaseConnector) {
		this.databaseConnector = databaseConnector;
	}

	public GTFResult getGene(Assembly assembly, String chromosome, long position) {
		long bucket = (position / GENE_BUCKET_SIZE) * GENE_BUCKET_SIZE;

		String sql = String.format(
				"SELECT gene FROM %s.GTF_gene WHERE chromosome = %s AND bucket = %s AND %s between `start` and `end`",
				getDatabase(assembly),
				chromosome, bucket, position
		);

		String symbol = null;
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					if (resultSet.next()) {
						symbol = resultSet.getString(1);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return new GTFResult(symbol);
	}

	public List<GTFTranscriptRow> getTranscriptRows(Assembly assembly, String transcript) {
		String sql = String.format(
				"SELECT `gene`, `start`, `end`, `feature` from %s.GTF WHERE transcript = '%s' AND feature = 'exon' ORDER BY `start`, `end`",
				getDatabase(assembly),
				transcript
		);

		List<GTFTranscriptRow> rows = new ArrayList<GTFTranscriptRow>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String gene = resultSet.getString("gene");
						int start = resultSet.getInt("start");
						int end = resultSet.getInt("end");
						String feature = resultSet.getString("feature");
						rows.add(new GTFTranscriptRow(
								gene,
								start,
								end,
								feature
						));
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return rows;
	}

	public List<GTFTranscriptRowExternal> getTranscriptRowsByChromosomeAndPositions(Assembly assembly, String chromosome, long[] positions) {

		String sqlWherePosition = Arrays.stream(positions)
				.mapToObj(position -> String.format("(`start` < %s and %s < `end`)", position, position))
				.collect(Collectors.joining(" or ", "(", ")"));

		String sql = String.format(
				"SELECT `transcript`, `gene`, `approved`, `start`, `end`, `feature` from %s.GTF WHERE feature IN ('transcript') and chromosome = '%s' and %s" +
						" ORDER BY `start`, `end`",
				getDatabase(assembly),
				chromosome, sqlWherePosition
		);

		List<GTFTranscriptRowExternal> rows = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String transcript = resultSet.getString("transcript");
						String gene = resultSet.getString("gene");
						String approved = resultSet.getString("approved");
						int start = resultSet.getInt("start");
						int end = resultSet.getInt("end");
						String feature = resultSet.getString("feature");
						rows.add(new GTFTranscriptRowExternal(
								transcript, gene, approved,
								start, end, feature
						));
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return rows;
	}


	public List<String> getTranscriptsByChromosomeAndPositions(Assembly assembly, String chromosome, long[] positions) {
		String sqlWherePosition = Arrays.stream(positions)
				.mapToObj(position -> String.format("(`start` < %s and %s < `end`)", position, position))
				.collect(Collectors.joining(" or ", "(", ")"));

		String sql = String.format(
				"SELECT `transcript` from %s.GTF WHERE feature IN ('transcript') and chromosome = '%s' and %s" +
						" ORDER BY `start`, `end`",
				getDatabase(assembly),
				chromosome, sqlWherePosition
		);

		List<String> transcripts = new ArrayList<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String transcript = resultSet.getString("transcript");

						if (!transcripts.contains(transcript)) {
							transcripts.add(transcript);
						}
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}
		return transcripts;
	}

	@Override
	public List<GTFTranscriptRow> lookup(AnfisaExecuteContext context, Assembly assembly, Position position, String transcript) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Set<String> getCdsTranscript(Assembly assembly, Variant variant) {
		int start = Math.min(variant.getStart(), variant.end);
		int end = Math.max(variant.getStart(), variant.end);
		String sql = String.format(
				"select transcript from %s.GTF where feature = 'CDS' and " +
						"chromosome = '%s' and " +
						"((`start` <= %s and %s <= `end`) or (`start` <= %s and %s <= `end`))",
				getDatabase(assembly),
				variant.chromosome.getChar(),
				start, start,
				end, end
		);

		Set<String> transcripts = new HashSet<>();
		try (Connection connection = databaseConnector.createConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						String transcript = resultSet.getString("transcript");
						transcripts.add(transcript);
					}
				}
			}
		} catch (SQLException ex) {
			throw ExceptionBuilder.buildExternalDatabaseException(ex);
		}

		return transcripts;
	}

	@Override
	public void close() {
		databaseConnector.close();
	}

	private static String getDatabase(Assembly assembly) {
		if (assembly == Assembly.GRCh37) {
			return "ensembl";
		} else if (assembly == Assembly.GRCh38) {
			return "ensembl_hg38";
		} else {
			throw new RuntimeException("Unknown assembly: " + assembly);
		}
	}
}
