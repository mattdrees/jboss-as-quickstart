package org.jboss.as.quickstarts.kitchensink.test;

import junit.framework.Assert;
import org.jboss.as.quickstarts.kitchensink.model.Conference;
import org.jboss.as.quickstarts.kitchensink.model.Page;

import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
* @author Matt Drees
*/
@Stateful
public class OrderColumnTester
{
    @Inject
    EntityManager entityManager;

    private Long id;

    public void insertConference() {
        Conference conf = new Conference();
        conf.pages.add(new Page("A", conf));
        conf.pages.add(new Page("B", conf));
        conf.pages.add(new Page("C", conf));
        entityManager.persist(conf);
        this.id = conf.id;
    }

    public void checkOrder() {
        Conference conference = entityManager.find(Conference.class, id);
        Assert.assertEquals("A", conference.pages.get(0).name);
        Assert.assertEquals("B", conference.pages.get(1).name);
        Assert.assertEquals("C", conference.pages.get(2).name);

        Assert.assertEquals(new Integer(0), conference.pages.get(0).pageOrder);
    }

    public void rearrangePages() {
        Conference conference = entityManager.find(Conference.class, id);
        Page removed = conference.pages.remove(0);
        conference.pages.add(removed);
    }


    public void checkOrderHasChanged() {
        Conference conference = entityManager.find(Conference.class, id);
        Assert.assertEquals("B", conference.pages.get(0).name);
        Assert.assertEquals("C", conference.pages.get(1).name);
        Assert.assertEquals("A", conference.pages.get(2).name);

        Assert.assertEquals(new Integer(0), conference.pages.get(0).pageOrder);
    }


    public void rearrangePagesServerSide() {
        int oldIndex = 2;
        int newIndex = 0;
        movePage(oldIndex, newIndex);
    }

    private void movePage(int oldIndex, int newIndex) {
        if (oldIndex == newIndex)
            return;

        int delta = oldIndex - newIndex > 0 ? 1 : -1;
        int upper = Math.max(oldIndex, newIndex);
        int lower = Math.min(oldIndex, newIndex);

        String jpaql = "UPDATE Page page\n" +
            "SET page.pageOrder = \n" +
            "  CASE page.pageOrder WHEN :old THEN (0 + :new)\n" +
            "  ELSE (page.pageOrder + :delta) END\n" +
            "WHERE page.pageOrder BETWEEN :lower AND :upper";

        int updatedRows = entityManager.createQuery(jpaql)
            .setParameter("old", oldIndex)
            .setParameter("new", newIndex)
            .setParameter("delta", delta)
            .setParameter("upper", upper)
            .setParameter("lower", lower)
            .executeUpdate();

        int expectedUpdates = upper - lower + 1;
        if (updatedRows != expectedUpdates)
            throw new IllegalStateException("incorrect number of rows updated: " + updatedRows + "; expected " + expectedUpdates);
    }


    public void checkOrderHasChangedAfterServerSideUpdate() {
        Conference conference = entityManager.find(Conference.class, id);
        Assert.assertEquals("A", conference.pages.get(0).name);
        Assert.assertEquals("B", conference.pages.get(1).name);
        Assert.assertEquals("C", conference.pages.get(2).name);
    }

}
