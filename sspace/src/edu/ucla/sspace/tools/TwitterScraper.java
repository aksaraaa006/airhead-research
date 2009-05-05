package edu.ucla.sspace.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;

import java.net.URL;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TwitterScraper extends TimerTask {
  private BufferedWriter twitterWriter;
  private int twitCount;
  private String twitDir;

  private static final int MAX_TWITS = 1440;
  private static final String TWITTER_URL =
    "http://twitter.com/statuses/public_timeline.xml";
  private static final int TIME_DELAY = 60000;

  private TwitterScraper(String fileDir) {
    twitDir = fileDir;
    startNewFile();
  }

  public void startNewFile() {
    try {
      String currentDate =
        new Date().toString().replace(" ","_").replace(":","-");
      String filename = twitDir + "/twitter_harvest_" + currentDate + ".xml";
      twitterWriter = new BufferedWriter(new FileWriter(filename));
      twitterWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      twitterWriter.write("<statuses type=\"array\">\n");
      twitCount = 0;
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  public void closeFile() {
    try {
      twitterWriter.write("</statuses>");
      twitterWriter.close();
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  public void run() {
    try {
      URL url = new URL(TWITTER_URL);
      BufferedReader br =
        new BufferedReader(new InputStreamReader(url.openStream()));
      String line = null;
      while ((line = br.readLine()) != null) {
        if (!line.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") &&
            !line.equals("<statuses type=\"array\">") &&
            !line.equals("    </statuses>")) {
          twitterWriter.write(line);
          twitterWriter.newLine();
        }
      }
      br.close();
      twitCount++;
      if (twitCount == MAX_TWITS) {
        closeFile();
        startNewFile();
      }
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("usage: java TwitterScraper <output-dir>");
      throw new IllegalArgumentException("output directory required");
    }
    Timer twitterTimer = new Timer();
    TwitterScraper scrapper = new TwitterScraper(args[0]);
    twitterTimer.schedule(scrapper, 0, TIME_DELAY);
  }
}
