package datatypeshelper.preprocessing.docsupplier;
import datatypeshelper.utils.doc.Document;

public interface DocumentSupplier {

	public Document getNextDocument();
	
	public void setDocumentStartId(int documentStartId);
}
