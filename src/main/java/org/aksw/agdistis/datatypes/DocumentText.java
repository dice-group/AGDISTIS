package org.aksw.agdistis.datatypes;

public class DocumentText {

	protected String text;

	public DocumentText() {
	}

	public DocumentText(String text) {
		this.text = text;
	}

	/**
	 * Set the value of text
	 * 
	 * @param text
	 *            the new value of text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get the value of text
	 * 
	 * @return the value of text
	 */
	public String getText() {
		return text;
	}

	public Object getValue() {
		return getText();
	}

	public void parseValue(String value) {
		this.text = value;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 == null) {
			return false;
		}
		if (arg0 instanceof DocumentText) {
			if (this.text == null) {
				return ((DocumentText) arg0).text == null;
			} else {
				return this.text.equals(((DocumentText) arg0).text);
			}
		} else {
			return super.equals(arg0);
		}
	}

}