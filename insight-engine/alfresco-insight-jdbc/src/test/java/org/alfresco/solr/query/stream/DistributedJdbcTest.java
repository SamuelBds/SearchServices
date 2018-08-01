/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.solr.query.stream;

import java.util.List;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joel
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedJdbcTest extends AbstractStreamTest
{
    @BeforeClass
    public static void setupJDBC()
    {
        System.setProperty("org.alfresco.search.jdbc.direct", "true");
    }
    @AfterClass
    public static void teardownJDBC()
    {
        System.clearProperty("org.alfresco.search.jdbc.direct");
    }
    @Rule
    public JettyServerRule jetty = new JettyServerRule(1, this, getSolrCoreProps());

    @Test
    public void testSearch() throws Exception
    {
        String sql = "select DBID, LID from alfresco where cm_content = 'world' order by DBID limit 10 ";
        String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";

        Properties props = getConnectionProperties(alfrescoJson);
        String connectionString = getConnectionString();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = DriverManager.getConnection(connectionString, props);
            DatabaseMetaData databaseMetaData = con.getMetaData();

            ResultSet resultSet2 = null;

            try {
                resultSet2 = databaseMetaData.getTables(null, null, null, null);
                ResultSetMetaData resultSetMetaData = resultSet2.getMetaData();
                int columnCount = resultSetMetaData.getColumnCount();
                assertEquals(columnCount, 5);
                assertEquals(resultSetMetaData.getColumnLabel(1), "TABLE_CAT");
                assertEquals(resultSetMetaData.getColumnLabel(2), "TABLE_SCHEM");
                assertEquals(resultSetMetaData.getColumnLabel(3), "TABLE_NAME");
                assertEquals(resultSetMetaData.getColumnLabel(4), "TABLE_TYPE");
                assertEquals(resultSetMetaData.getColumnLabel(5), "REMARKS");
            } finally {
                if(resultSet2 != null) {
                    resultSet2.close();
                }
            }

            try {
                resultSet2 = databaseMetaData.getColumns(null, null, null, null);
                ResultSetMetaData resultSetMetaData = resultSet2.getMetaData();
                int columnCount = resultSetMetaData.getColumnCount();
                assertEquals(columnCount, 19);
                assertEquals(resultSetMetaData.getColumnLabel(1), "TABLE_CAT");
                assertEquals(resultSetMetaData.getColumnLabel(2), "TABLE_SCHEM");
                assertEquals(resultSetMetaData.getColumnLabel(3), "TABLE_NAME");
                assertEquals(resultSetMetaData.getColumnLabel(4), "COLUMN_NAME");
                assertEquals(resultSetMetaData.getColumnLabel(5), "DATA_TYPE");
                assertEquals(resultSetMetaData.getColumnLabel(6), "TYPE_NAME");
                assertEquals(resultSetMetaData.getColumnLabel(7), "COLUMN_SIZE");
                assertEquals(resultSetMetaData.getColumnLabel(8), "BUFFER_LENGTH");
                assertEquals(resultSetMetaData.getColumnLabel(9), "DECIMAL_DIGITS");
                assertEquals(resultSetMetaData.getColumnLabel(10), "NUM_PREC_RADIX");
                assertEquals(resultSetMetaData.getColumnLabel(11), "NULLABLE");
                assertEquals(resultSetMetaData.getColumnLabel(12), "REMARKS");
                assertEquals(resultSetMetaData.getColumnLabel(13), "COLUMN_DEF");
                assertEquals(resultSetMetaData.getColumnLabel(14), "SQL_DATA_TYPE");
                assertEquals(resultSetMetaData.getColumnLabel(15), "SQL_DATETIME_SUB");
                assertEquals(resultSetMetaData.getColumnLabel(16), "CHAR_OCTET_LENGTH");
                assertEquals(resultSetMetaData.getColumnLabel(17), "ORDINAL_POSITION");
                assertEquals(resultSetMetaData.getColumnLabel(18), "IS_NULLABLE");
                assertEquals(resultSetMetaData.getColumnLabel(19), "SCOPE_CATALOG");
            } finally {
                if(resultSet2 != null) {
                    resultSet2.close();
                }
            }

            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            int i=0;
            while (rs.next()) {
                ++i;
                assertNotNull(rs.getString("DBID"));
            }
            assertEquals(i, 4);
        } finally {
            rs.close();
            stmt.close();
            con.close();
        }

        sql = "select cm_name, count(*) from alfresco group by cm_name having (count(*) > 1 AND cm_name = 'bill') order by count(*) asc";
        try {
            try {
                con = DriverManager.getConnection(connectionString, props);
                stmt = con.createStatement();
                rs = stmt.executeQuery(sql);
                int i=0;
                while (rs.next()) {
                    ++i;
                    assertNotNull(rs.getString("DBID"));
                }
                throw new Exception("Exception should have been thrown");
            } finally {
                rs.close();
                stmt.close();
                con.close();
            }
        } catch (Throwable e) {
            if(e.getMessage().equals("Exception should have been thrown")) {
                throw e;
            } else {
                assertTrue(e.getMessage().contains("HAVING clause can only be applied to aggregate functions."));
            }
        } finally {
            rs.close();
            stmt.close();
            con.close();
        }
    }
    
    private String getConnectionString() {
        List<SolrClient> clusterClients = getClusterClients();
        String baseUrl = ((HttpSolrClient) clusterClients.get(0)).getBaseURL();
        String[] parts = baseUrl.split("://");
        String uri = parts[1];
        String[] path = uri.split("/");
        return "jdbc:alfresco://"+path[0]+"?collection="+path[2];
    }

    private Properties getConnectionProperties(String json) {
        List<SolrClient> clusterClients = getClusterClients();
        String shards = getShardsString(clusterClients);
        Properties props = new Properties();
        props.put("json", json);
        props.put("alfresco.shards", shards);
        //Add the basicauth username and passwords required by test framework
        props.put("user", "test");
        props.put("password", "pass");
        return props;
    }

    private Properties getSolrCoreProps() {
        Properties props = new Properties();
        //This tells the test framework to enforce basic auth.
        props.put("BasicAuth", "true");
        return props;
    }

}

