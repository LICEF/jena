/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.query.text ;

import java.io.IOException ;
import java.util.* ;
import java.util.Map.Entry ;

import org.apache.lucene.analysis.Analyzer ;
import org.apache.lucene.analysis.core.KeywordAnalyzer ;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper ;
import org.apache.lucene.analysis.standard.StandardAnalyzer ;
import org.apache.lucene.document.* ;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException ;
import org.apache.lucene.queryparser.classic.QueryParser ;
import org.apache.lucene.queryparser.classic.QueryParserBase ;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory ;
import org.apache.lucene.util.Version ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.graph.NodeFactory ;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra ;

public class TextIndexLucene implements TextIndex {
    private static Logger          log      = LoggerFactory.getLogger(TextIndexLucene.class) ;

    private static int             MAX_N    = 10000 ;
    public static final Version    VER      = Version.LUCENE_46 ;

    public static final FieldType  ftIRI ;
    static {
        ftIRI = new FieldType() ;
        ftIRI.setTokenized(false) ;
        ftIRI.setStored(true) ;
        ftIRI.setIndexed(true) ;
        ftIRI.freeze() ;
    }
    public static final FieldType  ftString = StringField.TYPE_NOT_STORED ;
    public static final FieldType  ftText   = TextField.TYPE_NOT_STORED ;
    // Bigger index, easier to debug!
    // public static final FieldType ftText = TextField.TYPE_STORED ;

    private final EntityDefinition docDef ;
    private final Directory        directory ;
    private IndexWriter            indexWriter ;
    private Analyzer               analyzer ;

    private static final String BORDER_DELIMITER = "borderdelimiter";

    public TextIndexLucene(Directory directory, EntityDefinition def) {
        this(directory, def, null);
    }

    public TextIndexLucene(Directory directory, EntityDefinition def, String lang) {
        this.directory = directory ;
        this.docDef = def ;

        // create the analyzer as a wrapper that uses KeywordAnalyzer for
        // entity and graph fields and StandardAnalyzer for all other
        Map<String, Analyzer> analyzerPerField = new HashMap<>() ;
        analyzerPerField.put(def.getEntityField(), new KeywordAnalyzer()) ;
        if ( def.getGraphField() != null )
            analyzerPerField.put(def.getGraphField(), new KeywordAnalyzer()) ;
        
        for (String field : def.fields()) {
        	Analyzer analyzer = def.getAnalyzer(field);
        	if (analyzer != null) {
        		analyzerPerField.put(field, analyzer);
        	}
        }

        Analyzer _analyzer;
        if( "en".equals( lang ) )
            _analyzer = new EnglishAnalyzer(VER);
        else if( "fr".equals( lang ) )
            _analyzer = new FrenchAnalyzer(VER);
        else if( "es".equals( lang ) )
            _analyzer = new SpanishAnalyzer(VER);
        else
            _analyzer = new StandardAnalyzer(VER);

        this.analyzer = new PerFieldAnalyzerWrapper(_analyzer, analyzerPerField) ;

        // force creation of the index if it don't exist
        // otherwise if we get a search before data is written we get an
        // exception
        startIndexing() ;
        finishIndexing() ;
    }

    public Directory getDirectory() {
        return directory ;
    }

    public Analyzer getAnalyzer() {
        return analyzer ;
    }

    @Override
    public void startIndexing() {
        try {
            IndexWriterConfig wConfig = new IndexWriterConfig(VER, analyzer) ;
            indexWriter = new IndexWriter(directory, wConfig) ;
        }
        catch (IOException e) {
            exception(e) ;
        }
    }

    @Override
    public void finishIndexing() {
        try {
            if (indexWriter != null) {
                indexWriter.commit();
                close();
            }
        }
        catch (IOException e) {
            exception(e) ;
        }
    }

    @Override
    public void abortIndexing() {
        try {
            if (indexWriter != null) {
                indexWriter.rollback();
                close();
            }
        }
        catch (IOException ex) {
            exception(ex) ;
        }
    }

    @Override
    public void close() {
        if ( indexWriter != null )
            try {
                indexWriter.close() ;
                indexWriter = null;
            }
            catch (IOException ex) {
                exception(ex) ;
            }
    }

    @Override
    public void addEntity(Entity entity) {
        if ( log.isDebugEnabled() )
            log.debug("Add entity: " + entity) ;
        try {
            boolean autoBatch = (indexWriter == null) ;
            Document doc = doc(entity) ;
            if ( autoBatch )
                startIndexing() ;
            indexWriter.addDocument(doc) ;
// put in comment for work with jena transaction
//            if ( autoBatch )
//                finishIndexing() ;

        }
        catch (IOException e) {
            exception(e) ;
        }
    }

    @Override
    public void deleteEntity(Entity entity) {
        if ( log.isDebugEnabled() )
            log.debug("Delete entity: "+entity) ;
        try {
            boolean autoBatch = (indexWriter == null) ;

            TermQuery qUri = new TermQuery(new Term("uri", entity.getId()));
            Map<String, Object> map = entity.getMap();
            String property = map.keySet().iterator().next();
            String value = (String)map.get(property);

            //replacing single and double quotes when unbalanced
            int countOfSingleQuotes = value.length() - value.replace( "'", "" ).length();
            if( countOfSingleQuotes % 2 != 0 )
                value = value.replace( "'", "?" );

            int countOfDoubleQuotes = value.length() - value.replace( "\"", "" ).length();
            if( countOfDoubleQuotes % 2 != 0 )
                value = value.replace( "\"", "?" );
            //

            QueryParser qp = new QueryParser(VER, property, analyzer);
            Query qPropValue = qp.parse("\"" + BORDER_DELIMITER + " " + value + " " + BORDER_DELIMITER + "\"");

            BooleanQuery q = new BooleanQuery();
            q.add(qUri, BooleanClause.Occur.MUST);
            q.add(qPropValue, BooleanClause.Occur.MUST);

            if ( autoBatch )
                startIndexing() ;
            indexWriter.deleteDocuments(q);
// put in comment for work with jena transaction
//            if ( autoBatch )
//                finishIndexing() ;

        } catch (Exception e) { exception(e) ; }
    }

    private Document doc(Entity entity) {
        Document doc = new Document() ;
        Field entField = new Field(docDef.getEntityField(), entity.getId(), ftIRI) ;
        doc.add(entField) ;

        String graphField = docDef.getGraphField() ;
        if ( graphField != null ) {
            Field gField = new Field(graphField, entity.getGraph(), ftString) ;
            doc.add(gField) ;
        }

        for ( Entry<String, Object> e : entity.getMap().entrySet() ) {
            Field field = new Field(e.getKey(), BORDER_DELIMITER + " " + e.getValue() + " " + BORDER_DELIMITER, ftText) ;
            doc.add(field) ;
        }
        return doc ;
    }

    @Override
    public Map<String, Node> get(String uri) {
        try {
            IndexReader indexReader = DirectoryReader.open(directory) ;
            List<Map<String, Node>> x = get$(indexReader, uri) ;
            if ( x.size() == 0 )
                return null ;
            // if ( x.size() > 1)
            // throw new TextIndexException("Multiple entires for "+uri) ;
            return x.get(0) ;
        }
        catch (Exception ex) {
            exception(ex) ;
            return null ;
        }
    }

    private static Query parseQuery(String queryString, String primaryField, Analyzer analyzer) throws ParseException {
        QueryParser queryParser = new QueryParser(VER, primaryField, analyzer) ;
        queryParser.setAllowLeadingWildcard(true) ;
        Query query = queryParser.parse(queryString) ;
        return query ;
    }
    
    private List<Map<String, Node>> get$(IndexReader indexReader, String uri) throws ParseException, IOException {
        String escaped = QueryParserBase.escape(uri) ;
        String qs = docDef.getEntityField() + ":" + escaped ;
        Query query = parseQuery(qs, docDef.getPrimaryField(), analyzer) ;
        IndexSearcher indexSearcher = new IndexSearcher(indexReader) ;
        ScoreDoc[] sDocs = indexSearcher.search(query, 1).scoreDocs ;
        List<Map<String, Node>> records = new ArrayList<Map<String, Node>>() ;

        // Align and DRY with Solr.
        for ( ScoreDoc sd : sDocs ) {
            //** score :: sd.score
            Document doc = indexSearcher.doc(sd.doc) ;
            String[] x = doc.getValues(docDef.getEntityField()) ;
            if ( x.length != 1 ) {}
            String uriStr = x[0] ;
            Map<String, Node> record = new HashMap<>() ;
            Node entity = NodeFactory.createURI(uriStr) ;
            record.put(docDef.getEntityField(), entity) ;

            for ( String f : docDef.fields() ) {
                // log.info("Field: "+f) ;
                String[] values = doc.getValues(f) ;
                for ( String v : values ) {
                    Node n = entryToNode(v) ;
                    record.put(f, n) ;
                }
                records.add(record) ;
            }
        }
        return records ;
    }

    @Override
    public List<Node> query(String qs) {
        return query(qs, MAX_N) ;
    }

    @Override
    public List<Node> query(String qs, int limit) {
        //** score
        try(IndexReader indexReader = DirectoryReader.open(directory)) {
            return query$(indexReader, qs, limit) ;
        } 
        catch (Exception ex) {
            exception(ex) ;
            return null ;
        }
    }

    private List<Node> query$(IndexReader indexReader, String qs, int limit) throws ParseException, IOException {
        IndexSearcher indexSearcher = new IndexSearcher(indexReader) ;
        Query query = parseQuery(qs, docDef.getPrimaryField(), analyzer) ;
        if ( limit <= 0 )
            limit = MAX_N ;
        ScoreDoc[] sDocs = indexSearcher.search(query, limit).scoreDocs ;

        List<Node> results = new ArrayList<>() ;

        // Align and DRY with Solr.
        for ( ScoreDoc sd : sDocs ) {
            Document doc = indexSearcher.doc(sd.doc) ;
            String[] values = doc.getValues(docDef.getEntityField()) ;
            for ( String v : values ) {
                Node n = TextQueryFuncs.stringToNode(v) ;
                results.add(n) ;
            }
        }
        return results ;
    }

    @Override
    public EntityDefinition getDocDef() {
        return docDef ;
    }

    private Node entryToNode(String v) {
        // TEMP
        return NodeFactoryExtra.createLiteralNode(v, null, null) ;
    }

    private static void exception(Exception ex) {
        throw new TextIndexException(ex) ;
    }

    public List<NodeAndScore> queryWithScore(String qs) { return queryWithScore(qs, 0) ; }

    public List<NodeAndScore> queryWithScore(String qs, int limit) {
        try {
            // Upgrade at Java7 ...
            IndexReader indexReader = DirectoryReader.open(directory) ;
            try {
                return queryWithScore$(indexReader, qs, limit) ;
            } finally {
                indexReader.close() ;
            }
        } catch (ParseException ex) {
            exception(ex) ;
            return null ;
        } catch (IOException ex) {
            exception(ex) ;
            return null ;
        }
    }

    public List<NodeAndScore> queryWithScore$(IndexReader indexReader , String qs, int limit) throws ParseException, IOException {
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        //QueryParser queryParser = new QueryParser(VER, docDef.getPrimaryField(), analyzer);
        QueryParser queryParser = new QueryParser(VER, qs.split(":")[0], analyzer);
        Query query = queryParser.parse(qs);
        if ( limit <= 0 )
            limit = MAX_N ;
        ScoreDoc[] sDocs = indexSearcher.search(query, limit).scoreDocs ;

        List<NodeAndScore> results = new ArrayList<NodeAndScore>() ;

        // Align and DRY with Solr.
        for ( ScoreDoc sd : sDocs )
        {
            Document doc = indexSearcher.doc(sd.doc) ;
            String[] values = doc.getValues(docDef.getEntityField()) ;
            float score = sd.score;
            for ( String v : values )
            {
                Node n = Node.createURI(v);
                results.add(new NodeAndScore(n, score)) ;
            }
        }
        return results ;
    }
}
