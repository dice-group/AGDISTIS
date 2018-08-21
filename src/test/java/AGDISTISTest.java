
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.agdistis.algorithm.CandidateUtil;
import org.aksw.agdistis.algorithm.NEDAlgo_HITS;
import org.aksw.agdistis.datatypes.Document;
import org.aksw.agdistis.datatypes.NamedEntitiesInText;
import org.aksw.agdistis.datatypes.NamedEntityInText;
import org.aksw.agdistis.util.TripleIndexContext;
import org.aksw.agdistis.webapp.GetDisambiguation;
import org.junit.Test;

public class AGDISTISTest {

	@Test
	public void testUmlaute() throws InterruptedException, IOException {
		String taisho = "Emperor Taishō";
		String taishoURL = "http://dbpedia.org/resource/Emperor_Taishō";
		String japan = "Japan";
		String japanURL = "http://dbpedia.org/resource/Japan";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(taisho, taishoURL);
		correct.put(japan, japanURL);
		String preAnnotatedText = "<entity>" + taisho + "</entity> was the 123rd Emperor of <entity>" + japan
				+ "</entity>.";

		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d, null);

		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}
	@Test
	public void testMinExample()throws InterruptedException, IOException{
		//45907,,,,D,<entity http://d-nb.info/gnd/16323937-X;null>Schiffbau Matchbox</entity>,<entity http://d-nb.info/gnd/107521680X;null>Fratelli</entity>,,,,,STS Leiter Dokumentation,04/09/2017,"Wiederaufnahme: 23.2.2015, 22.10.2015, 15.09.2017",* Monatsplakat Juni; Z: Zeitung Nr. 8; Postkarte,,1,<entity http://d-nb.info/gnd/1061676641;http://www.wikidata.org/wiki/Q24044859>Antonio Vigano</entity>,,,,"<entity http://d-nb.info/gnd/1062324536;null>Silvan Kappeler</entity>, <entity http://d-nb.info/gnd/120763400;http://www.wikidata.org/wiki/Q1313462>Fabian Müller</entity>",,,"<entity http://d-nb.info/gnd/1084219425;null>Enrico Beeler</entity>, <entity null;http://www.wikidata.org/wiki/Q41121872>Petra Fischer</entity>",Regula,11/04/2013,1,"S, Ju",,"<entity null;null>Nicolas Dauwalder</entity>, <entity null;null>Rasmus Stahel</entity>",,,,1,0,,,,,SE,01/06/2013,1,<entity http://d-nb.info/gnd/10171987-5;http://www.wikidata.org/wiki/Q40313234>CH: Zürich: Schauspielhaus</entity>,,<entity http://d-nb.info/gnd/1061676641;http://www.wikidata.org/wiki/Q24044859>Antonio Vigano</entity>,,80002201306011,<entity http://d-nb.info/gnd/16323937-X;null>Zürich: Schauspielhaus Schiffbau Matchbox</entity>,2012/13,,<entity http://d-nb.info/gnd/1061676641;http://www.wikidata.org/wiki/Q24044859>Antonio Vigano</entity>; <entity null;null>Michele Fiocchi</entity>; <entity http://d-nb.info/gnd/157210030;null>Remo Rostagno</entity>; <entity http://d-nb.info/gnd/119513749;http://www.wikidata.org/wiki/Q3660267>Carmelo Samonà</entity>,80002,,,,
		//String preAnnotatedText = "45907,,,,D,<entity>Schiffbau Matchbox</entity>,<entity>Fratelli</entity>,,,,,STS Leiter Dokumentation,04/09/2017,\"Wiederaufnahme: 23.2.2015, 22.10.2015, 15.09.2017\",* Monatsplakat Juni; Z: Zeitung Nr. 8; Postkarte,,1,<entity>Antonio Vigano</entity>,,,,\"<entity>Silvan Kappeler</entity>, <entity>Fabian Müller</entity>\",,,\"<entity>Enrico Beeler</entity>, <entity>Petra Fischer</entity>\",Regula,11/04/2013,1,\"S, Ju\",,\"<entity>Nicolas Dauwalder</entity>, <entity>Rasmus Stahel</entity>\",,,,1,0,,,,,SE,01/06/2013,1,<entity>Schauspielhaus Zürich</entity>,,<entity>Antonio Vigano</entity>,,80002201306011,<entity>Zürich: Schauspielhaus Schiffbau Matchbox</entity>,2012/13,,<entity>Antonio Vigano</entity>; <entity>Michele Fiocchi</entity>; <entity>Remo Rostagno</entity>; <entity>Carmelo Samonà</entity>,80002,,,,";
		//String preAnnotatedText="49357,3,,1,D,<entity>Stadttheater</entity>,<entity>Ränke und Schwänke. Lebensbilder mit Gesang in drei Akten</entity>,,,,,STS Leiter Dokumentation,04/09/2017,,\"* Weiss, <entity>Das Basler Stadttheater</entity> 1834 - 1934 (5.3.2: Base 1 (1))\",,,,,,,,,,,Regula,25/01/2013,,Sm,,,<entity>Eduard Stiegmann</entity>,,,0,0,,,,,,9.10.1854,1,<entity>CH: Basel: Stadttheater</entity>,,,,40001185410091,<entity>Basel: Stadttheater Stadttheater</entity>,1854/55,,<entity>Georg Starke</entity>,40001,,,,";
		String preAnnotatedText="14548,3,,22,D,,<entity>Csárdásfürstin. Operette in 3 Akten</entity>,,,,,STS Leiter Dokumentation,04/09/2017,,\"* 25 Fotos, 9 Dias im Aussenlager\",,2590,<entity>Tobias Dinslage</entity>,,<entity>Lea Havas</entity>,<entity>Lech-Rudolf Gorywoda</entity>,\"<entity>Joachim Wolf</entity>, <entity>Barbara Giesecke</entity>, <entity>Tom Martinsen</entity>, <entity>Márta Kosztolányi</entity>, <entity>Andreas Herrmann</entity>, <entity>Snezana Stamenkovic</entity>, <entity>Simon Ginsberg</entity>, <entity>Hans-Joachim Frick</entity>, <entity>Laszlo Turek</entity>, <entity>Gunter Elmer</entity>, <entity>Vesselin Ouroumov</entity>\",9,,<entity>Klemens Renoldner</entity>,<entity>Christian Schneeberger</entity>,31/03/2003,25,Ot,<entity>Dagmar Schlenzka</entity>,,<entity>Emmerich Kálmán</entity>,0,,1,1,,91,,,NI,15/12/1996,1,<entity>CH: Bern: Stadttheater</entity>,,<entity>Markus Hertel</entity>,,30001199612151,\"<entity>Bern: Stadttheater</entity> \",1996/97,,<entity>Leo Stein</entity>; <entity>Béla Jenbach</entity>,30001,,,,";
		//String preAnnotatedText="<entity>CH: Bern: Stadttheater</entity>";
		//String preAnnotatedText="<entity>Csárdásfürstin. Operette in 3 Akten</entity>";
		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);

		agdistis.run(d, null);
		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			//assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}
		assertTrue(true);
	}
	@Test
	public void testFile()throws InterruptedException, IOException{
		List<String> data =new ArrayList<String>();
		String dataStr="";
		FileInputStream fstream = new FileInputStream("C:/Users/Daniel/Desktop/IAA/trainingSetReadyLinked.csv");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		strLine = br.readLine();
		while ((strLine = br.readLine()) != null)   {
			// Print the content on the console
			data.add(strLine.replaceAll("<entity\\s?(https?://[A-Z  a-z 0-9 \\- \\\\. /]+|null);(https?://[ A-Z a-z 0-9 \\- \\\\. /]+|null)>","<entity>"));
			dataStr+=strLine;
		}
		br.close();
		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		PrintWriter writer = new PrintWriter("C:/Users/Daniel/Desktop/output.txt", "UTF-8");


		for(String preAnnotatedText:data) {
			Document d = GetDisambiguation.textToDocument(preAnnotatedText);

			agdistis.run(d, null);
			NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
			HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
			writer.println(preAnnotatedText);
			for (NamedEntityInText namedEntity : namedEntities) {
				String disambiguatedURL = namedEntity.getNamedEntityUri();
				results.put(namedEntity, disambiguatedURL);
			}

			for (NamedEntityInText namedEntity : namedEntities) {
				String disambiguatedURL = namedEntity.getNamedEntityUri();
				System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
				Pattern pattern = Pattern.compile("<entity\\s?(https?://[A-Z  a-z 0-9 \\- \\\\. /]+|null);(https?://[ A-Z a-z 0-9 \\- \\\\. /]+|null)>"+namedEntity.getLabel()+"</entity>");
				Matcher matcher = pattern.matcher(dataStr);
				if (matcher.find())
				{
					writer.println(matcher.group(0));
				}
				writer.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			}
		}
		writer.close();


	}
	@Test
	public void testMinimalExample() throws InterruptedException, IOException {
		String obama = "Barack Obama";
		String obamaURL = "http://dbpedia.org/resource/Barack_Obama";
		String merkel = "Angela Merkel";
		String merkelURL = "http://dbpedia.org/resource/Angela_Merkel";
		String city = "Berlin";
		String cityURL = "http://dbpedia.org/resource/Berlin";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(obama, obamaURL);
		correct.put(merkel, merkelURL);
		correct.put(city, cityURL);

		String preAnnotatedText = "<entity>" + obama + "</entity> visits <entity>" + merkel + "</entity> in <entity>"
				+ city + "</entity>.";

		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d, null);

		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}

	@Test
	/**
	 * This test ensures that entities consiting only of punctations, which are
	 * later reduced to a string of length 0 by
	 * https://github.com/dice-group/AGDISTIS/blob/master/src/main/java/org/aksw/agdistis/util/Stemming.java#L91
	 * are not causing exceptions
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void testinterpunctation() throws InterruptedException, IOException {
		String question = "???";
		String questionURL = "???";

		HashMap<String, String> correct = new HashMap<String, String>();
		correct.put(question, questionURL);

		String preAnnotatedText = "<entity>???</entity>.";

		NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
		Document d = GetDisambiguation.textToDocument(preAnnotatedText);
		agdistis.run(d, null);

		NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
		HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			results.put(namedEntity, disambiguatedURL);
		}
		for (NamedEntityInText namedEntity : namedEntities) {
			String disambiguatedURL = namedEntity.getNamedEntityUri();
			System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
			assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
		}

	}

	@Test
	public void testContext() throws InterruptedException, IOException {
		String angelina = "Angelina";
		String angelinaURL = "http://dbpedia.org/resource/Angelina_Jolie";
		String brad = "Brad";
		String bradURL = "http://dbpedia.org/resource/Brad_Pitt";
		String jon = "Jon";
		String jonURL = "http://dbpedia.org/resource/Jon_Voight";

		// load properties to see if the context index exists
		Properties prop = new Properties();
		InputStream input = CandidateUtil.class.getResourceAsStream("/config/agdistis.properties");
		prop.load(input);
		String envContext = System.getenv("AGDISTIS_CONTEXT");
		Boolean context = Boolean.valueOf(envContext != null ? envContext : prop.getProperty("context"));
		if (context == true) { // in case the index by context exist

			HashMap<String, String> correct = new HashMap<String, String>();
			correct.put(angelina, angelinaURL);
			correct.put(jon, jonURL);
			correct.put(brad, bradURL);

			String preAnnotatedText = "<entity>" + angelina + "</entity>, her father <entity>" + jon
					+ "</entity>, and her partner <entity>" + brad
					+ "</entity> never played together in the same movie.";

			NEDAlgo_HITS agdistis = new NEDAlgo_HITS();
			Document d = GetDisambiguation.textToDocument(preAnnotatedText);
			agdistis.run(d, null);

			NamedEntitiesInText namedEntities = d.getNamedEntitiesInText();
			HashMap<NamedEntityInText, String> results = new HashMap<NamedEntityInText, String>();
			for (NamedEntityInText namedEntity : namedEntities) {
				String disambiguatedURL = namedEntity.getNamedEntityUri();
				results.put(namedEntity, disambiguatedURL);
			}
			for (NamedEntityInText namedEntity : namedEntities) {
				String disambiguatedURL = namedEntity.getNamedEntityUri();
				System.out.println(namedEntity.getLabel() + " -> " + disambiguatedURL);
				assertTrue(correct.get(namedEntity.getLabel()).equals(disambiguatedURL));
			}
		}
	}

}
