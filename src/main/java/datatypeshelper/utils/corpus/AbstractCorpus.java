package datatypeshelper.utils.corpus;

import java.util.HashMap;
import java.util.Iterator;

import datatypeshelper.utils.corpus.properties.CorpusProperty;

public abstract class AbstractCorpus implements Corpus {

    private static final long serialVersionUID = -5837383654657177879L;

    protected HashMap<Class<? extends CorpusProperty>, CorpusProperty> properties;

    public AbstractCorpus() {
        properties = new HashMap<Class<? extends CorpusProperty>, CorpusProperty>();
    }

    @Override
	public Iterator<Class<? extends CorpusProperty>> getPropertiesIterator() {
        return properties.keySet().iterator();
    }

    @Override
	public void removeProperty(Class<? extends CorpusProperty> propertyClass) {
        properties.remove(propertyClass);
    }

    /**
     * @param property
     */
    @Override
	public <T extends CorpusProperty> void addProperty(T property) {
        properties.put(property.getClass(), property);
    }

    /**
     * @return com.unister.semweb.topic_modeling.utils.DocumentProperty
     * @param propertyClass
     */
    @Override
	@SuppressWarnings("unchecked")
    public <T extends CorpusProperty> T getProperty(Class<T> propertyClass) {
        return (T) properties.get(propertyClass);
    }

    @Override
	public HashMap<Class<? extends CorpusProperty>, CorpusProperty> getProperties() {
        return properties;
    }

    @Override
	public void setProperties(HashMap<Class<? extends CorpusProperty>, CorpusProperty> properties) {
        this.properties = properties;
    }
}
