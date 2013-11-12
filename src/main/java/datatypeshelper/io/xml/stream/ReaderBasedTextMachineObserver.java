package datatypeshelper.io.xml.stream;

public interface ReaderBasedTextMachineObserver {

    public void foundPattern(int patternId, String data, String patternMatch);
}
