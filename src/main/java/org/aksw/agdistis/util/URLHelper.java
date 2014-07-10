package org.aksw.agdistis.util;

public class URLHelper {
    /**
     * A URL consists of the two parts <code>&lt;sheme&gt;:&lt;sheme-specific-part&gt;</code>. This method only accepts
     * URLs which have a letter or number inside their sheme-specific-part. So "http://" is no URL.
     * 
     * @param object
     * @return
     */
    public static boolean isURL(String string) {
        int pos = string.indexOf(':');
        if (pos > 0) {
            for (int i = pos + 1; i < string.length(); ++i) {
                if (Character.isLetterOrDigit(string.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}