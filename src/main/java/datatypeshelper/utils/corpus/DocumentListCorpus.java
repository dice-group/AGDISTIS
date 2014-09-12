package datatypeshelper.utils.corpus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentText;

public class DocumentListCorpus<T extends List<Document>> extends AbstractCorpus {

    private static final long serialVersionUID = 761394991935529006L;

    private static final Logger logger = LoggerFactory.getLogger(DocumentListCorpus.class);

    protected T corpus;

    public DocumentListCorpus(T listImplementation)
    {
        corpus = listImplementation;
    }

    @Override
	public void addDocument(Document document)
    {
        // corpus.add(document.getDocumentId(), document);
        corpus.add(document);
    }

    public String[] getTextArray() {
        ArrayList<String> texts = new ArrayList<String>(corpus.size());
        DocumentText text;
        for (Document document : corpus)
        {
            text = document.getProperty(DocumentText.class);
            if (text != null)
            {
                texts.add(text.getText());
            }
            else
            {
                logger.warn("got a Document without a DocumentText property!");
            }
        }
        return texts.toArray(new String[0]);
    }

    public Iterator<Document> getIterator() {
        return corpus.iterator();
    }

    public int size() {
        return getNumberOfDocuments();
    }

    @Override
	public Document getDocument(int documentId) {
        return corpus.get(documentId);
    }

    @Override
    public int getNumberOfDocuments() {
        return corpus.size();
    }

    @Override
    public void clear() {
        corpus.clear();
    }

    @Override
    public Iterator<Document> iterator() {
        return corpus.iterator();
    }

    @Override
    public List<Document> getDocuments(int startId, int endId) {
        return corpus.subList(startId, endId);
    }
}
