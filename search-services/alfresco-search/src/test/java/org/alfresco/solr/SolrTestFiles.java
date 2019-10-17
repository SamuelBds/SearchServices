/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr;
/**
 * A trait to mixin the location of the test files used in solr test.
 * @author Michael Suzuki
 *
 */
public interface SolrTestFiles
{
    String TEST_FILES_LOCATION = "target/test-classes/test-files";
    String TEST_EXECUTION_FOLDER = "target/test-execution-folder";
    String TEST_EXECUTION_SOLRHOME = TEST_EXECUTION_FOLDER + "/solrhome";
    String TEST_SOLR_COLLECTION = TEST_EXECUTION_SOLRHOME + "/collection1";
    String TEST_SOLR_CONF = TEST_SOLR_COLLECTION + "/conf/";
    String TEMPLATE_CONF = TEST_EXECUTION_SOLRHOME + "/templates/%s/conf/";
}
