package datatypeshelper.io.xml.stream;

public class ReaderBasedXMLTextMachine extends AbstractReaderBasedTextMachine {

    public static final int XML_COMMENT_PATTERN_ID = 0;
    public static final int XML_TAG_PATTERN_ID = 1;
    public static final int XML_ENCODED_CHAR_PATTERN_ID = 2;

    private int state = 0;
    protected StringBuilder dataBuffer = new StringBuilder();
    protected StringBuilder patternBuffer = new StringBuilder();

    @Override
    protected void processNextChar(char c, ReaderBasedTextMachineObserver observer) {
        switch (state) {
        case 0: {
            switch (c) {
            case '<': {
                patternBuffer.append(c);
                state = 1;
                break;
            }
            case '&': {
                patternBuffer.append(c);
                state = 8;
                break;
            }
            default: {
                dataBuffer.append(c);
            }
            }
            break;
        }
        case 1: { // Saw "<"
            if (c == '!') {
                state = 2;
            } else {
                state = 7;
            }
            patternBuffer.append(c);
            break;
        }
        case 2: { // Saw "<!"
            if (c == '-') {
                state = 3;
            } else {
                state = 7;
            }
            patternBuffer.append(c);
            break;
        }
        case 3: { // Saw "<!-"
            if (c == '-') {
                state = 4;
            } else {
                state = 7;
            }
            patternBuffer.append(c);
            break;
        }
        case 4: { // Saw "<!--"
            if (c == '-') {
                state = 5;
            }
            patternBuffer.append(c);
            break;
        }
        case 5: { // Saw "<!--[^-]*-"
            if (c == '-') {
                state = 6;
            } else {
                state = 4;
            }
            patternBuffer.append(c);
            break;
        }
        case 6: { // Saw "<!--[^-]*--"
            patternBuffer.append(c);
            if (c == '>') {
                observer.foundPattern(XML_COMMENT_PATTERN_ID, dataBuffer.toString(), patternBuffer.toString());
                dataBuffer.delete(0, dataBuffer.length());
                patternBuffer.delete(0, patternBuffer.length());
                state = 0;
            } else {
                state = 4;
            }
            break;
        }
        case 7: { // Saw "<[^>]*"
            patternBuffer.append(c);
            if (c == '>') {
                observer.foundPattern(XML_TAG_PATTERN_ID, dataBuffer.toString(), patternBuffer.toString());
                dataBuffer.delete(0, dataBuffer.length());
                patternBuffer.delete(0, patternBuffer.length());
                state = 0;
            }
            break;
        }
        case 8: { // Saw "&"
            switch (c) {
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '#': {
                state = 9;
                patternBuffer.append(c);
                break;
            }
            case '<': {
                state = 1;
                dataBuffer.append('&');
                patternBuffer.delete(0, patternBuffer.length());
                patternBuffer.append(c);
                break;
            }
            default: {
                state = 0;
                patternBuffer.delete(0, patternBuffer.length());
                dataBuffer.append('&');
                dataBuffer.append(c);
            }
            }
            break;
        }
        case 9: { // Saw "&[#a-zA-Z]"
            if (patternBuffer.length() > 7) {
                // no encoded character has such a long encoding
                dataBuffer.append(patternBuffer.toString());
                patternBuffer.delete(0, patternBuffer.length());
                dataBuffer.append(c);
                state = 0;
                break;
            }
            switch (c) {
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9': {
                patternBuffer.append(c);
                break;
            }
            case '<': {
                state = 1;
                dataBuffer.append(patternBuffer.toString());
                patternBuffer.delete(0, patternBuffer.length());
                patternBuffer.append(c);
                break;
            }
            case ';': {
                patternBuffer.append(c);
                observer.foundPattern(XML_ENCODED_CHAR_PATTERN_ID, dataBuffer.toString(), patternBuffer.toString());
                dataBuffer.delete(0, dataBuffer.length());
                patternBuffer.delete(0, patternBuffer.length());
                state = 0;
                break;
            }
            default: {
                state = 0;
            }
            }
            break;
        }
        }// switch (state)
    }
}
