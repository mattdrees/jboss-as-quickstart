package org.jboss.as.quickstarts.kitchensink.rest;

import javax.ws.rs.core.Response;

/**
 * see <a href="http://tools.ietf.org/html/rfc4918#section-11.2">WebDAV specification section 11.2 (definition for "Unprocessable Entity")</a>
 *
 * @author Matt Drees
 */
public class UnprocessableEntityStatusType implements Response.StatusType {
    @Override
    public int getStatusCode() {
        return 422;
    }

    @Override
    public Response.Status.Family getFamily() {
        return Response.Status.Family.CLIENT_ERROR;
    }

    @Override
    public String getReasonPhrase() {
        return "Unprocessable Entity";
    }
}
