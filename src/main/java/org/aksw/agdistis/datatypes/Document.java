package org.aksw.agdistis.datatypes;

import java.io.Serializable;

public class Document implements Comparable<Document>, Serializable {

	private static final long serialVersionUID = -3213426637730517409L;

	protected int documentId;

	private DocumentText text;

	private NamedEntitiesInText nes;

	public Document() {
	}

	public Document(int documentId) {
		this.documentId = documentId;
	}

	public int getDocumentId() {
		return documentId;
	}

	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Document) {
			return this.documentId == ((Document) obj).getDocumentId();
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int compareTo(Document document) {
		if (this.documentId == document.getDocumentId()) {
			return 0;
		}
		return this.documentId < document.getDocumentId() ? -1 : 1;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Document id=" + documentId);
		result.append("\n[ ");
		result.append("]\n");
		return result.toString();
	}

	public void addText(DocumentText text) {
		this.text = text;
	}

	public NamedEntitiesInText getNamedEntitiesInText() {
		return this.nes;
	}

	public DocumentText DocumentText() {
		return this.text;
	}

	public void addNamedEntitiesInText(NamedEntitiesInText nes) {
		this.nes = nes;
	}

}