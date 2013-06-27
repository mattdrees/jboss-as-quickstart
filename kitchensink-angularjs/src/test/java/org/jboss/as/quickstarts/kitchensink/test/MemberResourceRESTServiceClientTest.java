/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.kitchensink.test;

import com.google.common.collect.Iterables;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.rest.JaxRsActivator;
import org.jboss.as.quickstarts.kitchensink.rest.JsonPatchRequest;
import org.jboss.as.quickstarts.kitchensink.rest.MemberResourceRESTService;
import org.jboss.as.quickstarts.kitchensink.rest.PATCH;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
import org.jboss.as.quickstarts.kitchensink.util.Resources;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GenericType;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.UserTransaction;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@RunAsClient
public class MemberResourceRESTServiceClientTest {

    @Deployment
    public static Archive<?> createTestArchive() {

        PomEquippedResolveStage resolver = Maven.resolver().offline().loadPomFromFile("pom.xml");
        
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(
                        Member.class,
                        MemberRepository.class,
                        MemberRegistration.class,
                        Resources.class)
                .addPackage(MemberResourceRESTService.class.getPackage())
                .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
                .addAsResource("import.sql", "import.sql")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                // Deploy our test datasource
                .addAsWebInfResource("test-ds.xml")
                .addAsLibraries(resolver
                    .resolve("com.github.fge:json-patch")
                    .withTransitivity()
                    .asFile());
    }

    @ArquillianResource
    URL deploymentUrl;


    @Path("/members")
    public static interface MemberResourceRESTServiceClient
    {

        @GET
        @Path("/{id:[0-9][0-9]*}")
        @Produces(MediaType.APPLICATION_JSON)
        public ClientResponse<Member> lookupMemberById(@PathParam("id") long id) ;

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createMember(Member member) ;

        @PATCH
        @Consumes("application/json-patch")
        @Path("/{id:[0-9][0-9]*}")
        public Response patchMember(@PathParam("id") long id, String patch) ;


        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public List<Member> listAllMembers();


        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Path("/{id:[0-9][0-9]*}")
        public Response updateMember(@PathParam("id") long id, Member updatedMember);
    }

    MemberResourceRESTServiceClient client;

    @BeforeClass
    public static void initResteasyClient() {
        RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
    }

    @Before
    public void buildClient()
    {
        String restApiBaseUrl = deploymentUrl.toString() + "rest";
        client = ProxyFactory.create(MemberResourceRESTServiceClient.class, restApiBaseUrl);
    }

    @Test
    public void testRegistrationForNewPersonSuccessful() throws Exception {
        Member newMember = new Member();
        newMember.setName("Sam Doe");
        newMember.setEmail("sam@mailinator.com");
        newMember.setPhoneNumber("5253555555");

        Response response = client.createMember(newMember);

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRegistrationWhenEmailIsTaken() throws Exception {
        Member newMember = new Member();
        newMember.setName("Sam Doe");
        newMember.setEmail("john.smith@mailinator.com");
        newMember.setPhoneNumber("5253555555");

        Response response = client.createMember(newMember);

        assertEquals(409, response.getStatus());
    }

    @Test
    public void testPatchApplicationWhenNameUpdateIsSuccessful() throws Exception {
        Long id = setupJane();

        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/name\"," +
                "\"value\": \"Jenny Doe\"" +
                "}]";

        Response response = client.patchMember(id, patch);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void testListAllMembers() {
        List<Member> members = client.listAllMembers();
        assertFalse(members.isEmpty());
    }

    @Test
    public void testGetById() {
        Member member = getJohn();
    }

    @Test
    public void testUpdateMember() {
        Member john = getJohn();
        assertEquals(john.getPhoneNumber(), "2125551212");
        john.setPhoneNumber("2125551217");

        ClientResponse<?> response = (ClientResponse<?>) client.updateMember(0, john);
        assertEquals(204, response.getStatus());
        response.releaseConnection();
        Member updatedJohn = getJohn();
        assertEquals(john.getPhoneNumber(), "2125551217");
    }

    @Test
    public void testUpdateMemberIdFails() {
        Member john = getJohn();
        john.setId(2L);

        ClientResponse<?> response = (ClientResponse<?>) client.updateMember(0, john);
        assertEquals(400, response.getStatus());
    }

    private Member getJohn() {
        ClientResponse<Member> response = client.lookupMemberById(0);
        assertEquals(200, response.getStatus());
        Member member = response.getEntity();
        assertEquals(member.getName(), "John Smith");
        response.releaseConnection();
        return member;
    }


    @Test
    public void testGetByIdWhenDoesNotExist() {
        ClientResponse<Member> response = client.lookupMemberById(42);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testUpdateMemberFailsWhenDoesNotExist() {

        Member newMember = new Member();
        newMember.setName("Jimmy Doe");
        newMember.setEmail("jimmy@mailinator.com");
        newMember.setPhoneNumber("5253555777");

        ClientResponse<?> response = (ClientResponse<?>) client.updateMember(42, newMember);
        assertEquals(404, response.getStatus());
    }


    // work-around for the fact that it's hard to clean up test data
    static int counter = 0;

    private Long setupJane() {
        String email = "jane" + counter + "@mailinator.com";
        counter++;
        Member newMember = new Member();
        newMember.setName("Jane Doe");
        newMember.setEmail(email);
        newMember.setPhoneNumber("5555555555");

        ClientResponse<?> response = (ClientResponse<?>) client.createMember(newMember);
        assertEquals(200, response.getStatus());
        response.releaseConnection();

        List<Member> members = client.listAllMembers();
        for (Member member : members)
        {
            if (member.getEmail().equals(email))
                return member.getId();
        }
        throw new AssertionError("Could not find jane");
    }

    @Test
    public void testPatchApplicationWhenNewNameIsInvalid() throws Exception {
        Long id = setupJane();
        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/name\"," +
                "\"value\": \"Jane Doe 2\"" +
                "}]";

        ClientResponse<Map<String, String>> response = (ClientResponse<Map<String, String>>) client.patchMember(id, patch);

        //note: this should probably instead be 422, but for now I am keeping with 400 to match original project's service
        assertEquals(400, response.getStatus());
        Map<String, String> errors = getErrors(response);
        assertEquals("name", Iterables.getOnlyElement(errors.keySet()));
        assertEquals("Must not contain numbers", errors.get("name"));
    }

    @Test
    public void testPatchApplicationWhenNewEmailIsTaken() throws Exception {
        Long id = setupJane();
        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/email\"," +
                "\"value\": \"john.smith@mailinator.com\"" +
                "}]";

        ClientResponse<Map<String, String>> response = (ClientResponse<Map<String, String>>) client.patchMember(id, patch);

        //note: this should probably instead be 422, but for now I am keeping with 400 to match original project's service
        assertEquals(409, response.getStatus());
        Map<String, String> errors = getErrors(response);
        assertEquals("email", Iterables.getOnlyElement(errors.keySet()));
        assertEquals("Email taken", errors.get("email"));
    }

    private Map<String, String> getErrors(ClientResponse<Map<String, String>> response) {
        return response.getEntity(new GenericType<Map<String, String>>(){});
    }


}
