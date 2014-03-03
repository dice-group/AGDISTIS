package datatypeshelper.utils.doc;

/**
 * Class DocumentId
 */
public class DocumentURI extends AbstractSimpleDocumentProperty<String> implements ParseableDocumentProperty,
        StringContainingDocumentProperty {

    private static final long serialVersionUID = 2701176401881628900L;

    public DocumentURI() {
        super("");
    }

    public DocumentURI(String source) {
        super(source);
    }

    @Override
    public void parseValue(String value) {
        set(value);
    }

    @Override
    public String getStringValue() {
        return get();
    }
}
