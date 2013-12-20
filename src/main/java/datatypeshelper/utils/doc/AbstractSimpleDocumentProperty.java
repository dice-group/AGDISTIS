package datatypeshelper.utils.doc;

public class AbstractSimpleDocumentProperty<T> extends AbstractDocumentProperty {

    private static final long serialVersionUID = -3181007408113509355L;
    
    private T value;

    public AbstractSimpleDocumentProperty(T value) {
        this.value = value;
    }

    @Override
    public Object getValue() {
        return get();
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
