package datatypeshelper.io.xml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentCategory;
import datatypeshelper.utils.doc.DocumentName;
import datatypeshelper.utils.doc.DocumentScore;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ParseableDocumentProperty;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import datatypeshelper.utils.doc.ner.SignedNamedEntityInText;

@SuppressWarnings("unchecked")
class CorpusXmlTagHelper {

    private static List<Class<? extends ParseableDocumentProperty>> registeredProperties = Arrays
            .asList((Class<? extends ParseableDocumentProperty>) DocumentName.class, DocumentText.class,
                    DocumentCategory.class, DocumentScore.class);

    public static final String XML_FILE_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    public static final String CORPUS_TAG_NAME = "corpus";
    public static final String NAMESPACE = "http://semweb.unister.de/xml-corpus-schema-2013";
    public static final String NAMESPACE_DECLARATION = "xmlns=\"" + NAMESPACE + "\"";

    public static final String DOCUMENT_TAG_NAME = Document.class.getSimpleName();

    public static final String TEXT_WITH_NAMED_ENTITIES_TAG_NAME = "TextWithNamedEntities";
    public static final String TEXT_PART_TAG_NAME = "SimpleTextPart";
    public static final String NAMED_ENTITY_IN_TEXT_TAG_NAME = NamedEntityInText.class.getSimpleName();
    public static final String SIGNED_NAMED_ENTITY_IN_TEXT_TAG_NAME = SignedNamedEntityInText.class.getSimpleName();

    public static final String DOCUMENT_CATEGORIES_TAG_NAME = "DocumentMultipleCategories";
    public static final String DOCUMENT_CATEGORIES_SINGLE_CATEGORY_TAG_NAME = "Category";

    public static final String SOURCE_ATTRIBUTE_NAME = "source";
    public static final String URI_ATTRIBUTE_NAME = "uri";

    private static Map<Class<? extends ParseableDocumentProperty>, String> propertyToTagNameMapping = createDocPropTagMap();
    private static Map<String, Class<? extends ParseableDocumentProperty>> tagNameToPropertyMapping = MapUtils
            .invertMap(propertyToTagNameMapping);

    private static Map<Class<? extends ParseableDocumentProperty>, String> createDocPropTagMap() {
        Map<Class<? extends ParseableDocumentProperty>, String> map = new HashMap<Class<? extends ParseableDocumentProperty>, String>(
                registeredProperties.size());
        for (Class<? extends ParseableDocumentProperty> c : registeredProperties) {
            map.put(c, c.getSimpleName());
        }
        return map;
    }

    public static synchronized void registerParseableDocumentProperty(Class<? extends ParseableDocumentProperty> clazz) {
        propertyToTagNameMapping.put(clazz, clazz.getSimpleName());
        tagNameToPropertyMapping.put(clazz.getSimpleName(), clazz);
    }

    public static String getTagNameOfParseableDocumentProperty(Class<? extends ParseableDocumentProperty> clazz) {
        if (propertyToTagNameMapping.containsKey(clazz)) {
            return propertyToTagNameMapping.get(clazz);
        } else {
            return null;
        }
    }

    public static Class<? extends ParseableDocumentProperty> getParseableDocumentPropertyClassForTagName(String tagName) {
        if (tagNameToPropertyMapping.containsKey(tagName)) {
            return tagNameToPropertyMapping.get(tagName);
        } else {
            return null;
        }
    }
}
