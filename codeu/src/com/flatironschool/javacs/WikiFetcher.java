package CLIapplication.javacs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class WikiFetcher {
	private long lastRequestTime = -1;
	private long minInterval = 1000;

	/**
	 * Fetches and parses a URL string, returning a list of paragraph elements.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements fetchWikipedia(String url) throws IOException {
		sleepIfNeeded();

		// download and parse the document
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();

		// select the content text and pull out the paragraphs.
		//Anwaar - Change id to "main" for NYT pages. 
		Element content = doc.getElementById("mw-content-text");
		//Element content = doc.getElementById("main");

		// TODO: avoid selecting paragraphs from sidebars and boxouts
		Elements paras = content.select("p");
		return paras;
	}

	public void fetchWikipediaPic(String url) throws IOException{
		sleepIfNeeded();
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
    	Element body = doc.body();
    	Elements tables = body.getElementsByTag("table");
  	  	for (Element table : tables) {
        	if (table.className().contains("infobox")==true) {
            	//Elements e = table.getElementsByClass("image"));
            	//System.out.println(url);
			    String stub = table.getElementsByClass("image").first().attr("href");
			    stub = stub.substring(5);
				System.out.println(url + "#/media" + stub);
            	return;
            	//String image = doc.select("img").first().text();
        	}
    	}
	}

	/**
	 * Reads the contents of a Wikipedia page from src/resources.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements readWikipedia(String url) throws IOException {
		URL realURL = new URL(url);

		// assemble the file name
		String slash = File.separator;
		String filename = "resources" + slash + realURL.getHost() + realURL.getPath();

		// read the file
		InputStream stream = WikiFetcher.class.getClassLoader().getResourceAsStream(filename);
		Document doc = Jsoup.parse(stream, "UTF-8", filename);

		// TODO: factor out the following repeated code
		Element content = doc.getElementById("mw-content-text");
		Elements paras = content.select("p");
		return paras;
	}

	/**
	 * Rate limits by waiting at least the minimum interval between requests.
	 */
	private void sleepIfNeeded() {
		if (lastRequestTime != -1) {
			long currentTime = System.currentTimeMillis();
			long nextRequestTime = lastRequestTime + minInterval;
			if (currentTime < nextRequestTime) {
				try {
					//System.out.println("Sleeping until " + nextRequestTime);
					Thread.sleep(nextRequestTime - currentTime);
				} catch (InterruptedException e) {
					System.err.println("Warning: sleep interrupted in fetchWikipedia.");
				}
			}
		}
		lastRequestTime = System.currentTimeMillis();
	}
}
