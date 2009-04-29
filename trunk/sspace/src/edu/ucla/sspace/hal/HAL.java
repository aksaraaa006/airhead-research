package src.edu.ucla.sspace.hal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.LinkedHashSet;
import java.util.Set;


public class HAL
{

	private final Set<String> rows;
	private final Set<String> cols;
	
	public HAL()
	{
		rows = new LinkedHashSet<String>();
		cols = new LinkedHashSet<String>();		
	}
	
	public void  parse(String fileName) throws IOException
	{
		BufferedReader buff = new BufferedReader(new FileReader(fileName));
		String line = null;
		while((line = buff.readLine()) != null)
		{
			String[] text = line.split("//s");
			for(String word : text)
			{
				rows.add(word);
				cols.add(word);
			}
		}		
	}
	
}


