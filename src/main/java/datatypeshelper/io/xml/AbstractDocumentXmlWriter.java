package datatypeshelper.io.xml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.utils.doc.Document;
import datatypeshelper.utils.doc.DocumentMultipleCategories;
import datatypeshelper.utils.doc.DocumentProperty;
import datatypeshelper.utils.doc.DocumentText;
import datatypeshelper.utils.doc.ParseableDocumentProperty;
import datatypeshelper.utils.doc.ner.NamedEntitiesInText;
import datatypeshelper.utils.doc.ner.NamedEntityInText;
import datatypeshelper.utils.doc.ner.SignedNamedEntityInText;

abstract class AbstractDocumentXmlWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDocumentXmlWriter.class);

    protected void writeDocument(FileWriter fout, Document document) throws IOException {
        fout.write("<" + CorpusXmlTagHelper.DOCUMENT_TAG_NAME + " id=\"" + document.getDocumentId() + "\">\n");
        DocumentText text = null;
        NamedEntitiesInText nes = null;
        DocumentMultipleCategories categories = null;
        for (DocumentProperty property : document) {
            if (property instanceof DocumentText) {
                text = (DocumentText) property;
            } else if (property instanceof NamedEntitiesInText) {
                nes = (NamedEntitiesInText) property;
            } else if (property instanceof DocumentMultipleCategories) {
                categories = (DocumentMultipleCategories) property;
            } else if (property instanceof ParseableDocumentProperty) {
                writeDocumentProperty(fout, (ParseableDocumentProperty) property);
            }
        }
        if (categories != null) {
            fout.write("<" + CorpusXmlTagHelper.DOCUMENT_CATEGORIES_TAG_NAME + ">\n");
            writeArray(fout, categories.getCategories(),
                    CorpusXmlTagHelper.DOCUMENT_CATEGORIES_SINGLE_CATEGORY_TAG_NAME);
            fout.write("</" + CorpusXmlTagHelper.DOCUMENT_CATEGORIES_TAG_NAME + ">\n");
        }
        if (text != null) {
            if (nes != null) {
                fout.write("<" + CorpusXmlTagHelper.TEXT_WITH_NAMED_ENTITIES_TAG_NAME + ">"
                        + prepareText(text, nes) + "</"
                        + CorpusXmlTagHelper.TEXT_WITH_NAMED_ENTITIES_TAG_NAME + ">\n");
            } else {
                writeDocumentProperty(fout, text);
            }
        }
        fout.write("</" + CorpusXmlTagHelper.DOCUMENT_TAG_NAME + ">\n");
    }

    protected void writeDocumentProperty(FileWriter fout, ParseableDocumentProperty property) throws IOException {
        String tagName = CorpusXmlTagHelper.getTagNameOfParseableDocumentProperty(property.getClass());
        if (tagName != null) {
            fout.write("<" + tagName + ">" + StringEscapeUtils.escapeXml(property.getValue().toString()) + "</"
                    + tagName
                    + ">\n");
        } else {
            LOGGER.error("There is no XML tag name defined for the ParseableDocumentProperty class "
                    + property.getClass().getCanonicalName() + ". Discarding this property.");
        }
    }

    protected void writeArray(FileWriter fout, Object[] array, String elementTagName) throws IOException {
        for (int i = 0; i < array.length; ++i) {
            fout.write("<" + elementTagName + ">" + array[i].toString() + "</" + elementTagName + ">\n");
        }
    }

    protected String prepareText(DocumentText text, NamedEntitiesInText nes) {
        List<String> textParts = new ArrayList<String>();
        List<NamedEntityInText> entities = nes.getNamedEntities();
        Collections.sort(entities);
        String originalText = text.getText();
        // start with the last label and add the parts of the new text beginning
        // with its end to the array
        // Note that we are expecting that the labels are sorted descending by
        // there position in the text!
        boolean isSignedNamedEntity;
        int startFormerLabel = originalText.length();
        for (NamedEntityInText currentNE : entities) {
            // proof if this label undercuts the last one.
            if (startFormerLabel >= currentNE.getEndPos()) {
                isSignedNamedEntity = currentNE instanceof SignedNamedEntityInText;
                // append the text between this label and the former one
                textParts.add(">");
                textParts.add(CorpusXmlTagHelper.TEXT_PART_TAG_NAME);
                textParts.add("</");
                try {
                    textParts.add(StringEscapeUtils.escapeXml(originalText.substring(currentNE.getEndPos(),
                            startFormerLabel)));
                } catch (StringIndexOutOfBoundsException e) {
                    LOGGER.error("Got a wrong named entity (" + currentNE.toString() + ")", e);
                    textParts.add("<AN_ERROR_OCCURED/>");
                }
                textParts.add(">");
                textParts.add(CorpusXmlTagHelper.TEXT_PART_TAG_NAME);
                textParts.add("<");
                // append the markedup label
                textParts.add(">");
                textParts.add(isSignedNamedEntity ? CorpusXmlTagHelper.SIGNED_NAMED_ENTITY_IN_TEXT_TAG_NAME
                        : CorpusXmlTagHelper.NAMED_ENTITY_IN_TEXT_TAG_NAME);
                textParts.add("</");
                try {
                    textParts.add(StringEscapeUtils.escapeXml(originalText.substring(currentNE.getStartPos(),
                            currentNE.getEndPos())));
                } catch (StringIndexOutOfBoundsException e) {
                    LOGGER.error("Got a wrong named entity (" + currentNE.toString() + ")", e);
                    textParts.add("<AN_ERROR_OCCURED/>");
                }
                textParts.add("\">");
                // textParts.add(Integer.toString(currentNE.getLength()));
                // textParts.add("\" length=\"");
                // textParts.add(Integer.toString(currentNE.getStartPos()));
                // textParts.add("\" start=\"");
                if (isSignedNamedEntity) {
                    textParts.add(((SignedNamedEntityInText) currentNE).getSource());
                    textParts.add("\" source=\"");
                }
                textParts.add(currentNE.getNamedEntityUri());
                textParts.add(" uri=\"");
                textParts.add(isSignedNamedEntity ? CorpusXmlTagHelper.SIGNED_NAMED_ENTITY_IN_TEXT_TAG_NAME
                        : CorpusXmlTagHelper.NAMED_ENTITY_IN_TEXT_TAG_NAME);
                textParts.add("<");
                // remember the start position of this label
                startFormerLabel = currentNE.getStartPos();
            }
        }
        if (startFormerLabel > 0) {
            textParts.add("</SimpleTextPart>");
            textParts.add(StringEscapeUtils.escapeXml(originalText.substring(0, startFormerLabel)));
            textParts.add("<SimpleTextPart>");
        }
        // Form the new text beginning with its end
        StringBuilder textWithMarkups = new StringBuilder();
        for (int i = textParts.size() - 1; i >= 0; --i) {
            textWithMarkups.append(textParts.get(i));
        }
        return textWithMarkups.toString();
    }

    public static void registerParseableDocumentProperty(Class<? extends ParseableDocumentProperty> clazz) {
        CorpusXmlTagHelper.registerParseableDocumentProperty(clazz);
    }
}
