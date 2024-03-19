package com.uh.rainbow.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>File:</b> Course.java
 * <p>
 * <b>Description:</b>
 *
 * @author Derek Garcia
 */
public class Course {

    private final String cid;
    private final String name;
    private final int credits;
    private final List<Section> sections = new ArrayList<>();

    public Course(String cid, String name, int credits){
        this.cid = cid;
        this.name = name;
        this.credits = credits;
    }

    public void addSection(Section section){
        this.sections.add(section);
    }


}
