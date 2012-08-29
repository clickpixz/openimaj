package org.openimaj.text.nlp.namedentity;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.store.SimpleFSDirectory;
import org.openimaj.text.nlp.namedentity.YagoEntityContextScorerFactory.YagoEntityContextScorer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EntityExtractionResourceBuilder {

	private static String DEFAULT_ALIAS_NAME = "AliasMapFile.txt";
	private static String DEFAULT_CONTEXT_NAME = "YagoLucene";
	private static String DEFAULT_ROOT_NAME = ".YagoEntityExtraction";
	private static String wikiApiPrefix = "http://en.wikipedia.org/w/api.php?format=xml&action=query&titles=";
	private static String wikiApiSuffix = "&prop=revisions&rvprop=content";
	private boolean verbose = true;
	private boolean validate = true;
	private boolean locations = false;

	public void buildCandidateAliasFile(String seedDirectoryPath) {
		buildCandidateAliasFile(seedDirectoryPath, getDefaultRootPath()
				+ File.separator + DEFAULT_ALIAS_NAME);
	}

	public void buildCandidateAliasFile(String seedDirectoryPath,
			String destinationPath) {
		writeAliasFile(getEntities(seedDirectoryPath), destinationPath,
				seedDirectoryPath);
	}

	public void buildContextLuceneIndex(String seedDirectoryPath) {
		buildContextLuceneIndex(seedDirectoryPath, getDefaultRootPath()
				+ File.separator + DEFAULT_CONTEXT_NAME);
	}

	public void buildContextLuceneIndex(String seedDirectoryPath,
			String destinationPath) {
		try {
			buildIndex(getEntities(seedDirectoryPath), destinationPath,
					seedDirectoryPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void buildAll(String seedDirectoryPath) {
		validateFileStructure();
		buildAll(seedDirectoryPath, getDefaultRootPath());
	}

	public void buildAll(String seedDirectoryPath, String destinationPath) {
		// Get the entities as people and organisations
		print("Building All...");
		HashMap<String, YagoNamedEntity> entities = getEntities(seedDirectoryPath);
		writeAliasFile(entities, destinationPath + File.separator
				+ DEFAULT_ALIAS_NAME, seedDirectoryPath);
		try {
			buildIndex(entities, destinationPath + File.separator
					+ DEFAULT_CONTEXT_NAME, seedDirectoryPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		print("Done");
	}

	public static String getDefaultRootPath() {
		return System.getProperty("user.home") + File.separator
				+ DEFAULT_ROOT_NAME;
	}

	public static String getDefaultAliasFilePath(){
		return getDefaultRootPath()+File.separator+DEFAULT_ALIAS_NAME;
	}

	public static String getDefaultIndexDirectoryPath(){
		return getDefaultRootPath()+File.separator+DEFAULT_CONTEXT_NAME;
	}

	public static String getAliasFrom(String rootName) {
		String result;
		String noGeo = null;
		if (rootName.startsWith("geoent_")) {
			noGeo = rootName.substring(rootName.indexOf('_') + 1,
					rootName.lastIndexOf('_'));
		} else
			noGeo = rootName;
		String spaces = noGeo.replaceAll("_", " ");
		String noParen;
		if (spaces.contains("("))
			noParen = spaces.substring(0, spaces.indexOf("("));
		else
			noParen = spaces;
		String dropComma;
		if (noParen.contains(","))
			dropComma = noParen.substring(0, spaces.indexOf(","));
		else
			dropComma = noParen;
		result = dropComma;
		return result;
	}

	private void validateFileStructure() {
		File rootDir = new File(getDefaultRootPath());
		if (!rootDir.isDirectory()) {
			rootDir.mkdir();
		}
		File indexDir = new File(getDefaultRootPath() + File.separator
				+ DEFAULT_CONTEXT_NAME);
		if (!indexDir.isDirectory()) {
			indexDir.mkdir();
		}
		else{
			for(File f : indexDir.listFiles())f.delete();
		}
	}

	private void buildIndex(HashMap<String, YagoNamedEntity> entities,
			String destinationPath, String seedDirectoryPath)
			throws IOException {
		print("Building Index...");
		setEntityContextValues(entities, seedDirectoryPath);
		print("Initializing Lucene objects...");
		
		//initialize lucene objects
		String[] names = { "Company", "Context" };
		FieldType[] types;
		FieldType ti = new FieldType();
		ti.setIndexed(true);
		ti.setTokenized(true);
		ti.setStored(true);
		FieldType n = new FieldType();
		n.setStored(true);
		n.setIndexed(true);
		types = new FieldType[3];
		types[0] = n;
		types[1] = ti;
		File f = new File(destinationPath);
		QuickIndexer qi = new QuickIndexer(new SimpleFSDirectory(f));
		
		//Initialize wiki objects
		DocumentBuilderFactory docBuilderFactory= DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null; 
		Document doc;		
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {			
			e.printStackTrace();
		}
		doc = null;
		WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}",
				"http://www.mywiki.com/wiki/${title}");		
		int count=0;
		print("Building Lucene Index...");
		for (YagoNamedEntity entity : entities.values()) {
			count++;
			if(count%5000==0)print("Processed "+count);
			// if wikiURL, add wiki to context
			if (entity.wikiURL != null) {
				String title = entity.wikiURL.substring(entity.wikiURL.lastIndexOf("/") + 1);
				try {
					doc = docBuilder.parse(wikiApiPrefix + title
							+ wikiApiSuffix);
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				doc.getDocumentElement().normalize();
				NodeList revisions = doc.getElementsByTagName("rev");
				if (revisions.getLength() > 0) {
					String markup = revisions.item(0).getTextContent();

					// convert markup dump to plaintext.
					String plainStr = wikiModel.render(
							new PlainTextConverter(), markup);
					// add it to the context.
					entity.addContext(plainStr);
				}
			}

			String[] values = { entity.rootName, entity.getContext() };
			qi.addDocumentFromFields(names, values, types);
		}
		qi.finalise();
	}

	private void setEntityContextValues(
			final HashMap<String, YagoNamedEntity> entities,
			String seedDirectoryPath) {
		print("Setting Context Values...");
		BufferedReader in = null;
		// Created
		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "created_stripped.tsv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		StreamLooper sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = values[1];
				String context = convertResource(values[2]);
				if (entities.keySet().contains(rootName)) {
					entities.get(rootName).addContext(context);
				}
			}
		};
		sl.loop();

		// wikiAnchorText
		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "hasWikipediaAnchorText_stripped.tsv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = values[1];
				String context = convertLiteral(values[2]);
				if (entities.keySet().contains(rootName)) {
					entities.get(rootName).addContext(context);
				}
			}
		};
		sl.loop();

		// wikiUrl

		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "hasWikipediaUrl_stripped.tsv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = values[1];
				if (entities.keySet().contains(rootName)) {
					entities.get(rootName).wikiURL = values[2].replaceAll("\"",
							"");
				}
			}
		};
		sl.loop();
		// validate
		print("Validating Context...");
		int noContext = 0;
		for (YagoNamedEntity ne : entities.values()) {
			for (String alias : ne.aliasList) {
				ne.addContext(alias);
			}
			if ((ne.getContext() == null || ne.getContext().equals(""))
					&& ne.wikiURL == null) {
				noContext++;
			}
		}
		print("No Context: " + noContext);
	}

	private void setEntityAliasValues(
			final HashMap<String, YagoNamedEntity> entities,
			String seedDirectoryPath) {
		print("Setting Alias Values...");
		// Populate 'isCalled'
		BufferedReader in = null;
		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "isCalled_stripped.tsv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		StreamLooper sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = values[1];
				String alias = convertLiteral(values[2]);
				if (entities.keySet().contains(rootName)) {
					entities.get(rootName).addAlias(alias);
				}
			}
		};
		sl.loop();

		// populate 'means'

		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "means_stripped.tsv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = values[2];
				String alias = convertLiteral(values[1]);
				// System.out.println(alias);
				if (entities.keySet().contains(rootName)) {
					entities.get(rootName).addAlias(alias);
				}
			}
		};
		sl.loop();
		print("Validating Aliases...");
		int noAliases = 0;
		for (YagoNamedEntity ne : entities.values()) {
			if (ne.aliasList.size() == 0) {
				String alias = getAliasFrom(ne.rootName);
				if (!(alias == null || alias.length() == 0))
					ne.addAlias(alias);
				if (ne.aliasList.size() == 0)
					noAliases++;
			}
		}
		print("No alias: " + noAliases);
	}

	private void writeAliasFile(HashMap<String, YagoNamedEntity> entities,
			String destinationPath, String seedDirectoryPath) {
		setEntityAliasValues(entities, seedDirectoryPath);

		BufferedWriter w;
		try {
			w = openFileAsWriteStream(destinationPath);
			w.write("");
			for (YagoNamedEntity ne : entities.values()) {
				if (ne.aliasList.size() > 0) {
					w.append("+" + ne.rootName + "\n");
					for (String alias : ne.aliasList) {
						w.append("." + alias + "\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private HashMap<String, YagoNamedEntity> getEntities(
			String seedDirectoryPath) {
		print("Getting Entities...");
		final HashMap<String, YagoNamedEntity> result = new HashMap<String, YagoNamedEntity>();
		BufferedReader in = null;
		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "wordnet_person_100007846.txt");
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}
		// get People
		StreamLooper sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = convertLiteral(values[1]);
				if (!rootName.startsWith("Category:")) {
					YagoNamedEntity ne = new YagoNamedEntity(rootName,
							NamedEntity.Type.Person);
					result.put(rootName, ne);
				}
			}
		};
		sl.loop();

		// get Organisations
		try {
			in = openFileAsReadStream(seedDirectoryPath + File.separator
					+ "wordnet_organization_108008335.txt");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		sl = new StreamLooper(in) {
			@Override
			protected void doWork(String s) {
				String[] values = s.split("\\s+");
				String rootName = convertLiteral(values[1]);
				if (!(rootName.startsWith("Category:") || rootName
						.startsWith("geoent_"))) {
					YagoNamedEntity ne = new YagoNamedEntity(rootName,
							NamedEntity.Type.Organisation);
					result.put(rootName, ne);
				}
			}
		};
		sl.loop();

		if (locations) {
			// get Locations
			try {
				in = openFileAsReadStream(seedDirectoryPath + File.separator
						+ "wordnet_location_100027167.txt");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			sl = new StreamLooper(in) {
				@Override
				protected void doWork(String s) {
					String[] values = s.split("\\s+");
					String rootName = convertLiteral(values[1]);
					if (!rootName.startsWith("Category:")) {
						YagoNamedEntity ne = new YagoNamedEntity(rootName,
								NamedEntity.Type.Location);
						result.put(rootName, ne);
					}
				}
			};
			sl.loop();
		}
		print("Total Entities: " + result.size());
		return result;
	}

	public static BufferedReader openFileAsReadStream(String path)
			throws FileNotFoundException {
		FileReader fr = null;
		fr = new FileReader(path);
		BufferedReader br = new BufferedReader(fr);
		return br;
	}

	public static BufferedWriter openFileAsWriteStream(String path)
			throws IOException {
		FileWriter fw = null;
		fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		return bw;
	}

	private static String convertLiteral(String literal) {
		String escaped = StringEscapeUtils.unescapeJava(literal);
		String first = null;
		if (escaped.startsWith("\""))
			first = escaped.substring(1);
		else
			first = escaped;
		if (first.endsWith("\""))
			return first.substring(0, first.length() - 1);
		else
			return first;
	}

	private static String convertResource(String literal) {
		String escaped = StringEscapeUtils.unescapeJava(literal);
		return escaped.replaceAll("_", " ");
	}

	private void print(String message) {
		if (verbose)
			System.out.println(message);
	}

	public static void main(String[] args) {
		new EntityExtractionResourceBuilder().buildAll(args[0]);
	}

	public static abstract class StreamLooper {
		BufferedReader reader;

		public StreamLooper(BufferedReader reader) {
			this.reader = reader;
		}

		public void loop() {
			String s = null;
			try {
				while ((s = reader.readLine()) != null) {
					doWork(s);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		protected abstract void doWork(String s);
	}

}