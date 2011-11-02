/**
 * Download all articles from Wikitravel in wikicode format.
 * 
 * Be responsible!
 * Overloading Wikitravel's servers is in the interest of nobody.
 * Don't run this script if a reasonably fresh dump is already available.
 * If you run it, make the dump public and be sure to give the link here:
 * http://wikitravel.org/en/Wikitravel_talk:How_to_re-use_Wikitravel_guides
 * The script takes about 14 days to download all articles.
 * 
 * Usage: groovy -classpath lib/nekohtml.jar:lib/xercesImpl.jar oxygenpump.groovy
 * Data is stored in the "wikicode" directory.
 *
 * Whatever you do with the data, conform to the data's license. See:
 * http://wikitravel.org/shared/How_to_re-use_Wikitravel_guides
 *
 * License:
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Author: Nicolas Raoul http://nicolas-raoul.blogspot.com
 */

CLEAN = false
INITIAL_INDEX_URL = "http://wikitravel.org/en/Special:Allpages/%25s" // First page listed at http://wikitravel.org/en/Special:Allpages
//INITIAL_INDEX_URL = "unittests/index1.html"
NEXT_INDEX_PAGE_ANCHOR_LABEL = 'Next page'
URL_BASE = 'http://wikitravel.org'

/**
 * Cleaning
 */
def wikicodeDir = new File('wikicode')
if (CLEAN) {
	wikicodeDir.deleteDir()
	wikicodeDir.mkdir()
}
articlesList = new File("articles-list.txt")
if (CLEAN) {
	articlesList.write('')
}
/**
 * An HTML parser for web pages, that can handle non-valid XML.
 */
def nekoParser = new org.cyberneko.html.parsers.SAXParser()
nekoParser.setFeature('http://xml.org/sax/features/namespaces', false)
parser = new XmlParser(nekoParser)

/**
 * Output.
 */
debugToFile = false // If false, logs to standard output.
if (debugToFile) {
        debugFile = new File('debug.log')
        debugFile.write('')
}


/**
 * List all articles.
 */
if(CLEAN) {
	def cursorUrl = INITIAL_INDEX_URL
	while (true) {
		// Load current index page
		page = parse(cursorUrl)
		tableElements = page.depthFirst().TABLE.findAll{ it }

		// Save each article link
		tableElement = tableElements[2] // Main part, contains all articles links.
		aElements = tableElement.depthFirst().A.findAll{ it }
		aElements.each {
			href = it.attribute('href')
			article = href.substring(4)
			articlesList.append(article + "\n")
		}

		// Find next index page
		tableElement = tableElements[1] // Navigation part, contains a link to the next page.
		aElements = tableElement.depthFirst().A.findAll{ it }
		nextPageUri = null
		aElements.each {
			if(it.value()[0].contains(NEXT_INDEX_PAGE_ANCHOR_LABEL)) {
				nextPageUri = it.attribute('href')
			}
		}

		if(nextPageUri == null) {
			debug('No next page, it was the last index page.')
			break
		}

		def nextPageUrl = URL_BASE + nextPageUri
		cursorUrl = nextPageUrl
		pause()
	}
}

/**
 * Get the wikicode for every article.
 */
articlesList.eachLine { article ->
	if(
	! article.contains(":") // Ignore meta-level articles, for instance Wikitravel:Bad_jokes_and_other_deleted_nonsense/MY_HOUSE
	//&& ! article.startsWith("/") // Ignore the single file that starts with a slash
	) {
		try {
			getWikicode(article)
		}
		catch(exception) {
			debug ('!!! EXCEPTION !!! article:' + article + " exception:" + exception)
		}
		pause()
	}
}

/**
 * Get wikicode of an article
 */
def getWikicode(article) {
	debug("Downloading article " + article)
	node = new XmlSlurper().parse("http://wikitravel.org/wiki/en/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=" + article)
	def wikicode = node.query.pages.page.revisions.rev.text()
	article = article.replaceAll("/", "%2F") // Replace slashes with percent encoding, so that it does not mess with the filesystem.
	new File("wikicode/" + article + ".wikicode").write(wikicode)
}

/**
 * Wait 30 seconds to reduce load on server. See http://wikitravel.org/shared/How_to_re-use_Wikitravel_guides
 */
def pause() {
	Thread.sleep(30000)
}

/**
 * Parse an URL with the Neko parser
 * @param url URL of the page to parse
 * @return parsed page, exploitable with GPath
 */
def parse(url) {
        def page
        while (page == null) {
                try {
                        debug ('Downloading index starting from ' + url)
                        page = parser.parse(url);
                }
                catch (Exception e) {
                        debug('Exception ' + e + ' while loading ' + url + ' , retrying in 5 seconds')
                        Thread.sleep(5000)
                }
        }
        return page
}

/**
 * Print a debugging message.
 * @message message to be written as debug. (String)
 */
def debug(message) {
        if (debugToFile) {
                debugFile.append(message + '\n')
        } else {
                println message
        }
}
