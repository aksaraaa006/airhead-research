package edu.ucla.sspace.tools;

import edu.ucla.sspace.text.DocumentPreprocessor;

import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * @author Keith Stevens
 */
public class ReutersCleaner {

    private DocumentBuilder db;

    private DocumentPreprocessor processor;

    public ReutersCleaner() throws Exception {
        processor = new DocumentPreprocessor();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        db = dbf.newDocumentBuilder();
    }

    public String parseFile(String inFile) throws Exception {
        StringBuilder textBuilder = new StringBuilder();
        Document doc = db.parse(inFile);
        NodeList articles = doc.getElementsByTagName("REUTERS");
        for (int i = 0; i < articles.getLength();++i) {
            Element article = (Element) articles.item(i);
            StringBuilder catBuilder = new StringBuilder();
            String[] cats = new String[] {
                "TOPICS", "PLACES", "PEOPLE", "ORGS", "EXCHANGES", "COMPANIES"};
            for (String category : cats) {
                catBuilder.append(extractCategories(article, category));
                catBuilder.append(";");
            }
            NodeList body = article.getElementsByTagName("BODY");
            System.out.println(catBuilder.toString());
            String text = body.item(0).getTextContent().replaceAll("\n", " ");
            textBuilder.append(processor.process(text)).append("\n");
        }
        return textBuilder.toString();
    }

    public String extractCategories(Element article, String category) {
        NodeList cat = article.getElementsByTagName(category);
        if (cat.getLength() == 0)
            return "";
        NodeList items = ((Element) cat.item(0)).getElementsByTagName("D");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.getLength(); ++i)
            builder.append(items.item(i).getTextContent()).append(",");
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        ReutersCleaner cleaner = new ReutersCleaner();

        PrintWriter writer = new PrintWriter(args[0]);
        for (int i = 1; i < args.length; ++i)
            writer.printf(cleaner.parseFile(args[i]));
        writer.close();
    }
}
