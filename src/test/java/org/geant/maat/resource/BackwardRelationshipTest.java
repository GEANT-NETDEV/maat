package org.geant.maat.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geant.maat.utils.RelationshipHelper;
import org.geant.maat.utils.ResourceHttpClient;
import org.geant.maat.utils.ResourceReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static com.jayway.jsonpath.JsonPath.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
@DirtiesContext
@TestPropertySource(properties = {"resourceService.type=extended", "resourceService.checkExistingResource=true"})
class BackwardRelationshipTest extends HttpTest{

    private final ObjectMapper mapper = new ObjectMapper();
    private final ResourceHttpClient client = new ResourceHttpClient();
    private final String example_resource_clear = ResourceReader.getStringResource("resourceClear.json", "schema.json");
    private final RelationshipHelper relationshipHelper=new RelationshipHelper();
    @Test
    @DisplayName("When adding new resource with bref: relation to existing resource")
    void createWithBrefToClear() throws JsonProcessingException {
        var response1 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response1.statusCode(), HttpStatus.CREATED.value());

        String href1 = parse(response1.body()).read("$.href");
        String id1 = parse(response1.body()).read("$.id");

        JsonNode new_resource=relationshipHelper.addRelationshipToResource("bref:", "testRelation", href1, example_resource_clear);
        var response2 = client.sendPost(resourceUrlNoSlash(), String.valueOf(new_resource));
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String href2 = parse(response2.body()).read("$.href");
        JsonNode response2Body=mapper.readTree(response2.body());


        var updatedResponse1=client.sendGetAndParse(resourceUrl() + id1);
        assertEquals(updatedResponse1.get("resourceRelationship"), relationshipHelper.addRelationshipToResource("ref:", "testRelation", href2, response1.body()).get("resourceRelationship"));
        assertEquals(new_resource.get("resourceRelationship"), response2Body.get("resourceRelationship"));
    }

    @Test
    @DisplayName("When adding new resource with none relation to existing resource")
    void createWithNoneToClear() throws JsonProcessingException {
        var response1 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response1.statusCode(), HttpStatus.CREATED.value());

        String href1 = parse(response1.body()).read("$.href");
        String id1 = parse(response1.body()).read("$.id");

        JsonNode new_resource=relationshipHelper.addRelationshipToResource("", "testRelation", href1, example_resource_clear);
        var response2 = client.sendPost(resourceUrlNoSlash(), String.valueOf(new_resource));
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        JsonNode response2Body=mapper.readTree(response2.body());
        JsonNode response1Body=mapper.readTree(response1.body());


        var updatedResponse1=client.sendGetAndParse(resourceUrl() + id1);
        assertEquals(updatedResponse1.get("resourceRelationship"), response1Body.get("resourceRelationship"));
        assertEquals(new_resource.get("resourceRelationship"), response2Body.get("resourceRelationship"));
    }

    @Test
    @DisplayName("When adding new resource with bref: relation to no existing resource with checking return")
    void createWithBrefToNoExistingCheck(){
        var response = client.sendPost(resourceUrl(), ResourceReader.getStringResource("resourceNoExisting.json", "schema.json"));
        assertEquals(response.statusCode(), HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("When updating resource with bref:relation")
    void updateBrefRelation() {
        var response1 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response1.statusCode(), HttpStatus.CREATED.value());

        String href1 = parse(response1.body()).read("$.href");
        String id1 = parse(response1.body()).read("$.id");


        JsonNode new_resource = relationshipHelper.addRelationshipToResource("bref:", "testRelation", href1, example_resource_clear);
        var response2 = client.sendPost(resourceUrlNoSlash(), String.valueOf(new_resource));
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String href2 = parse(response2.body()).read("$.href");
        String id2 = parse(response2.body()).read("$.id");



        String updateJson = relationshipHelper.updateResourceWithRelation("bref:", "testRelation2", href2, response1.body());
        var updatedResponse1 = client.sendPatchAndParse(resourceUrl()+id1, updateJson);

        assertEquals(updatedResponse1.get("resourceRelationship"),relationshipHelper.addRelationshipToResource("bref:", "testRelation2", href2, response1.body()).get("resourceRelationship"));
        assertEquals(client.sendGetAndParse(resourceUrl()+id2).get("resourceRelationship"), relationshipHelper.addOnlyOneRelationshipToResource("ref:", "testRelation2", href1, response2.body()).get("resourceRelationship"));

    }
    @Test
    @DisplayName("When updating resource with ref:relation")
    void updateRefRelation() {
        var response1 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response1.statusCode(), HttpStatus.CREATED.value());

        String href1 = parse(response1.body()).read("$.href");
        String id1 = parse(response1.body()).read("$.id");

        var response2 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String href2 = parse(response2.body()).read("$.href");
        String id2 = parse(response2.body()).read("$.id");

        String updateJson = relationshipHelper.updateResourceWithRelation("ref:", "testRelation1", href2, response1.body());
        var updatedResponse1 = client.sendPatchAndParse(resourceUrl()+id1, updateJson);

        assertEquals(updatedResponse1.get("resourceRelationship"),relationshipHelper.addRelationshipToResource("ref:", "testRelation1", href2, response1.body()).get("resourceRelationship"));
        assertEquals(client.sendGetAndParse(resourceUrl()+id2).get("resourceRelationship"), relationshipHelper.addRelationshipToResource("bref:", "testRelation1", href1, response2.body()).get("resourceRelationship"));
    }

    @Test
    @DisplayName("When updating resource with bref:relation and ref:relation")
    void updateBrefAndRefRelation() {
        var response1 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response1.statusCode(), HttpStatus.CREATED.value());

        String href1 = parse(response1.body()).read("$.href");
        String id1 = parse(response1.body()).read("$.id");

        var response2 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String href2 = parse(response2.body()).read("$.href");
        String id2 = parse(response2.body()).read("$.id");

        String updateJson = relationshipHelper.updateResourceWithRelation("ref:", "testRelation1", href2, response1.body());
        JsonNode new_resource=relationshipHelper.addRelationshipToResource("bref:", "testRelation2", href2, updateJson);
        var updatedResponse1=client.sendPatchAndParse(resourceUrl()+id1, new_resource.toString());


        String update2Json = relationshipHelper.updateResourceWithRelation("bref:", "testRelation1", href1, response2.body());
        JsonNode new_resource2=relationshipHelper.addRelationshipToResource("ref:", "testRelation2", href1, update2Json);

        assertEquals(new_resource.get("resourceRelationship"),updatedResponse1.get("resourceRelationship"));
        assertEquals(new_resource2.get("resourceRelationship"), client.sendGetAndParse(resourceUrl()+id2).get("resourceRelationship"));
    }

    @Test
    @DisplayName("When updating resource with other relation")
    void updateOtherRelation() throws JsonProcessingException {
        var response1 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response1.statusCode(), HttpStatus.CREATED.value());

        String id1 = parse(response1.body()).read("$.id");

        var response2 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String href2 = parse(response2.body()).read("$.href");
        String id2 = parse(response2.body()).read("$.id");
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String updateJson = relationshipHelper.updateResourceWithRelation("", "testRelation", href2, response1.body());
        var updatedResponse1 = client.sendPatchAndParse(resourceUrl()+id1, updateJson);

        JsonNode updateJsonBody=mapper.readTree(updateJson);
        JsonNode response2Body=mapper.readTree(response2.body());

        assertEquals(updateJsonBody.get("resourceRelationship"), updatedResponse1.get("resourceRelationship"));
        assertEquals(response2Body.get("resourceRelationship"), client.sendGetAndParse(resourceUrl()+id2).get("resourceRelationship"));
    }

    //When deleting resource with bref:relation
    @Test
    @DisplayName("When deleting resource with bref:relation")
    void deleteWithBrefRelation() {
        var response1 = client.sendPostAndParse(resourceUrlNoSlash(), example_resource_clear);

        String href1 = response1.get("href").textValue();
        String id1 = response1.get("id").textValue();

        JsonNode new_resource=relationshipHelper.addRelationshipToResource("bref:", "testRelation", href1, example_resource_clear);
        var response2 = client.sendPostAndParse(resourceUrlNoSlash(), String.valueOf(new_resource));

        String href2 = response2.get("href").textValue();
        String id2 = response2.get("id").textValue();

        JsonNode res1updated=relationshipHelper.addRelationshipToResource("ref:", "testRelation",href2, response1.toString());
        assertEquals(new_resource.get("resourceRelationship"), response2.get("resourceRelationship"));
        assertEquals(res1updated.get("resourceRelationship"), client.sendGetAndParse(resourceUrl()+id1).get("resourceRelationship"));

        var responseDelete=client.sendDelete(resourceUrl()+id2);
        assertEquals(HttpStatus.NO_CONTENT.value(), responseDelete.statusCode());
        assertEquals(response1.get("resourceRelationship"), client.sendGetAndParse(resourceUrl()+id1).get("resourceRelationship"));
    }

    @Test
    @DisplayName("When deleting resource with bref:relation and ref:relation")
    void deleteWithBrefandRefRelation() {
        var response1 = client.sendPostAndParse(resourceUrlNoSlash(), example_resource_clear);

        String href1 = response1.get("href").textValue();
        String id1 = response1.get("id").textValue();

        var response2 = client.sendPost(resourceUrlNoSlash(), example_resource_clear);
        assertEquals(response2.statusCode(), HttpStatus.CREATED.value());

        String href2 = parse(response2.body()).read("$.href");
        String id2 = parse(response2.body()).read("$.id");

        String updateJson = relationshipHelper.updateResourceWithRelation("ref:", "testRelation1", href2, response1.toString());
        JsonNode new_resource=relationshipHelper.addRelationshipToResource("bref:", "testRelation2", href2, updateJson);
        var updatedResponse1=client.sendPatchAndParse(resourceUrl()+id1, new_resource.toString());

        String update2Json = relationshipHelper.updateResourceWithRelation("bref:", "testRelation1", href1, response2.body());
        JsonNode new_resource2=relationshipHelper.addRelationshipToResource("ref:", "testRelation2", href1, update2Json);

        assertEquals(new_resource.get("resourceRelationship"),updatedResponse1.get("resourceRelationship"));
        assertEquals(new_resource2.get("resourceRelationship"), client.sendGetAndParse(resourceUrl()+id2).get("resourceRelationship"));

        var responseDelete=client.sendDelete(resourceUrl()+id2);
        assertEquals(HttpStatus.NO_CONTENT.value(), responseDelete.statusCode());
        assertEquals(response1.get("resourceRelationship"), client.sendGetAndParse(resourceUrl()+id1).get("resourceRelationship"));
    }


}
