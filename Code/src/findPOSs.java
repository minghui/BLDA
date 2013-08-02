import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import common.FileUtil;
import common.JC;

public class findPOSs {

	@SuppressWarnings("deprecation")
	public static void main(String args[]) throws IOException {

		String [] descrp = {"Filelist ", "target user list"};
		String [] directory = {"/data/targetUsers/3kplusUsers.txt", "/data/targetUsers/targetUsers.txt"};
		char [] options = {'f','t'};
		new JC();
		JC.setInputOptions(descrp, directory, options, args, "00", 0);
		String filelist = JC.getARG(0);
		String file2 = JC.getARG(1);
		JC.close();
		
		ArrayList<String> lines = new ArrayList<String> ();
		ArrayList<String> lines2 = new ArrayList<String> ();
//		ArrayList<String> notFounds = new ArrayList<String> ();
		
		FileUtil.readLines(filelist, lines2);
		FileUtil.readLines(file2, lines);
		
		for(int i = 0; i < lines.size(); i++) {
			int pos = lines2.indexOf(lines.get(i)) + 1;
//			if(pos > 0)
			System.out.println(lines.get(i) + "\t" + pos);
//			else
//				notFounds.add(lines.get(i) + "\t" + pos);
		}
		System.out.println("done");		
	}
}

