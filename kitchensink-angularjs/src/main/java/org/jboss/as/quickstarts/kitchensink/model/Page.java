package org.jboss.as.quickstarts.kitchensink.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Matt Drees
 */
@Entity
public class Page {
    @Id
    @GeneratedValue
    public Long id;

    public String name;

//    @Column(name="page_order")
    @Column(name="pages_order", insertable = false, updatable = false)
    public Integer pageOrder;

    public Page(String name, Conference conference) {
        this.name = name;
        this.conference = conference;
    }

    @ManyToOne
    @JoinColumn(insertable=false, updatable=false, nullable=false)
    public Conference conference;

    public Page() {
    }
}
