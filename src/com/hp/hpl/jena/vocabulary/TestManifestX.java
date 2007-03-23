package com.hp.hpl.jena.vocabulary ;

/* CVS $Id: TestManifestX.java,v 1.1 2007-03-23 11:59:43 andy_seaborne Exp $ */
 
import com.hp.hpl.jena.rdf.model.*;
 
/**
 * Vocabulary definitions from test-manifest-x.n3 
 * @author Auto-generated by schemagen on 09 Jan 2007 14:03 
 */
public class TestManifestX {
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://jena.hpl.hp.com/2005/05/test-manifest-extra#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    /** <p>Default type of a test</p> */
    public static final Property defaultTestType = m_model.createProperty( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#defaultTestType" );
    
    /** <p>Include another manifest file.</p> */
    public static final Property include = m_model.createProperty( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#include" );
    
    /** <p>Whether to create a text index</p> */
    public static final Property textIndex = m_model.createProperty( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#textIndex" );
    
    /** <p>Syntax of the query</p> */
    public static final Property dataSyntax = m_model.createProperty( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#dataSyntax" );
    
    /** <p>Syntax of the query</p> */
    public static final Property querySyntax = m_model.createProperty( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#querySyntax" );
    
    /** <p>Query test not to be run</p> */
    public static final Resource TestSurpressed = m_model.createResource( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#TestSurpressed" );
    
    /** <p>Query serialization tests</p> */
    public static final Resource TestSerialization = m_model.createResource( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#TestSerialization" );
    
    /** <p>Syntax tests which expect a parse failure</p> */
    public static final Resource TestBadSyntax = m_model.createResource( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#TestBadSyntax" );
    
    /** <p>Syntax tests (query)</p> */
    public static final Resource TestSyntax = m_model.createResource( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#TestSyntax" );
    
    /** <p>The class of test that are Query tests (query, data, results)</p> */
    public static final Resource TestQuery = m_model.createResource( "http://jena.hpl.hp.com/2005/05/test-manifest-extra#TestQuery" );
    
}
