package CLIapplication.javacs;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;

public class WikiCrawler {
	// keeps track of where we started
	private final String source;

	// the index where the results go
	private JedisIndex index;

	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();

	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();
	
	private static String visitedUrl;
	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
		if (queue.isEmpty()) {
			return null;
		}
		visitedUrl = queue.peek();
		String url = queue.poll();
		System.out.println("Crawling " + url);

		if (index.isIndexed(url)) {
			System.out.println("Already indexed.");
			return null;
		}
		//Simplified since we are not using test case. Can be added back for testing purposes.
		Elements paragraphs;
		try {
			paragraphs = wf.fetchWikipedia(url);
			index.indexPage(url, paragraphs);
			queueInternalLinks(paragraphs);		
		}catch(Exception e) {
			
		}
	
		//wf.fetchWikipediaPic(url);	
		return url;
	}

	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
		for (Element paragraph: paragraphs) {
			queueInternalLinks(paragraph);
		}
	}

	/**
	 * Parses a paragraph and adds internal links to the queue.
	 * 
	 * @param paragraph
	 */
	private void queueInternalLinks(Element paragraph) {
		Elements elts = paragraph.select("a[href]");


		for (Element elt: elts) {
			String relURL = elt.attr("href");

			if (relURL.startsWith("/wiki/")) {
				String absURL = "https://en.wikipedia.org" + relURL;
				//System.out.println(absURL);
				queue.offer(absURL);
			}
			else if (relURL.contains("nytimes")) {
				queue.offer(relURL);
			}
		}
	}
	//Temproray method to test description retrieving
	public void doCrawl()throws IOException{
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);

		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;

		for(int i = 0; i < 10; i ++){
			res = wc.crawl(true);
		}
	}

	public static void main(String[] args) throws IOException {

		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		//Anwaar- Changing the Source to specified article.
		//String source = "http://www.nytimes.com/2016/08/03/technology/instagram-stories-snapchat-facebook.html?ref=technology";
		String source = "https://en.wikipedia.org/wiki/Association_football";
		WikiCrawler wc = new WikiCrawler(source, index);

		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		
		for(int i = 0; i < 1000; i ++){
			res = wc.crawl(false);
		}
	
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
		//Anwaar - Use this to Free the index
		//index.deleteAllKeys();
	}
}
