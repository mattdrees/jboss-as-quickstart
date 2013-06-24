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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.rest.JsonPatchApplier;
import org.jboss.as.quickstarts.kitchensink.rest.MemberResourceRESTService;
import org.jboss.as.quickstarts.kitchensink.rest.UnprocessableEntityStatusType;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;
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
import javax.transaction.UserTransaction;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class MemberResourceRESTServiceTest {

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
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                // Deploy our test datasource
                .addAsWebInfResource("test-ds.xml")
                .addAsLibraries(resolver
                    .resolve("com.github.fge:json-patch")
                    .withTransitivity()
                    .asFile());
    }

    @Inject
    MemberResourceRESTService service;


    @Inject
    EntityManager em;

    @Inject
    UserTransaction utx;

    @Inject
    Logger log;

    private Long id;
    private Long id2;

    @Before
    public void setup() throws Exception {
        setupTransaction();
        Member member = new Member();
        member.setName("Jane Doe");
        member.setEmail("jane@mailinator.com");
        member.setPhoneNumber("2125551234");
        em.persist(member);
        this.id = member.getId();

        Member member2 = new Member();
        member2.setName("John Doe");
        member2.setEmail("john@mailinator.com");
        member2.setPhoneNumber("5551123125");
        em.persist(member2);
        this.id2 = member2.getId();

        finishTransaction();
    }

    @After
    public void cleanup() throws Exception {
        setupTransaction();
        try {
            if (id != null)
            {
                removeMember(id);
            }
            if (id2 != null)
            {
                removeMember(id2);
            }
        } finally {
            id = null;
            id2 = null;
            finishTransaction();
        }
    }

    private void finishTransaction() throws Exception {
        em.flush();
        em.clear();
        utx.commit();
    }

    private void setupTransaction() throws Exception {
        utx.begin();
        em.joinTransaction();
    }

    private void removeMember(Long id) {
        Member member;
        try {
            member = em.find(Member.class, id);
        } catch (EntityNotFoundException e) {
            return;
        }
        em.remove(member);
    }


    @Test
    public void testPatchApplicationWhenNameUpdateIsSuccessful() throws Exception {
        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/name\"," +
                "\"value\": \"Jenny Doe\"" +
                "}]";

        Response response = service.patchMember(id, patch);

        assertEquals(204, response.getStatus());

        setupTransaction();
        Member member = em.find(Member.class, id);
        assertEquals("Jenny Doe", member.getName());
        finishTransaction();
    }

    @Test
    public void testPatchApplicationWhenNewNameIsInvalid() throws Exception {
        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/name\"," +
                "\"value\": \"Jane Doe 2\"" +
                "}]";

        Response response = service.patchMember(id, patch);

        //note: this should probably instead be 422, but for now I am keeping with 400 to match original project's service
        assertEquals(400, response.getStatus());
        Map<String, String> errors = (Map<String, String>) response.getEntity();
        assertEquals("name", Iterables.getOnlyElement(errors.keySet()));
        assertEquals("Must not contain numbers", errors.get("name"));

        setupTransaction();
        Member member = em.find(Member.class, id);
        assertEquals("Jane Doe", member.getName());
        finishTransaction();
    }

    @Test
    public void testPatchApplicationWhenNewEmailIsTaken() throws Exception {
        String patch =
                "[{" +
                "\"op\": \"replace\", " +
                "\"path\": \"/email\"," +
                "\"value\": \"john@mailinator.com\"" +
                "}]";

        Response response = service.patchMember(id, patch);

        //note: this should probably instead be 422, but for now I am keeping with 400 to match original project's service
        assertEquals(409, response.getStatus());
        Map<String, String> errors = (Map<String, String>) response.getEntity();
        assertEquals("email", Iterables.getOnlyElement(errors.keySet()));
        assertEquals("Email taken", errors.get("email"));

        setupTransaction();
        Member member = em.find(Member.class, id);
        assertEquals("jane@mailinator.com", member.getEmail());
        finishTransaction();
    }


}
