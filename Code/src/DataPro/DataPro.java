package DataPro;
import java.io.IOException;
import java.util.ArrayList;
import common.FileUtil;
import common.JC;

public class DataPro {
	
	public static void main(String args[]) throws IOException {
		new JC();
		String [] descrp = {"File direction(/) ", "OuptDir(/) "};
		String [] directory = {"\\data\\rawData\\input\\", "\\data\\rawData\\output\\"};
		char [] options = {'i', 'o'};
		JC.setInputOptions(descrp, directory, options, args, "00", 1);
		String dataDir = JC.getARG(0);
		String outputDir = JC.getARG(1);
		JC.close();
		
//		FileUtil.test();
		
		ArrayList<String> inputlines = new ArrayList<String>();
		ArrayList<String> outputlines = new ArrayList<String>();
		
		String[] files = FileUtil.listFiles(dataDir);
		
		for(int i = 0; i < files.length; i++) {
//		for(int i = 0; i < 4; i++) {
			FileUtil.readLines(dataDir + files[i], inputlines);
			
			merge2lines(inputlines, outputlines);

			FileUtil.writeLines(outputDir + files[i], outputlines);
			if(outputlines.size()*2 != inputlines.size())
				System.out.println("Error in " + files[i] + "\t" + inputlines.size() + 
					"\t" + outputlines.size());
			else
				System.out.println("\t" + files[i] + "\t" + inputlines.size() + 
						"\t" + outputlines.size());
			inputlines.clear();
			outputlines.clear();
		}
		System.out.println("done");
	}

	private static void merge2lines(ArrayList<String> inputlines, ArrayList<String> outputlines) {
		String output = null;
		for(int i = 0; i < inputlines.size(); i++) {
			String tmp = inputlines.get(i);
			// 2011-11-01 11:06 : 55 
			// to: 2011-09-01 14:53:45: **
			if(tmp.matches(".*\\d\\d\\d\\d[\\d -:]+")) {
				if(i + 1 < inputlines.size() && inputlines.get(i + 1).matches(".*\\w.*")) {
					tmp = tmp.replaceAll(" +: +", ":").trim();
					output = tmp + ": " + inputlines.get(i + 1);
				}
				outputlines.add(output);
				// skip next line
				i++;
			}
		}
	}

}

