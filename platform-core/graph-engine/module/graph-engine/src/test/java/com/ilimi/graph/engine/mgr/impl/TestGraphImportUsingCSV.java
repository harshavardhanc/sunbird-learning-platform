package com.ilimi.graph.engine.mgr.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.ilimi.graph.common.Request;
import com.ilimi.graph.common.Response;
import com.ilimi.graph.common.dto.Property;
import com.ilimi.graph.common.dto.Params;
import com.ilimi.graph.common.dto.StringValue;
import com.ilimi.graph.common.dto.Params.StatusType;
import com.ilimi.graph.common.enums.GraphEngineParams;
import com.ilimi.graph.common.enums.GraphHeaderParams;
import com.ilimi.graph.dac.enums.GraphDACParams;
import com.ilimi.graph.dac.enums.SystemNodeTypes;
import com.ilimi.graph.dac.enums.SystemProperties;
import com.ilimi.graph.dac.router.GraphDACActorPoolMgr;
import com.ilimi.graph.dac.router.GraphDACManagers;
import com.ilimi.graph.dac.util.Neo4jGraphFactory;
import com.ilimi.graph.engine.router.GraphEngineManagers;
import com.ilimi.graph.engine.router.RequestRouter;
import com.ilimi.graph.enums.ImportType;
import com.ilimi.graph.importer.InputStreamValue;
import com.ilimi.graph.importer.OutputStreamValue;

public class TestGraphImportUsingCSV {

    long timeout = 50000;
    Timeout t = new Timeout(Duration.create(30, TimeUnit.SECONDS));
    String graphId = "LEARNING_MAP_TEST";

    private ActorRef initReqRouter() throws Exception {
        ActorSystem system = ActorSystem.create("MySystem");
        ActorRef reqRouter = system.actorOf(Props.create(RequestRouter.class));

        Future<Object> future = Patterns.ask(reqRouter, "init", timeout);
        Object response = Await.result(future, t.duration());
        Thread.sleep(2000);
        System.out.println("Response from request router: " + response);
        return reqRouter;
    }
    
    private Request getRequest() {
        Request request = new Request();
        request.getContext().put(GraphHeaderParams.GRAPH_ID.name(), graphId);
        request.setManagerName(GraphEngineManagers.GRAPH_MANAGER);
        request.setOperation("importGraph");
        request.put(GraphEngineParams.FORMAT.name(), new StringValue(ImportType.CSV.name()));
        return request;
    }

    private void deleteGraph() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        request.setOperation("deleteGraph");
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
    }
    
    private void testImportDefinitionNodes() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        request.setManagerName(GraphEngineManagers.NODE_MANAGER);
        request.setOperation("importDefinitions");
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/test_definition.json");
        DataInputStream dis = new DataInputStream(inputStream);
        byte[] b = new byte[dis.available()];
        dis.readFully(b);
        request.put(GraphEngineParams.INPUT_STREAM.name(), new StringValue(new String(b)));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
    }
    
    private String getNodeProperty(String uniqueId, String propertyName) throws Exception {
        ActorRef dacRouter = GraphDACActorPoolMgr.getDacRouter();
        Request request = getRequest();
        request.setManagerName(GraphDACManagers.DAC_SEARCH_MANAGER);
        request.setOperation("getNodeProperty");
        request.put(GraphDACParams.NODE_ID.name(), new StringValue(uniqueId));
        request.put(GraphDACParams.PROPERTY_KEY.name(), new StringValue(propertyName));
        Future<Object> req = Patterns.ask(dacRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Property property = (Property) response.get(GraphDACParams.PROPERTY.name());
        String val = (String) property.getPropertyValue();
        return val;
    }
    
    @Test
    public void testForRequiredColumsHandling() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-withoutRequiredColumns.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.ERROR.name(), params.getStatus());
        assertTrue(params.getErrmsg().startsWith("Required columns are missing"));
        deleteGraph();
    }
    
    @Test
    public void testForRequiredDataMissing() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-withoutRequiredData.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.ERROR.name(), params.getStatus());
        assertTrue(params.getErrmsg().startsWith("Required data(uniqueId, objectType) is missing for the row[2]"));
        deleteGraph();
    }
    
    @Test
    public void testForDefinitionNodeMissedValidation() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NoDefinitionNodeCheck.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        OutputStreamValue osV = (OutputStreamValue) response.get(GraphEngineParams.OUTPUT_STREAM.name());
        ByteArrayOutputStream os = (ByteArrayOutputStream) osV.getOutputStream();
        String output = new String(os.toByteArray());
        assertTrue(output.contains("Definition node not found for Object Type: TEST_OBJECT"));
        deleteGraph();
    }
    
    @Test
    public void testForNodesAndRelationsCount() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NodesAndRelationsCount.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
        GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId);
        Transaction tx = graphDb.beginTx();
        GlobalGraphOperations globalOps = GlobalGraphOperations.at(graphDb);
        int nodesCount = IteratorUtil.count(globalOps.getAllNodes().iterator());
        int relationsCount = IteratorUtil.count(globalOps.getAllRelationships().iterator());
        assertEquals(3, nodesCount); // root node + importedObjects.
        assertEquals(1, relationsCount); // only isParentOf relation.
        tx.success(); tx.close();
        deleteGraph();
    }

    @Test
    public void testForTagsCount() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NodesAndTags.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        Thread.sleep(15000);
        GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId);
        Transaction tx = graphDb.beginTx();
        GlobalGraphOperations globalOps = GlobalGraphOperations.at(graphDb);
        int tagsCount = 0;
        ResourceIterator<Node> nodes = globalOps.getAllNodes().iterator();
        while(nodes.hasNext()) {
            Node node = nodes.next();
            if(SystemNodeTypes.TAG.name().equals(node.getProperty(SystemProperties.IL_SYS_NODE_TYPE.name()))) {
                tagsCount++;
            }
        }
        assertEquals(2, tagsCount);
        tx.success(); tx.close();
        deleteGraph();
    }
    
    @Test
    public void testForUpdateNodeMetadata() throws Exception {
        testImportDefinitionNodes();
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NodesAndTags.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
        String description = getNodeProperty("Num:C1", "description");
        System.out.println("Description:"+description);
        assertEquals("Geometry", description);
        
        request = getRequest();
        inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-UpdatedNode.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        req = Patterns.ask(reqRouter, request, t);
        obj = Await.result(req, t.duration());
        response = (Response) obj;
        params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
        description = getNodeProperty("Num:C1", "description");
        assertEquals("Desc of Geometry", description);
        deleteGraph();
    }
    
    @Test
    public void testForUpdateRelationship() throws Exception {
        testImportDefinitionNodes();
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NodesAndTags.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
        GraphDatabaseService graphDb = Neo4jGraphFactory.getGraphDb(graphId);
        Transaction tx = graphDb.beginTx();
        GlobalGraphOperations globalOps = GlobalGraphOperations.at(graphDb);
        Iterator<Relationship> relations = globalOps.getAllRelationships().iterator();
        Relationship expRelation = null;
        while(relations.hasNext()) {
            Relationship relation = relations.next();
            String nodeId = (String) relation.getStartNode().getProperty(SystemProperties.IL_UNIQUE_ID.name());
            if("Num".equals(nodeId)) {
                expRelation = relation;
                break;
            }
        }
        String endNodeId = (String) expRelation.getEndNode().getProperty(SystemProperties.IL_UNIQUE_ID.name());
        assertEquals("Num:C1", endNodeId);
        tx.success(); tx.close();
        
        request = getRequest();
        inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-UpdatedRelationship.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        req = Patterns.ask(reqRouter, request, t);
        obj = Await.result(req, t.duration());
        response = (Response) obj;
        params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
        tx = graphDb.beginTx();
        while(relations.hasNext()) {
            Relationship relation = relations.next();
            String nodeId = (String) relation.getStartNode().getProperty(SystemProperties.IL_UNIQUE_ID.name());
            if("Num".equals(nodeId)) {
                expRelation = relation;
                break;
            }
        }
        endNodeId = (String) expRelation.getEndNode().getProperty(SystemProperties.IL_UNIQUE_ID.name());
        assertEquals("Num:C2", endNodeId);
        tx.success(); tx.close();
        
        deleteGraph();
    }
    
    @Test
    public void testForDefinitionNodeUpdate() throws Exception {
        ActorRef reqRouter = initReqRouter();
        Request request = getRequest();
        request.setManagerName(GraphEngineManagers.NODE_MANAGER);
        request.setOperation("importDefinitions");
        InputStream inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/test_definition.json");
        DataInputStream dis = new DataInputStream(inputStream);
        byte[] b = new byte[dis.available()];
        dis.readFully(b);
        request.put(GraphEngineParams.INPUT_STREAM.name(), new StringValue(new String(b)));
        Future<Object> req = Patterns.ask(reqRouter, request, t);
        Object obj = Await.result(req, t.duration());
        Response response = (Response) obj;
        Params params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        
        Thread.sleep(10000);
        
        request = getRequest();
        inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NodesAndTags.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        req = Patterns.ask(reqRouter, request, t);
        obj = Await.result(req, t.duration());
        response = (Response) obj;
        params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        OutputStreamValue osV = (OutputStreamValue) response.get(GraphEngineParams.OUTPUT_STREAM.name());
        ByteArrayOutputStream os = (ByteArrayOutputStream) osV.getOutputStream();
        String output = new String(os.toByteArray());
        assertFalse(output.contains("Required Metadata description not set"));
        
        request = getRequest();
        request.setManagerName(GraphEngineManagers.NODE_MANAGER);
        request.setOperation("importDefinitions");
        inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/test_definition_updated.json");
        dis = new DataInputStream(inputStream);
        b = new byte[dis.available()];
        dis.readFully(b);
        request.put(GraphEngineParams.INPUT_STREAM.name(), new StringValue(new String(b)));
        req = Patterns.ask(reqRouter, request, t);
        obj = Await.result(req, t.duration());
        response = (Response) obj;
        params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());

        request = getRequest();
        inputStream = GraphMgrTest.class.getClassLoader().getResourceAsStream("testCSVs/CSV-NodesAndTags.csv");
        request.put(GraphEngineParams.INPUT_STREAM.name(), new InputStreamValue(inputStream));
        req = Patterns.ask(reqRouter, request, t);
        obj = Await.result(req, t.duration());
        response = (Response) obj;
        params = response.getParams();
        assertEquals(StatusType.SUCCESS.name(), params.getStatus());
        osV = (OutputStreamValue) response.get(GraphEngineParams.OUTPUT_STREAM.name());
        os = (ByteArrayOutputStream) osV.getOutputStream();
        output = new String(os.toByteArray());
        assertTrue(output.contains("Required Metadata description not set"));
        
        deleteGraph();
        
        
    }
}
