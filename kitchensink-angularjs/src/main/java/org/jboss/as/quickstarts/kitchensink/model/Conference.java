package org.jboss.as.quickstarts.kitchensink.model;

import com.google.common.collect.Lists;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Drees
 */
@Entity
public class Conference {

    @Id
    @GeneratedValue
    public Long id;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "conference")
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "conference_id", nullable = false)
    @OrderColumn(nullable = false)
    public List<Page> pages = new ArrayList<Page>();

}
