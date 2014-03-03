/******************************************************************
 * File:        CompositeTemplate.java
 * Created by:  Dave Reynolds
 * Created on:  12 Dec 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dclib.templates;

import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;

import com.epimorphics.dclib.framework.BindingEnv;
import com.epimorphics.dclib.framework.ConverterProcess;
import com.epimorphics.dclib.framework.DataContext;
import com.epimorphics.dclib.framework.NullResult;
import com.epimorphics.dclib.framework.Pattern;
import com.epimorphics.dclib.framework.Template;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;

public class CompositeTemplate extends TemplateBase implements Template {
    protected JsonObject spec;
    protected List<Template> templates;
    
    /**
     * Test if a json object specifies on of these templates
     */
    public static boolean isSpec(JsonObject spec) {
        if (spec.hasKey(JSONConstants.TYPE)) {
            return spec.get(JSONConstants.TYPE).getAsString().value().equals(JSONConstants.COMPOSITE);
        } else {
            return spec.hasKey( JSONConstants.ONE_OFFS ) && spec.hasKey( JSONConstants.TEMPLATES );
        }
    }

    public CompositeTemplate(JsonObject spec, DataContext dc) {
        super(spec);
        this.spec = spec;
        
        // Extract the list of to level templates to run
        templates = getTemplates(spec.get(JSONConstants.TEMPLATES), dc);
        
        // Extract any reference templates
        for (Template t : getTemplates(spec.get(JSONConstants.REFERENCED), dc)) {
            dc.registerTemplate(t);
        }
    }

    @Override
    public Node convertRow(ConverterProcess proc, BindingEnv row, int rowNumber) {
        super.convertRow(proc, row, rowNumber);
        
        Node result = null;
        for (Template template : templates) {
            if (template.isApplicableTo(row)) {
                try {
                    Node n = template.convertRow(proc, row, rowNumber);
                    if (result == null && n != null) {
                        result = n;
                    }
                } catch (NullResult e) {
                    // Silently ignore null results
                } catch (Exception e) {
                    proc.getMessageReporter().report("Warning: template " + template.getName() + " applied but failed: " + e, rowNumber);
                }
            }
        }
        return result;
    }
    
    @Override
    public void preamble(ConverterProcess proc) {
        super.preamble(proc);
        
        DataContext dc = proc.getDataContext();
        BindingEnv env = proc.getEnv();
        
        // Instantiate any global bindings
        if (spec.hasKey(JSONConstants.BIND)) {
            JsonObject binding = spec.get(JSONConstants.BIND).getAsObject();
            for (Entry<String, JsonValue> ent : binding.entrySet()) {
                Pattern p = new Pattern(ent.getValue().getAsString().value(), dc);
                env.put(ent.getKey(), p.evaluate(env, proc, -1));
            }
            
            // Fix up dataset binding incase the BIND has changed the $base (which it typically does)
            Object baseURI = env.get(ConverterProcess.BASE_OBJECT_NAME);
            if (baseURI != null) {
                Node dataset = NodeFactory.createURI(baseURI.toString());
                env.put(ConverterProcess.DATASET_OBJECT_NAME, dataset);
            }

        }
        
        // Process any one-offs
        for (Template t : getTemplates(spec.get(JSONConstants.ONE_OFFS), dc)) {
            t.preamble(proc);
            t.convertRow(proc, env, 0);
        }
    }
}
