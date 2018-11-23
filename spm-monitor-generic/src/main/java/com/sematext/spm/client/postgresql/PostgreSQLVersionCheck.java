/*
 * Licensed to Sematext Group, Inc
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Sematext Group, Inc licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.sematext.spm.client.postgresql;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.db.DbConnectionManager;
import com.sematext.spm.client.db.DbStatsExtractorConfig;
import com.sematext.spm.client.observation.BaseVersionConditionCheck;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostgreSQLVersionCheck extends BaseVersionConditionCheck {
    private static final Log LOG = LogFactory.getLog(PostgreSQLVersionCheck.class);

    @Override
    protected String readVersion() {
        DbStatsExtractorConfig config = (DbStatsExtractorConfig) getExtractorConfig();

        String dbDriverClass = config.getDbDriverClass();
        String dbUrl = config.getDbUrl();
        String dbUser = config.getDbUser();
        String dbPassword = config.getDbPassword();
        String dbAdditionalConnectionParams = config.getDbAdditionalConnectionParams();

        DbConnectionManager dbConnectionManager = DbConnectionManager.getConnectionManager(dbUrl);


        try {
            if (dbConnectionManager == null) {
                dbConnectionManager = new DbConnectionManager(dbUrl, dbDriverClass, dbUser, dbPassword, dbAdditionalConnectionParams);
            }

            Connection conn = dbConnectionManager.getConnection();
            if (conn == null) {
                LOG.error("DB connection not available, skipping query");

                return null;
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW SERVER_VERSION;");

            if (rs.next()) {
                String version = rs.getString(1).trim();

                return getVersion(version);
            }

            return null;
        } catch (Exception e) {
            LOG.error("Error while reading postgresql version", e);

            return null;
        }
    }

    public String getVersion(String version) {
        int[] versions;
        try {
            String versionPart = version.split(" ")[0];
            String[] versionParts = versionPart.split("\\.");

            versions = new int[versionParts.length];
            for(int i = 0; i < versionParts.length; i++) {
                versions[i] = Integer.parseInt(versionParts[i]);
            }

            return versionPart;
        } catch (Exception e) {
            // Postgres might be in development, with format \d+[beta|rc]\d+
            Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z]+)(\\d+)");
            Matcher m = pattern.matcher(version);
            if (m.matches() && m.groupCount() == 3) {
                versions = new int[3];
                versions[0] = Integer.parseInt(m.group(1));
                versions[1] = -1;
                versions[2] = Integer.parseInt(m.group(3));
            } else {
                return null;
            }

            return versions[0] + ".-1." + versions[2]; //TODO check if support 2.-1.3 version (which is < 2.0)
        }
    }
}
