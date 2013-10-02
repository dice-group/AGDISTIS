package datatypeshelper.utils.corpus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import datatypeshelper.utils.corpus.properties.CorpusProperty;
import datatypeshelper.utils.doc.Document;

public interface Corpus extends Serializable, Iterable<Document> {

    public int getNumberOfDocuments();

    public Document getDocument(int documentId);

    public void addDocument(Document document);

    public void clear();

    public List<Document> getDocuments(int startId, int endId);

    public <T extends CorpusProperty> void addProperty(T property);

    public void removeProperty(Class<? extends CorpusProperty> propertyClass);

    public <T extends CorpusProperty> T getProperty(Class<T> propertyClass);

    public Iterator<Class<? extends CorpusProperty>> getPropertiesIterator();

    public HashMap<Class<? extends CorpusProperty>, CorpusProperty> getProperties();

    public void setProperties(HashMap<Class<? extends CorpusProperty>, CorpusProperty> properties);
}
