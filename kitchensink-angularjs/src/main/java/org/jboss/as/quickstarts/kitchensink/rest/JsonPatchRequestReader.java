package org.jboss.as.quickstarts.kitchensink.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Logger;

/**
 * @author Matt Drees
 */
@Provider
@Consumes("application/json-patch")
public class JsonPatchRequestReader implements MessageBodyReader<JsonPatchRequest>{

    @Context
    Providers providers;

    //TODO: integrate with the the ObjectMapper used by the jax-rs implementation
    ObjectMapper mapper = new ObjectMapper()
            .setNodeFactory(JacksonUtils.nodeFactory())
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {

        MessageBodyReader<String> stringReader = providers.getMessageBodyReader(String.class, String.class, annotations, mediaType);

        return JsonPatchRequest.class.isAssignableFrom(type) &&
                stringReader.isReadable(String.class, String.class, annotations, mediaType);
    }

    @Override
    public JsonPatchRequest readFrom(
            Class<JsonPatchRequest> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException {

        MessageBodyReader<String> stringReader = providers.getMessageBodyReader(String.class, String.class, annotations, mediaType);
        String contentAsString = stringReader.readFrom(String.class, String.class, annotations, mediaType, httpHeaders, entityStream);
        return buildJsonPatchRequest(contentAsString);
    }

    // public for testing
    public JsonPatchRequest buildJsonPatchRequest(String contentAsString) {
        return new JsonPatchRequest(Logger.getLogger(JsonPatchRequest.class.getName()), mapper, contentAsString);
    }
}
