package datatypeshelper.utils.doc;

import java.util.Arrays;

public abstract class AbstractArrayContainingDocumentProperty implements ArrayContainingDocumentProperty {

    private static final long serialVersionUID = -4282985203144385913L;

    @Override
    public Object getValue() {
        return getValueAsArray();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName());
        builder.append("=\"");
        builder.append(Arrays.toString(this.getValueAsArray()));
        builder.append("\"");
        return builder.toString();
    }
}
