package datatypeshelper.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.util.FileUtils;

import datatypeshelper.io.CorpusReader;
import datatypeshelper.preprocessing.docsupplier.DocumentSupplier;
import datatypeshelper.utils.corpus.Corpus;
import datatypeshelper.utils.corpus.DocumentListCorpus;
import datatypeshelper.utils.doc.Document;

public class CorpusXmlReader extends AbstractDocumentXmlReader implements CorpusReader {// AbstractCorpusReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorpusXmlReader.class);

    // private MultiPatternAutomaton automaton;
    private BricsBasedXmlParser parser;

    protected File file;
    protected Corpus corpus;

    // private Document currentDocument;
    // private int lastPos;
    // private NamedEntityInText currentNamedEntity;
    // private List<NamedEntityInText> namedEntities = new ArrayList<NamedEntityInText>();
    // private List<String> categories = new ArrayList<String>();

    public CorpusXmlReader(File file) {
        this.file = file;
        // this.automaton = new BricsAutomatonManager(this, new String[] { "\\<[^\\<\\>]*\\>",
        // "\\&[#A-Za-z][A-Za-z0-9]{1,6};" });
        parser = new BricsBasedXmlParser(this);
    }

    @Override
    public void readCorpus() {
        this.corpus = new DocumentListCorpus<List<Document>>(new ArrayList<Document>());
        String text;
        try {
            text = FileUtils.readWholeFileAsUTF8(file.getName());
        } catch (IOException e) {
            LOGGER.error("Couldn't read file.", e);
            return;
        }
        // lastPos = 0;
        // currentDocument = null;
        // automaton.parseText(text);
        parser.parse(text);
    }

    @Override
    public void addDocuments(DocumentSupplier documentFactory) {
        LOGGER.info("Got a "
                + documentFactory.getClass().getCanonicalName()
                + " object as DocumentSupplier. But I'm a corpus reader and don't need such a supplier. ");
    }

    @Override
    public Corpus getCorpus() {
        if (corpus == null) {
            readCorpus();
        }
        return corpus;
    }

    @Override
    public boolean hasCorpus() {
        return corpus != null;
    }

    @Override
    public void deleteCorpus() {
        corpus = null;
    }

    @Override
    protected void finishedDocument(Document document) {
        this.corpus.addDocument(document);
    }

    // @Override
    // public void foundPattern(int patternId, int startPos, int length) {
    // switch (patternId) {
    // case 0: {
    // parseTag(startPos, length);
    // break;
    // }
    // case 1: {
    // parseEscapedCharachter(startPos, length);
    // break;
    // }
    // default: {
    // LOGGER.error("Got an unknown patternId from the automaton (patternId=" + patternId + ").");
    // break;
    // }
    // }
    // }

    // private void parseTag(int startPos, int length) {
    // // if this is a closing tag
    // if (text.charAt(startPos + 1) == '/') {
    // handleClosingTag(startPos, length);
    // } else if (text.charAt(startPos + length - 2) == '/') {
    // // if this is an empty xml tag
    // /* nothing to do */
    // } else {
    // handleOpeningTag(startPos, length);
    // }
    // }

    // private void handleOpeningTag(int startPos, int length) {
    // // we do not need text in front of the closing text
    // // --> set the lastPos
    // lastPos = startPos + length;
    // String tag = text.substring(startPos + 1, lastPos - 1);
    // int pos = tag.indexOf(' ');
    // String tagName;
    // if (pos == -1) {
    // tagName = tag;
    // } else {
    // tagName = tag.substring(0, pos);
    // }
    //
    // if (tagName.equals(CorpusXmlTagHelper.DOCUMENT_TAG_NAME)) {
    // currentDocument = new Document();
    // pos = tag.indexOf(" id=\"");
    // if (pos > 0) {
    // pos += 5;
    // int tmp = tag.indexOf('"', pos + 1);
    // if (tmp > 0) {
    // tmp = Integer.parseInt(tag.substring(pos, tmp));
    // currentDocument.setDocumentId(tmp);
    // }
    // }
    // } else if (tagName.equals(CorpusXmlTagHelper.NAMED_ENTITY_IN_TEXT_TAG_NAME)
    // || tagName.equals(CorpusXmlTagHelper.SIGNED_NAMED_ENTITY_IN_TEXT_TAG_NAME)) {
    // currentNamedEntity = parseNamedEntityInText(tag);
    // currentNamedEntity.setStartPos(buffer.length());
    // }
    // }

    // private void handleClosingTag(int startPos, int length) {
    // String tagName = text.substring(startPos + 2, startPos + length - 1);
    // if (tagName.equals(CorpusXmlTagHelper.DOCUMENT_TAG_NAME)) {
    // this.corpus.addDocument(currentDocument);
    // currentDocument = null;
    // } else if (tagName.equals(CorpusXmlTagHelper.TEXT_WITH_NAMED_ENTITIES_TAG_NAME) && (currentDocument != null)) {
    // currentDocument.addProperty(new DocumentText(buffer.toString()));
    // buffer.delete(0, buffer.length());
    // NamedEntitiesInText nes = new NamedEntitiesInText(namedEntities);
    // currentDocument.addProperty(nes);
    // namedEntities.clear();
    // } else if (tagName.equals(CorpusXmlTagHelper.TEXT_PART_TAG_NAME)) {
    // buffer.append(text.substring(lastPos, startPos));
    // } else if (tagName.equals(CorpusXmlTagHelper.NAMED_ENTITY_IN_TEXT_TAG_NAME)
    // || tagName.equals(CorpusXmlTagHelper.SIGNED_NAMED_ENTITY_IN_TEXT_TAG_NAME)) {
    // if (currentNamedEntity != null) {
    // currentNamedEntity.setLength(startPos - lastPos);
    // namedEntities.add(currentNamedEntity);
    // buffer.append(text.substring(lastPos, startPos));
    // }
    // } else if (tagName.equals(CorpusXmlTagHelper.DOCUMENT_CATEGORIES_TAG_NAME)) {
    // currentDocument.addProperty(new DocumentMultipleCategories(
    // categories.toArray(new String[categories.size()])));
    // categories.clear();
    // } else if (tagName.equals(CorpusXmlTagHelper.DOCUMENT_CATEGORIES_SINGLE_CATEGORY_TAG_NAME)) {
    // categories.add(text.substring(lastPos, startPos));
    // } else {
    // if (currentDocument != null) {
    // Class<? extends ParseableDocumentProperty> propertyClazz = CorpusXmlTagHelper
    // .getParseableDocumentPropertyClassForTagName(tagName);
    // if (propertyClazz != null) {
    // buffer.append(text.substring(lastPos, startPos));
    // try {
    // ParseableDocumentProperty property = propertyClazz.newInstance();
    // property.parseValue(buffer.toString());
    // currentDocument.addProperty(property);
    // } catch (Exception e) {
    // LOGGER.error("Couldn't parse property "
    // + propertyClazz + " from the String \"" + buffer.toString() + "\".", e);
    // }
    // buffer.delete(0, buffer.length());
    // }
    // }
    // }
    // lastPos = startPos + length;
    // }

    // private NamedEntityInText parseNamedEntityInText(String tag) {
    // String namedEntityUri = null;
    // String namedEntitySource = null;
    // int startPos = -1;
    // int length = -1;
    // int start = 0, end = 0;
    // try {
    // start = tag.indexOf(' ') + 1;
    // end = tag.indexOf('=', start);
    // String key, value;
    // while (end > 0) {
    // key = tag.substring(start, end).trim();
    // end = tag.indexOf('"', end);
    // start = tag.indexOf('"', end + 1);
    // value = tag.substring(end + 1, start);
    // if (key.equals(CorpusXmlTagHelper.URI_ATTRIBUTE_NAME)) {
    // namedEntityUri = value;
    // } else if (key.equals(CorpusXmlTagHelper.SOURCE_ATTRIBUTE_NAME)) {
    // namedEntitySource = value;
    // }
    // /*
    // * else if (key.equals("start")) {
    // * startPos = Integer.parseInt(value);
    // * } else if (key.equals("length")) {
    // * length = Integer.parseInt(value);
    // * }
    // */
    // ++start;
    // end = tag.indexOf('=', start);
    // }
    // if (namedEntitySource != null) {
    // return new SignedNamedEntityInText(startPos, length, namedEntityUri, namedEntitySource);
    // } else {
    // return new NamedEntityInText(startPos, length, namedEntityUri);
    // }
    // } catch (Exception e) {
    // LOGGER.error("Couldn't parse NamedEntityInText tag (" + tag + "). Returning null.", e);
    // }
    // return null;
    // }

    // private void parseEscapedCharachter(int startPos, int length) {
    // buffer.append(text.substring(lastPos, startPos));
    // lastPos = startPos + length;
    // buffer.append(StringEscapeUtils.unescapeXml(text.substring(startPos, lastPos)));
    // }

    // public static void registerParseableDocumentProperty(Class<? extends ParseableDocumentProperty> clazz) {
    // CorpusXmlTagHelper.registerParseableDocumentProperty(clazz);
    // }
}
