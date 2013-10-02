package datatypeshelper.utils.doc;

import java.util.Iterator;

public class DocumentPropertyIterator implements Iterator<DocumentProperty> {

    private Iterator<Class<? extends DocumentProperty>> classIterator;
    private Document document;

    public DocumentPropertyIterator(Document document) {
        this.document = document;
        this.classIterator = document.getPropertiesIterator();
    }

    @Override
    public boolean hasNext() {
        return classIterator.hasNext();
    }

    @Override
    public DocumentProperty next() {
        Class<? extends DocumentProperty> propertyClass = classIterator.next();
        if (propertyClass == null) {
            return null;
        } else {
            return document.getProperty(propertyClass);
        }
    }

    @Override
    public void remove() {
        classIterator.remove();
    }

}