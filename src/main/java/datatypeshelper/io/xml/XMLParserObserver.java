package datatypeshelper.io.xml;

public interface XMLParserObserver {

    public void handleOpeningTag(String tagString);

    public void handleClosingTag(String tagString);

    public void handleEmptyTag(String tagString);

    public void handleData(String data);
}
