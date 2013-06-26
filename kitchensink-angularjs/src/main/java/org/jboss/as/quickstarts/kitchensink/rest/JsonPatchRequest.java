package org.jboss.as.quickstarts.kitchensink.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.google.common.base.Preconditions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Matt Drees
 */
public class JsonPatchRequest {

    final Logger log;

    final ObjectMapper mapper;

    final JsonPatch jsonPatch;

    public JsonPatchRequest(Logger log, ObjectMapper mapper, String jsonPatchText) {
        Preconditions.checkNotNull(jsonPatchText);
        this.log = log;
        this.mapper = mapper;
        this.jsonPatch = parseAndBuildPatch(jsonPatchText);
    }

    public <T> T apply(T originalEntity) {
        Preconditions.checkNotNull(originalEntity);

        JsonNode entityAsJson = mapper.valueToTree(originalEntity);
        JsonNode updatedEntityAsJson = applyPatch(jsonPatch, entityAsJson);

        // This is safe enough
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) originalEntity.getClass();
        return convertUpdatedJsonBackToEntity(entityClass, updatedEntityAsJson);
    }

    private JsonPatch parseAndBuildPatch(String jsonPatchText) {
        JsonNode jsonNode = parseJson(jsonPatchText);
        return buildPatch(jsonNode);
    }

    private JsonNode parseJson(String jsonPatchText) {
        JsonNode jsonNode;
        try {
            jsonNode = JsonLoader.fromString(jsonPatchText);
        } catch (JsonProcessingException e) {
            log.fine("cannot parse json:\n" + jsonPatchText);
            throw badPatchException(e);
        } catch (IOException e) {
            //we're reading from a String, so I don't think this will happen.
            throw new RuntimeException("unexpected IO exception", e);
        }
        return jsonNode;
    }

    private JsonPatch buildPatch(JsonNode jsonNode) {
        JsonPatch jsonPatch;
        try {
            jsonPatch = JsonPatch.fromJson(jsonNode);
        } catch (JsonProcessingException e) {
            log.fine("cannot parse jsonNode into a JsonPatch:\n" + jsonNode);
            throw badPatchException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return jsonPatch;
    }

    private WebApplicationException badPatchException(Exception e) {
        return new WebApplicationException(
            e,
            Response
                .status(Response.Status.BAD_REQUEST)
                .entity("unable to parse json patch: " + e.getMessage())
                .build());
    }

    private JsonNode applyPatch(JsonPatch jsonPatch, JsonNode entityAsJson) {
        JsonNode updatedEntityAsJson;
        try {
            updatedEntityAsJson = jsonPatch.apply(entityAsJson);
        } catch (JsonPatchException e) {
            log.fine("cannot apply json patch:\n" + jsonPatch + "\n to:\n" + entityAsJson);
            String documentPrintedNicely = nicelyPrintDocumentForErrorMessage(entityAsJson);
            throw new WebApplicationException(
                e,
                Response
                    .status(Response.Status.CONFLICT)
                        // JsonPatchException messages don't contain much context info, so we'll add a little bit
                    .entity("unable to apply json patch: " + e.getMessage() + "\n" +
                            "patch: \n" + jsonPatch + "\n" +
                            "target document: \n" + documentPrintedNicely)
                    .build());
        }
        return updatedEntityAsJson;
    }

    private String nicelyPrintDocumentForErrorMessage(JsonNode entityAsJson) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entityAsJson);
        } catch (JsonProcessingException e1) {
            // this generally shouldn't happen, and we don't want to mask the original problem to the user, so
            // log and swallow
            log.log(Level.SEVERE, "unable to pretty-print entity", e1);
            return "<unavailable due to internal error, sorry>";
        }
    }

    private <T> T  convertUpdatedJsonBackToEntity(Class<T> originalEntityClass, JsonNode updatedEntityAsJson) {
        T updatedEntity;
        try {
            updatedEntity = mapper.treeToValue(updatedEntityAsJson, originalEntityClass);
        } catch (JsonProcessingException e) {
            log.fine("cannot convert updated json back into entity :\n" + updatedEntityAsJson);
            throw new WebApplicationException(
                e,
                Response
                    .status(new UnprocessableEntityStatusType())
                    .entity("unable to apply json patch to resource: " + e.getMessage())
                    .build());
        }
        return updatedEntity;
    }

    @Override
    public String toString() {
        return jsonPatch.toString();
    }
}
