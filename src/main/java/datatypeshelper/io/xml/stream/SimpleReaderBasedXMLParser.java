package datatypeshelper.io.xml.stream;

import java.io.Reader;

import org.apache.commons.lang3.StringEscapeUtils;

import datatypeshelper.io.xml.XMLParserObserver;

public class SimpleReaderBasedXMLParser implements ReaderBasedTextMachineObserver {

    private ReaderBasedXMLTextMachine textParser = new ReaderBasedXMLTextMachine();
    private XMLParserObserver observer;
    private Reader reader;
    private StringBuilder dataBuffer = new StringBuilder();

    public SimpleReaderBasedXMLParser(Reader reader, XMLParserObserver observer) {
        this.reader = reader;
        this.observer = observer;
    }

    public void parse() {
        textParser.analyze(reader, this);
    }

    @Override
    public void foundPattern(int patternId, String data, String patternMatch) {
        dataBuffer.append(data);
        if (patternId == ReaderBasedXMLTextMachine.XML_ENCODED_CHAR_PATTERN_ID) {
            dataBuffer.append(StringEscapeUtils.unescapeXml(patternMatch));
        } else if (patternId == ReaderBasedXMLTextMachine.XML_TAG_PATTERN_ID) {
            if (dataBuffer.length() > 0) {
                observer.handleData(dataBuffer.toString());
                dataBuffer.setLength(0);
            }
            if (patternMatch.startsWith("</")) {
                observer.handleClosingTag(patternMatch.substring(2, patternMatch.length() - 1));
            } else {
                if (patternMatch.endsWith("/>")) {

                } else {
                    observer.handleOpeningTag(patternMatch.substring(1, patternMatch.length() - 1));
                }
            }
        }
    }

    public void stop() {
        textParser.stop();
    }
}
