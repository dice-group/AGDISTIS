package datatypeshelper.utils.doc;

/**
 * Class DocumentClass
 */
public class DocumentCategory extends AbstractDocumentProperty implements ParseableDocumentProperty, StringContainingDocumentProperty {

    private static final long serialVersionUID = -799781054037155226L;

    public String category;

    public DocumentCategory() {
    }

    public DocumentCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public Object getValue() {
        return getCategory();
    }

    @Override
    public void parseValue(String value) {
        this.category = value;
    }

    @Override
    public String getStringValue() {
        return category;
    }
}
