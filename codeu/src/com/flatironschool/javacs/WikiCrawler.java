package searchenginecli;

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
        
        public static Map<String, String> descriptionMap = new HashMap<>();
        //private String description = "";
        private Set<String> urlSet = new HashSet<>();
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

		if (testing==false && index.isIndexed(url)) {
			System.out.println("Already indexed.");
			return null;
		}
		
		Elements paragraphs;
		if (testing) {
			paragraphs = wf.readWikipedia(url);
		} else {
			paragraphs = wf.fetchWikipedia(url);
		}
		index.indexPage(url, paragraphs);
		queueInternalLinks(paragraphs);		
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
            //This is for the description
            if(!urlSet.contains(visitedUrl)){
                String description = "";
                Iterable<Node> iter = new WikiNodeIterable(paragraph);
                int i = 0;
                int j = 0;
                //Gets text nodes from paragraph
                for(Node node: iter){
                    
                    if(node instanceof TextNode){
                        description += node;
                    }
                    
                    i++;
                    
                    if(i == 50){
                        break;
                    }
                }
                //This makes the text look more uniform
                Scanner scanner = new Scanner(description);
                description = "";
                while(scanner.hasNext()){
                    if(j < 15 ){
                        description = description + " " + scanner.next();
                    }else{
                        description = description + "\n" + scanner.next();
                        j =0;
                    }
                    j++;
                }
                urlSet.add(visitedUrl);
                descriptionMap.put(visitedUrl, description);
            }
            
            Elements elts = paragraph.select("a[href]");
            for (Element elt: elts) {
                String relURL = elt.attr("href");

                if (relURL.startsWith("/wiki/")) {
                    String absURL = "https://en.wikipedia.org" + relURL;
                    //System.out.println(absURL);
                    queue.offer(absURL);
                }
            }
	}
	//Temproray method to test description retrieving
        public void doCrawl()throws IOException{
            Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		//String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
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
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
                for(int i = 0; i < 10; i ++){
                    res = wc.crawl(false);
                }
	}
}
