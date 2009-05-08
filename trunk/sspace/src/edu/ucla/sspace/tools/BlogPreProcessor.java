package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.DocumentPreprocessor;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;

import java.text.DateFormat;
import java.text.ParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import java.sql.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BlogPreProcessor {
  public static ArgOptions setupOptions() {
    ArgOptions opts = new ArgOptions();
    opts.addOption('d', "blogdir", "location of directory containing only blog files", 
                      true, "STRING", "Required");
    return opts;
  }

  public static void main(String[] args)
      throws SAXException, ParserConfigurationException, IOException, ParseException {
    ArgOptions options = setupOptions();
    options.parseOptions(args);

    if (!options.hasOption("blogdir") || options.numPositionalArgs() != 1) {
      System.out.println("usage: java BlogPreProcessor [options] <out_file> \n" +
                         options.prettyPrint());
      System.exit(1);
    }
    PrintWriter pw = new PrintWriter(new File(options.getPositionalArg(0)));
    DocumentPreprocessor processor = new DocumentPreprocessor(new String[0]);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    DateFormat df = DateFormat.getInstance();

    File blogDir = new File(options.getStringOption("blogdir"));
    for (String blogFileName : blogDir.list()) {
      File blog = new File(blogDir, blogFileName);
      Document doc = db.parse(blog);
      NodeList entries = doc.getElementsByTagName("entry");
      for (int i = 0; i < entries.getLength(); ++i) {
        Element entry = (Element) entries.item(i);
        Node contentNode = entry.getElementsByTagName("content").item(0);
        Node dateNode = entry.getElementsByTagName("updated").item(0);
        String date = dateNode.getChildNodes().item(0).getNodeValue();
        String content = contentNode.getChildNodes().item(0).getNodeValue();
        long dateTime = Date.valueOf(date.split(" ")[0]).getTime();
        String cleanedContent = processor.process(content);
        if (!cleanedContent.equals(""))
          pw.println(dateTime + " " + cleanedContent);
      }
    }
    pw.close();
  }
}


