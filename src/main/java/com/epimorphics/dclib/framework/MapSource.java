/******************************************************************
 * File:        MapSource.java
 * Created by:  Dave Reynolds
 * Created on:  13 Dec 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dclib.framework;

import com.hp.hpl.jena.graph.Node;

/**
 * Signature for utilities the support mapping of input values 
 * to normalized RDF values (normally URIs).
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface MapSource {

    /**
     * Return the matching normalized RDF value or none if there no
     * match (or no unambiguous match)
     */
    public Node lookup(String key);
    
    
    /**
     * Return the name of this suorce, may be null
     */
    public String getName();
    
}