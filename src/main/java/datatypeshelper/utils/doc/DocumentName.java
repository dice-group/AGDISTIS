package datatypeshelper.utils.doc;

/**
 * Class DocumentId
 */
public class DocumentName extends AbstractDocumentProperty implements ParseableDocumentProperty,
        StringContainingDocumentProperty {

    private static final long serialVersionUID = -6517249709629701914L;

    public String name;

    public DocumentName() {
    }

    public DocumentName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object getValue() {
        return getName();
    }

    @Override
    public void parseValue(String value) {
        this.name = value;
    }

    @Override
    public String getStringValue() {
        return name;
    }
}
