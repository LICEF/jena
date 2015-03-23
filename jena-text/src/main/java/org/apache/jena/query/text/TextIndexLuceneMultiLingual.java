package org.apache.jena.query.text;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 13-11-12
 */
public class TextIndexLuceneMultiLingual extends TextIndexLucene {

    Hashtable<String, TextIndex> indexes;

    public TextIndexLuceneMultiLingual(File directory, EntityDefinition def, String[] languages) {
        super(new RAMDirectory(), def);

        indexes = new Hashtable<String, TextIndex>();

        try {
            //default index
            File indexDir = directory;
            Directory dir = FSDirectory.open(indexDir);
            TextIndex index = new TextIndexLucene(dir, def);
            indexes.put("default", index);

            //language indexes
            for (String lang : languages) {
                indexDir = new File(directory, lang );
                dir = FSDirectory.open(indexDir);
                index = new TextIndexLucene(dir, def, lang);
                indexes.put(lang, index);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<TextIndex> getIndexes() {
        return indexes.values();
    }

    TextIndex getIndex(String lang) {
        if (lang == null || !indexes.containsKey(lang))
            return indexes.get("default");
        else
            return indexes.get(lang);
    }

    public void startIndexing() {
        if (indexes == null)
            return;
        for (TextIndex index : indexes.values() )
            index.startIndexing();
    }

    public void finishIndexing() {
        if (indexes == null)
            return;
        for (TextIndex index : indexes.values() )
            index.finishIndexing();
    }

    public void addEntity(Entity entity, String lang) {
        if (lang.length() >= 2)
            lang = lang.toLowerCase().substring(0, 2);
        if (!indexes.containsKey(lang))
            lang = "default";
        indexes.get(lang).addEntity(entity) ;
    }

    public void deleteEntity(Entity entity, String lang) {
        if (lang.length() >= 2)
            lang = lang.toLowerCase().substring(0, 2);
        if (!indexes.containsKey(lang))
            lang = "default";
        indexes.get(lang).deleteEntity(entity);
    }

}