package org.alfresco.rest;

import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.ServerHealth;
import org.alfresco.utility.TasProperties;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

@ContextConfiguration("classpath:alfresco-restapi-context.xml")
public abstract class RestTest extends AbstractTestNGSpringContextTests
{
    @Autowired
    protected RestProperties restProperties;

    @Autowired
    protected TasProperties properties;

    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected RestWrapper restClient;
    
    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;
    
    @Autowired
    DataContent dataContent;

    @BeforeClass(alwaysRun = true)
    public void setupRestTest() throws Exception
    {
        serverHealth.assertServerIsOnline();

        RestAssured.baseURI = restProperties.envProperty().getTestServerUrl();
        RestAssured.port = restProperties.envProperty().getPort();
        RestAssured.basePath = restProperties.getRestBasePath();
    }
}
