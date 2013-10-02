package datatypeshelper.utils.doc;

public class DocumentMultipleCategories extends AbstractArrayContainingDocumentProperty implements DocumentProperty {

    private static final long serialVersionUID = -1527431847757666290L;

    private String categories[];

    public DocumentMultipleCategories(String[] categories) {
        this.categories = categories;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    @Override
    public Object[] getValueAsArray() {
        return getCategories();
    }
}
