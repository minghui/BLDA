package DataPro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.FileUtil;
import common.JC;

public class addPB {

	static ArrayList<String> stopWords;

	@SuppressWarnings("deprecation")
	public static void main(String args[]) throws IOException {
		new JC();
		String[] descrp = { "File direction(/) ", "OuptDir(/) " };
		String[] directory = { "\\data\\rawData\\output\\",
				"\\data\\rawData\\outputPB\\" };
		char[] options = { 'i', 'o' };
		JC.setInputOptions(descrp, directory, options, args, "00", 0);
		String dataDir = JC.getARG(0);
		String outputDir = JC.getARG(1);
		JC.close();

		ArrayList<String> inputlines = new ArrayList<String>();
		ArrayList<String> outputlines = new ArrayList<String>();
		String[] files = FileUtil.listFiles(dataDir);

		int no = 0;
		for(int i = 0; i < files.length; i++) {
//		for (int i = 0; i < 1; i++) {
			FileUtil.readLines(dataDir + files[i], inputlines);

			for (String inputline : inputlines) {
				if (inputline.length() < 22)
					System.out.println("Data length is too short:" + inputline);
				else
					outputlines.add(dataProcess(inputline));
			}
			FileUtil.writeLines(outputDir + files[i], outputlines);
			System.out.println(files[i] + "\t" + inputlines.size());
			inputlines.clear();
			outputlines.clear();
		}
		System.out.println("done");
	}

	private static String dataProcess(String inputline) {
		// System.out.println(inputline);
		String res = inputline.substring(20).toLowerCase().trim();
		if (res.matches(".*@\\w.*")) {
			if (res.matches(".* +rt +@\\w.*") || res.matches("rt +@\\w.*"))
				return inputline + "\t#PB#RT";
			if (res.startsWith("@"))
				return inputline + "\t#PB#RE";
		} else
			return inputline + "\t#PB#PO";
		return inputline + "\t#PB#ME";
	}

	private static void mvData(String filelist, String dataDir, String outputDir) {
		String outRaw = dataDir + "/tweets/";
		String outRTF = dataDir + "/tweets-fees";
		ArrayList<String> files = new ArrayList<String>();
		ArrayList<String> inputlines = new ArrayList<String>();
		FileUtil.readLines(filelist, files);
		for (int i = 0; i < files.size(); i++) {
			FileUtil.readLines(outRaw + "/" + files.get(i), inputlines);
			FileUtil.writeLines(outputDir + "/" + files.get(i), inputlines);
			inputlines.clear();
			System.out.println(files.get(i));
			FileUtil.readLines(outRTF + "/" + files.get(i), inputlines);
			FileUtil.writeLines(outputDir + "/" + files.get(i) + "@Fees",
					inputlines);
			inputlines.clear();
			System.out.println(files.get(i) + "@Fees");
		}
		System.exit(0);
	}
}
