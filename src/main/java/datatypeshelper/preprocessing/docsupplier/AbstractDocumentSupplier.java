package datatypeshelper.preprocessing.docsupplier;

public abstract class AbstractDocumentSupplier implements DocumentSupplier {

	protected int nextDocumentId;
	
	public AbstractDocumentSupplier() {
		nextDocumentId = 0;
	}
	
	public AbstractDocumentSupplier(int documentStartId) {
		nextDocumentId = documentStartId;
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
}
