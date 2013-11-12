package datatypeshelper.io.xml.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import datatypeshelper.io.xml.AbstractDocumentXmlReader;
import datatypeshelper.preprocessing.docsupplier.DocumentSupplier;
import datatypeshelper.utils.doc.Document;

public class StreamBasedXmlDocumentSupplier extends AbstractDocumentXmlReader implements DocumentSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamBasedXmlDocumentSupplier.class);

    private static final boolean USE_DOCUMENT_IDS_FROM_FILE_DEFAULT = true;

    private boolean useDocumentIdsFromFile;
    private FileReader xmlFileReader;
    private SimpleReaderBasedXMLParser xmlParser;
    private Document document;
    private int documentCount;
    private int nextDocumentId;

    public static StreamBasedXmlDocumentSupplier createReader(String filename)
            throws FileNotFoundException {
        return createReader(filename, USE_DOCUMENT_IDS_FROM_FILE_DEFAULT);
    }

    public static StreamBasedXmlDocumentSupplier createReader(String filename, boolean useDocumentIdsFromFile)
            throws FileNotFoundException {
        FileReader reader = new FileReader(filename);
        return new StreamBasedXmlDocumentSupplier(reader, useDocumentIdsFromFile);
    }

    public static StreamBasedXmlDocumentSupplier createReader(File file) {
        return createReader(file, USE_DOCUMENT_IDS_FROM_FILE_DEFAULT);
    }

    public static StreamBasedXmlDocumentSupplier createReader(File file, boolean useDocumentIdsFromFile) {
        FileReader reader;
        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            LOGGER.error("Couldn't create FileReader. Returning null.", e);
            return null;
        }
        return new StreamBasedXmlDocumentSupplier(reader, useDocumentIdsFromFile);
    }

    private StreamBasedXmlDocumentSupplier(FileReader reader, boolean useDocumentIdsFromFile) {
        this.useDocumentIdsFromFile = useDocumentIdsFromFile;
        this.xmlFileReader = reader;
        xmlParser = new SimpleReaderBasedXMLParser(reader, this);
    }

    @Override
    public Document getNextDocument() {
        if (xmlFileReader != null) {
            xmlParser.parse();
            if (document != null) {
                Document nextDocument = document;
                document = null;
                if (!useDocumentIdsFromFile) {
                    nextDocument.setDocumentId(getNextDocumentId());
                }
                ++documentCount;
                if (LOGGER.isInfoEnabled() && ((documentCount % 1000) == 0)) {
                    LOGGER.info("Read the " + documentCount + "th document from XML file.");
                }
                return nextDocument;
            } else {
                // The parser has reached the end of the file
                try {
                    xmlFileReader.close();
                } catch (IOException e) {
                    LOGGER.error("Error while closing the file reader used for reading the XML file.", e);
                }
                xmlFileReader = null;
            }
        }
        return null;
    }

    @Override
    public void setDocumentStartId(int documentStartId) {
        nextDocumentId = documentStartId;
    }

    protected int getNextDocumentId()
    {
        int tempId = nextDocumentId;
        ++nextDocumentId;
        return tempId;
    }

    @Override
    protected void finishedDocument(Document document) {
        this.document = document;
        xmlParser.stop();
    }
}
