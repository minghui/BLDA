package DataPro;

import java.util.ArrayList;
import java.util.HashMap;
import common.FileUtil;

public class wordFilter {

	public static String base = "C:/Users/minghui.qiu.2010/Desktop/music4k/";

	@SuppressWarnings("deprecation")
	public static void main(String args[]) {

//		String[] descrp = { "Filelist ", "File direction(/) ", "ID-name Map",
//				"OuptDir1(/) " };
//		String[] directory = { base + "filelist.txt",
//				base + "output_b2\\TextWithLabel.txt", base + "100users\\",
//				base + "100usersRes\\" };
//		char[] options = { 'f', 'i', 'm', 'o' };
//		new JC();
//		JC.setInputOptions(descrp, directory, options, args, "1111", 0);
//		String filelist = JC.getARG(0);
//		String inputFile = JC.getARG(1);
//		String inputDir = JC.getARG(2);
//		String resDir = JC.getARG(3);
//		JC.close();
		
		String map = base + "/mu4kmap.txt";
		String words = base + "/3k-users.txt";
		String output = base + "/3k-users-pos.txt";

		getWordPos(map, words, output);
		
		System.out.println("done");
	}

	private static void getWordPos(String map, String words, String output) {
		ArrayList<String> inputwords = new ArrayList<String>();
		HashMap<String, String> wordmap = new HashMap<String, String> ();
		ArrayList<String> outputs = new ArrayList<String>();
		
		FileUtil.readLines(words, inputwords); // each line is a word
		FileUtil.readHash(map, wordmap, true); // each line is a map
		System.out.println("input word size: " + inputwords.size());
		System.out.println("map size: " + wordmap.size());
		
		for(String word:inputwords) {
			if(wordmap.containsKey(word))
				outputs.add(word + "\t" + wordmap.get(word));
			else
				outputs.add(word + "\tnull");
		}
		
		FileUtil.writeLines(output, outputs);
	}
	
	
}
