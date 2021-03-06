/*
 * Copyright (C) 2005-2020 Alfresco Software Limited.
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
package org.alfresco.solr.tracker;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

import java.util.Properties;

/**
 * Test environment having initial Transaction Id greater than default range 0-2000
 * @author aborroy
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedAlfrescoSolrTrackerLargeTxnIdIT extends AbstractAlfrescoDistributedIT
{

    @BeforeClass
    private static void initData() throws Throwable
    {
        Properties properties = new Properties();
        // Set a wider range for Initial Transaction Id
        properties.put("solr.initial.transaction.range", "0-" + Long.MAX_VALUE);
        initSolrServers(2, "DistributedAlfrescoSolrTrackerLargeTxnTest", properties);
    }

    @AfterClass
    private static void destroyData()
    {
        dismissSolrServers();
    }
    
    @Test
    public void testTracker() throws Exception
    {
        putHandleDefaults();

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        // Transaction greater than 2000 is used, to check that "solr.initial.transaction.range" works as expected
        Transaction txn = getTransaction(0, 2);
        long txnCommitTimeMs = txn.getCommitTimeMs();

        // Subtract from the commit time to go beyond hole retention
        long backdatedCommitTimeMs = txnCommitTimeMs - 4600000;
        txn.setCommitTimeMs(backdatedCommitTimeMs);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node errorNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        // Next, create the node metadata for each node.
        // Note: the error node metadata will cause an exception.
        NodeMetaData folderMetaData = getNodeMetaData(folderNode, txn, acl, "mike", null, false);
        NodeMetaData fileMetaData   = getNodeMetaData(fileNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData errorMetaData   = getNodeMetaData(errorNode, txn, acl, "lisa", ancestors(folderMetaData.getNodeRef()), true);

        // Index the transaction, nodes, and nodeMetaDatas.
        // Note that the content is automatically created by the test framework.
        indexTransaction(txn, asList(errorNode, folderNode, fileNode), asList(errorMetaData, folderMetaData, fileMetaData));
        indexAclChangeSet(aclChangeSet, asList(acl, acl2), asList(aclReaders, aclReaders2));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

   }
}