/**
 *
 */
package de.bluekiwi.labs.experiment;


/**
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>, Ricardo Usbeck <ricardo.usbeck@googlemail.com>
 * 
 */
public class News500GoldStandard {
    //
    // /**
    // * normal triples are basically triples with owl:ObjectProperties
    // */
    // public static Map<String, ObjectPropertyTriple> GOLD_STANDARD_TRIPLES = new HashMap<String,
    // ObjectPropertyTriple>();
    // /**
    // * say triples have strings as values
    // */
    // private static Map<String, Triple> GOLD_STANDARD_SAY_TRIPLES = new HashMap<String, Triple>();
    //
    // public static void main(String[] args) throws IOException {
    // System.out.println("<?xml version=\"1.0\" " +
    // "encoding=\"UTF-8\"?>" +
    // "<corpus xmlns=\"http://semweb.unister.de/xml-corpus-schema-2013\">");
    // String pathname = "/Users/ricardousbeck/Dropbox/patterns_annotated.txt";
    // int count = 0;
    // for (Object o : FileUtils.readLines(new File(pathname))) {
    //
    // String line = (String) o;
    // String[] lineParts = line.replace("______", "___ ___").split("___");
    // if (lineParts[0].equals("NORMAL")) {
    //
    // ObjectPropertyTriple triple = new ObjectPropertyTriple(lineParts[1], lineParts[2], lineParts[3], lineParts[4],
    // lineParts[5], new HashSet<Integer>(Arrays.asList(Integer
    // .valueOf(lineParts[7]))));
    //
    // String subjectLabel = triple.getSubjectLabel();
    // int subjectIndex = lineParts[8].indexOf(subjectLabel);
    // String objectLabel = triple.getObjectLabel();
    // int objectIndex = lineParts[8].indexOf(objectLabel);
    //
    // List<Label> tmp = new ArrayList<Label>();
    // List<Candidate> arrayList = new ArrayList<Candidate>();
    // arrayList.add(new Candidate(triple.getSubjectUri(), subjectLabel, ""));
    // tmp.add(new Label(-1, subjectIndex, subjectIndex + subjectLabel.length(), subjectLabel, arrayList));
    // arrayList = new ArrayList<Candidate>();
    // arrayList.add(new Candidate(triple.getObjectUriPrefixed(), objectLabel, ""));
    // tmp.add(new Label(-1, objectIndex, objectIndex + objectLabel.length(), objectLabel, arrayList));
    // Collections.sort(tmp, new LabelLengthComparator());
    // Collections.reverse(tmp);
    // TextWithLabels text = new TextWithLabels(lineParts[8], tmp);
    // String tmpText = "<Document id=\"" + count + "\"><TextWithNamedEntities>";
    // tmpText += "<SimpleTextPart>";
    // tmpText += markupText(text);
    // tmpText += "</SimpleTextPart>";
    // tmpText += "</TextWithNamedEntities></Document>";
    // System.out.println(tmpText);
    // count++;
    // }
    // else if (lineParts[0].equals("SAY")) {
    //
    // DatatypePropertyTriple triple = new DatatypePropertyTriple(lineParts[1], lineParts[2], lineParts[3],
    // lineParts[5], new HashSet<Integer>(Arrays.asList(Integer.valueOf(lineParts[7]))));
    // GOLD_STANDARD_SAY_TRIPLES.put(triple.getKey(), triple);
    // }
    // else
    // throw new RuntimeException("WOWOWW: " + line);
    // }
    // System.out.println("</corpus>");
    // }
    //
    // private static String markupText(TextWithLabels text) {
    // List<String> textParts = new ArrayList<String>();
    // List<Label> labels = text.getLabels();
    // String originalText = text.getText();
    // // start with the last label and add the parts of the new text beginning with its end to the array
    // // Note that we are expecting that the labels are sorted descending by there position in the text!
    // int startFormerLabel = originalText.length();
    // for (Label currentLabel : labels) {
    // // proof if this label undercuts the last one.
    // if (startFormerLabel >= currentLabel.getEnd()) {
    // // append the text between this label and the former one
    // textParts.add(originalText.substring(currentLabel.getEnd(), startFormerLabel));
    // // append the markedup label
    // String tmp = "</SimpleTextPart><NamedEntityInText uri=\"";
    // tmp += currentLabel.getCandidates().get(0).getUrl();
    // tmp += "\">";
    // tmp += currentLabel.getLabel();
    // tmp += "</NamedEntityInText><SimpleTextPart>";
    // textParts.add(tmp);
    // // remember the start position of this label
    // startFormerLabel = currentLabel.getStart();
    // }
    // else {
    // System.out.println("Label undercuts another label. TextId: " + text.getId());
    // }
    // }
    // textParts.add(originalText.substring(0, startFormerLabel));
    // // Form the new text beginning with its end
    // StringBuilder textWithMarkups = new StringBuilder();
    // for (int i = textParts.size() - 1; i >= 0; --i) {
    // textWithMarkups.append(textParts.get(i));
    // }
    // return textWithMarkups.toString();
    // }

}
