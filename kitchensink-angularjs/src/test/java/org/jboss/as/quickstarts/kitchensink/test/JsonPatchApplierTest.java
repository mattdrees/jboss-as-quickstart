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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.rest.JsonPatchApplier;
import org.jboss.as.quickstarts.kitchensink.rest.UnprocessableEntityStatusType;
import org.jboss.as.quickstarts.kitchensink.util.Resources;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.*;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class JsonPatchApplierTest {

    @Deployment
    public static Archive<?> createTestArchive() {


        PomEquippedResolveStage resolver = Maven.resolver().offline().loadPomFromFile("pom.xml");
        
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(
                    Member.class,
                    JsonPatchApplier.class,
                    UnprocessableEntityStatusType.class,
                    Resources.class)
                .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                // Deploy our test datasource
                .addAsWebInfResource("test-ds.xml")
                .addAsLibraries(resolver
                    .resolve("com.github.fge:json-patch")
                    .withTransitivity()
                    .asFile());
    }

    @Inject
    JsonPatchApplier jsonPatchApplier;

    @Inject
    Logger log;


    public Member createJane(){
        Member member = new Member();
        member.setName("Jane Doe");
        member.setEmail("jane@mailinator.com");
        member.setPhoneNumber("2125551234");
        return member;
    }

    Member member = createJane();

    @Test
    public void testPatchApplicationWhenNameUpdateIsSuccessful() throws Exception {
        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/name\"," +
                "\"value\": \"Jenny Doe\"" +
                "}]";

        Member updatedMember = jsonPatchApplier.applyJsonPatch(patch, member);

        assertEquals("Jenny Doe", updatedMember.getName());
    }

    @Test
    public void testPatchApplicationWhenPatchDoesNotParse() throws Exception {
        String patch = "garbage";

        try {
            Member updatedMember = jsonPatchApplier.applyJsonPatch(patch, member);
            fail("should have thrown exception");
        }
        catch (WebApplicationException e)
        {
            assertEquals(400, e.getResponse().getStatus());
            log.fine(e.getResponse().getEntity().toString());
        }
    }

    @Test
    public void testPatchApplicationWhenPatchIsJsonButNotJsonPatch() throws Exception {
        String patch =
                "[{\"foo\": \"bar\"}]";

        try {
            Member updatedMember = jsonPatchApplier.applyJsonPatch(patch, member);
            fail("should have thrown exception");
        }
        catch (WebApplicationException e)
        {
            assertEquals(400, e.getResponse().getStatus());
            log.info(e.getResponse().getEntity().toString());
        }
    }

    @Test
    public void testPatchApplicationWhenPatchIsValidJsonPatchButPatchErrors() throws Exception {

        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/notARealPath\"," +
                "\"value\": \"something\"" +
                "}]";

        try {
            Member updatedMember = jsonPatchApplier.applyJsonPatch(patch, member);
            fail("should have thrown exception");
        }
        catch (WebApplicationException e)
        {
            assertEquals(409, e.getResponse().getStatus());
            log.fine(e.getResponse().getEntity().toString());
        }
    }


    @Test
    public void testPatchApplicationWhenPatchIsValidJsonPatchWithoutConflictButResultIsNotAValidEntity() throws Exception {

        String patch =
                "[{" +
                        "\"op\": \"add\", " +
                        "\"path\": \"/anUnsupportedAttribute\"," +
                        "\"value\": \"something\"" +
                        "}]";

        try {
            Member updatedMember = jsonPatchApplier.applyJsonPatch(patch, member);
            fail("should have thrown exception");
        }
        catch (WebApplicationException e)
        {
            assertEquals(422, e.getResponse().getStatus());
            log.fine(e.getResponse().getEntity().toString());
        }
    }

}
