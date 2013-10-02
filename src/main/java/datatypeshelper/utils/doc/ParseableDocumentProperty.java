package datatypeshelper.utils.doc;

/**
 * This interface describes {@link DocumentProperty} classes with the ability to
 * parse their value from a given String. This can be useful for reading /
 * parsing documents and their properties from a text without knowing the exact
 * way in which the different properties have to be parsed.
 * 
 * @author m.roeder
 * 
 */
public interface ParseableDocumentProperty extends DocumentProperty {

	/**
	 * This method is used to let the DocumentProperty parse its value from a
	 * given String. After performing this method the DocumentProperty should
	 * have the value contained inside this String.
	 * 
	 * @param value
	 */
	public void parseValue(String value);
}
