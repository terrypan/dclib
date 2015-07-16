/******************************************************************
 * File:        Main.java
 * Created by:  Dave Reynolds
 * Created on:  3 Dec 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dclib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.writer.WriterStreamRDFBlocks;

import com.epimorphics.dclib.framework.ConverterProcess;
import com.epimorphics.dclib.framework.ConverterService;
import com.epimorphics.dclib.framework.DataContext;
import com.epimorphics.dclib.framework.Template;
import com.epimorphics.dclib.templates.TemplateFactory;
import com.epimorphics.tasks.LiveProgressMonitor;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.SimpleProgressMonitor;
import com.epimorphics.util.NameUtils;
import com.hp.hpl.jena.rdf.model.Model;

public class Main {
    public static final String DEBUG_FLAG = "--debug";
    public static final String STREAMING_FLAG = "--streaming";
    
    public static void main(String[] argsIn) throws IOException {
        boolean debug = false;
        boolean streaming = false;
        
        // TODO proper command line parsing
        
        List<String> args = new ArrayList<>(argsIn.length);
        for (String arg : argsIn) args.add(arg);
        
        if (args.contains(DEBUG_FLAG)) {
            debug = true;
            args.remove(DEBUG_FLAG);
        }
        if (args.contains(STREAMING_FLAG)) {
            streaming = true;
            args.remove(STREAMING_FLAG);
        }

        if (args.size() < 2) {
            System.err.println("Usage:  java -jar dclib.jar [--debug] [--streaming] template.json ... data.csv");
            System.exit(1);
        }
        String templateName = args.get(0);
        String dataFile = args.get( args.size() -1 );
        
        ConverterService service = new ConverterService();
        DataContext dc = service.getDataContext();
        for (int i = 1; i < args.size() - 1; i++) {
            Template aux = TemplateFactory.templateFrom(args.get(i), dc);
            dc.registerTemplate(aux);
        }
        
        SimpleProgressMonitor reporter = new LiveProgressMonitor();

        boolean succeeded = false;
        if (streaming) {
            Template template = TemplateFactory.templateFrom(templateName, dc);
            
            File dataFileF = new File(dataFile);
            String filename = dataFileF.getName();
            String filebasename = NameUtils.removeExtension(filename);
            dc.getGlobalEnv().put(ConverterProcess.FILE_NAME, filename);
            dc.getGlobalEnv().put(ConverterProcess.FILE_BASE_NAME, filebasename);
            InputStream is = new FileInputStream(dataFileF);
            
            ConverterProcess process = new ConverterProcess(dc, is);
            process.setDebug(debug);
            process.setTemplate( template );
            process.setMessageReporter( reporter );
            process.setAllowNullRows(true);
            
            StreamRDF stream = new WriterStreamRDFBlocks( System.out );
            process.setOutputStream( stream );
            
            succeeded = process.process();
            
        } else {
            Model m = service.simpleConvert(templateName, dataFile, reporter, debug);
            if (m != null) {
                m.write(System.out, "Turtle");
            } else {
                succeeded = false;
            }
        }
        if (!succeeded) {
            System.err.println("Failed to convert data");
            for (ProgressMessage message : reporter.getMessages()) {
                System.err.println(message.toString());
            }
            System.exit(1);
        }
    }
    
    
}
