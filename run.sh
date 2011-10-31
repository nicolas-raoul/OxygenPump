groovy -classpath lib/nekohtml.jar:lib/xercesImpl.jar oxygenpump.groovy
cp license-of-the-wikicode.txt wikicode/
zip -r wikicode.zip wikicode
echo "Please upload wikicode.zip to http://code.google.com/p/oxygenpump/downloads (ask Nicolas Raoul to become a project committer)"
