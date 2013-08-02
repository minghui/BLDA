import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import common.FileUtil;
import common.JC;

public class Test {

	public static void main(String args[]) throws IOException {
		// create toy data
		short U = 8; // user numbers
		short W = 1000; // word number;
		short B = 4; // behaviour number
		short T = 8; // topic number
		new JC();

		float selfP = 0.5f;
		float selfW = 0.9f;
		float selfB = 0.9f;

		short UT[][] = new short[U][2];
		short TW[][] = new short[T][2];
		short[] TB = new short[T];

		for (int u = 0; u < U; u++) {
			UT[u][0] = (short) u;
			UT[u][1] = (short) Math.floor(Math.random() * T);
			System.out.println("user" + u + "\t" + UT[u][0] + "\t" + UT[u][1]);
		}
		for (int t = 0; t < T; t++) {
			TW[t][0] = (short) (2 * t);
			TW[t][1] = (short) (2 * t + 1);
			System.out.println("topic" + t + "\tW" + TW[t][0] + "\tW"
					+ TW[t][1]);
		}
		for (int t = 0; t < T; t++) {
			TB[t] = (short) (t % 4);
			System.out.println("topic" + t + "\t#PB#" + TB[t]);
		}

		ArrayList<String> datalines = new ArrayList<String>();
		FileUtil.mkdir(new File(JC.getCD() + "/data/toyData/"));

		for (int u = 0; u < U; u++) {
			for (int num = 0; num < 20; num++) {
				String line = "";
				int topic = 0;
				if (Math.random() > selfP) {
					if (Math.random() > 0.5) {
						topic = UT[u][0];
					} else
						topic = UT[u][1];
				} else {
					topic = (int) Math.floor(Math.random() * T);
				}

				for (int l = 0; l < 20; l++) {
					int word = 0;
					// whether select words from TW
					if (Math.random() < selfW) {
						if (Math.random() > 0.5) {
							word = TW[topic][0];
						} else
							word = TW[topic][1];
					} else {
						word = (int) Math.floor(Math.random() * W);
					}
					line += "W" + word + " ";
				}

				int behaviour = 0;
				if (Math.random() < selfB) {
					behaviour = TB[topic];
				} else {
					behaviour = (int) Math.floor(Math.random() * B);
				}
				line += "#PB#" + behaviour + " ";

				datalines.add(line);
			}
			FileUtil.writeLines(JC.getCD() + "/data/toyData/" + u + ".txt",
					datalines);
			datalines.clear();
		}
	}

}
