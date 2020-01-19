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

package org.forome.annotation.data.hgmd;

import org.forome.annotation.config.connector.HgmdConfigConnector;
import org.forome.annotation.data.DatabaseConnector;
import org.forome.annotation.data.hgmd.struct.HgmdPmidRow;
import org.forome.annotation.exception.ExceptionBuilder;
import org.forome.annotation.service.database.DatabaseConnectService;
import org.forome.annotation.struct.SourceMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class HgmdConnector implements AutoCloseable {

    private static final String SQL_ACC_NUM = "select acc_num from `hgmd_pro`.`hg19_coords` where chromosome = '%s' and coordSTART = %s and coordEND = %s";
    private static final String SQL_PMID = "SELECT distinct disease, PMID, Tag from `hgmd_pro`.`mutation` where acc_num = '%s'";
    private static final String SQL_PHEN = "SELECT distinct phenotype " +
            "FROM `hgmd_phenbase`.`hgmd_mutation` as m join `hgmd_phenbase`.`hgmd_phenotype` as p on p.phen_id = m.phen_id " +
            "WHERE acc_num = '%s'";
    private static final String SQL_HG38 = "SELECT coordSTART, coordEND FROM `hgmd_pro`.`hg38_coords` WHERE acc_num = '%s'";

    public class Data {
        public final List<HgmdPmidRow> hgmdPmidRows;
        public final List<String> phenotypes;

        public Data(List<HgmdPmidRow> hgmdPmidRows, List<String> phenotypes) {
            this.hgmdPmidRows = hgmdPmidRows;
            this.phenotypes = phenotypes;
        }
    }

    private final DatabaseConnector databaseConnector;

    public HgmdConnector(DatabaseConnectService databaseConnectService, HgmdConfigConnector hgmdConfigConnector) throws Exception {
        this.databaseConnector = new DatabaseConnector(databaseConnectService, hgmdConfigConnector);
    }

    public List<SourceMetadata> getSourceMetadata(){
        return databaseConnector.getSourceMetadata();
    }

    public List<String> getAccNum(String chromosome, long start, long end) {
        List<String> accNums = new ArrayList<>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(String.format(SQL_ACC_NUM, chromosome, start, end))) {
                    while (resultSet.next()) {
                        String accNum = resultSet.getString("acc_num");
                        accNums.add(accNum);
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return accNums;
    }

    public Data getDataForAccessionNumbers(List<String> accNums) {
        List<HgmdPmidRow> hgmdPmidRows = new ArrayList<>();
        List<String> phenotypes = new ArrayList<>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String accNum : accNums) {
                    try (ResultSet resultSet = statement.executeQuery(String.format(SQL_PMID, accNum))) {
                        while (resultSet.next()) {
                            String disease = resultSet.getString("disease");
                            String pMID = resultSet.getString("PMID");
                            String tag = resultSet.getString("Tag");
                            hgmdPmidRows.add(new HgmdPmidRow(
                                    disease, pMID, tag
                            ));
                        }
                    }
                    try (ResultSet resultSet = statement.executeQuery(String.format(SQL_PHEN, accNum))) {
                        while (resultSet.next()) {
                            String phenotype = resultSet.getString("phenotype");
                            phenotypes.add(phenotype);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return new Data(hgmdPmidRows, phenotypes);
    }

    public List<Long[]> getHg38(List<String> accNums) {
        List<Long[]> hg38s = new ArrayList<>();
        try (Connection connection = databaseConnector.createConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String accNum : accNums) {
                    try (ResultSet resultSet = statement.executeQuery(String.format(SQL_HG38, accNum))) {
                        while (resultSet.next()) {
                            Long coordSTART = resultSet.getLong("coordSTART");
                            Long coordEND = resultSet.getLong("coordEND");
                            hg38s.add(new Long[]{coordSTART, coordEND});
                        }
                    }
                }

            }
        } catch (SQLException ex) {
            throw ExceptionBuilder.buildExternalDatabaseException(ex);
        }
        return hg38s;
    }


    @Override
    public void close() {
        databaseConnector.close();
    }
}