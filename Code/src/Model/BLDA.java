package Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import common.FileUtil;
import common.JC;
import common.Stopwords;

public class BLDA {

	// public static ArrayList<String> stopWords;

	// public static HashSet<Integer> stopList;

	public static void main(String args[]) throws IOException {
		String dir = System.getProperty("user.dir");
		String filelist = dir + "/data/test/filelist-test.txt";
		String dataDir = dir + "/data/test/TMinput/";
		String tempOutDir = dir + "/data/test/TMoutput/";
		String modelParas = dir + "/data/test/modelParameters.txt";

		ArrayList<String> modelSettings = new ArrayList<String>();
		getModelPara(modelParas, modelSettings);
		int T = Integer.parseInt(modelSettings.get(0));
		float alpha = Float.parseFloat(modelSettings.get(1));
		float beta = Float.parseFloat(modelSettings.get(2));
		float betaB = Float.parseFloat(modelSettings.get(3));
		float gamma = Float.parseFloat(modelSettings.get(4));
		int iteration = Integer.parseInt(modelSettings.get(5));
		// the results are taken from the saved results
		int saveStep = Integer.parseInt(modelSettings.get(6));
		int saveTimes = Integer.parseInt(modelSettings.get(7));
		System.err.println("Topics:" + T + ", alpha(eta):" + alpha + ", beta:"
				+ beta + ", betaB:" + betaB + ", gamma:" + gamma
				+ ", iteration:" + iteration + ", saveStep:" + saveStep
				+ ", saveTimes:" + saveTimes);
		modelSettings.clear();

		// stopList = new HashSet<Integer> ();

		ArrayList<String> files = new ArrayList<String>();
		FileUtil.readLines(filelist, files);

		HashMap<String, Integer> wordMap = new HashMap<String, Integer>();
		HashMap<String, Integer> itemMap = new HashMap<String, Integer>();
		ArrayList<Document> docs = new ArrayList<Document>();
		ArrayList<String> uniWordMap = new ArrayList<String>();
		ArrayList<String> uniItemMap = new ArrayList<String>();
		ArrayList<Integer> uniItemMapCounts = new ArrayList<Integer>();
		ArrayList<Integer> uniWordMapCounts = new ArrayList<Integer>();

		// 1. read docs
		new Stopwords();
		for (int i = 0; i < files.size(); i++) {
			Document doc = new Document(dataDir + files.get(i), files.get(i),
					wordMap, itemMap, uniWordMap, uniItemMap, uniWordMapCounts,
					uniItemMapCounts);
			docs.add(doc);
		}

		// ComUtil.printHash(wordMap);
		if (uniWordMap.size() != wordMap.size()) {
			System.out.println(wordMap.size());
			System.out.println(uniWordMap.size());
			System.err
					.println("uniqword size is not the same as the hashmap size!");
			System.exit(0);
		}

		// output wordMap and itemMap
		FileUtil.writeLines(tempOutDir + "wordMap.txt", wordMap);
		FileUtil.writeLines(tempOutDir + "itemMap.txt", itemMap);
		FileUtil.writeLines(tempOutDir + "uniWordMap.txt", uniWordMap);
		FileUtil.writeLines(tempOutDir + "uniItemMap.txt", uniItemMap);
		FileUtil.writeLinesSorted(tempOutDir + "uniWordMapCounts.txt",
				uniWordMap, uniWordMapCounts, 0);
		FileUtil.writeLinesSorted(tempOutDir + "uniItemMapCounts.txt",
				uniItemMap, uniItemMapCounts, 0);
		int wordMapSize = wordMap.size();
		int itemMapSize = itemMap.size();
		wordMap.clear();
		itemMap.clear();
		uniWordMap.clear();
		uniItemMap.clear();
		uniWordMapCounts.clear();
		uniItemMapCounts.clear();

		// 2. grid search for best parameter settings
		ArrayList<Float> etas = new ArrayList<Float>();
		setGammas(etas);

		for (int setting = 0; setting < etas.size(); setting++) {
			alpha = etas.get(setting);
			String outputDir = tempOutDir + "Eta_" + alpha + "/";
			FileUtil.mkdir(new File(outputDir));
			FileUtil.mkdir(new File(outputDir + "/TaggedDocs/"));
			FileUtil.mkdir(new File(outputDir + "/modelRes/"));

			Model model = new Model(T, alpha, beta, betaB, gamma);
			model.init(docs, wordMapSize, itemMapSize);
			model.inference(iteration, docs, saveStep, saveTimes, outputDir
					+ "/modelRes/");
			model.getResFromLastIteration(docs);
			model.computeModelParameter();
			// try {
			// System.out.println("Saving model-final");
			// model.saveModelRes(outputDir + "/modelRes/model-final");
			// } catch (Exception e1) {
			// e1.printStackTrace();
			// }

			// 3. output model results
			// get uniWordMap and uniItemMap
			FileUtil.readLines(tempOutDir + "uniWordMap.txt", uniWordMap);
			FileUtil.readLines(tempOutDir + "uniItemMap.txt", uniItemMap);
			System.out.println("saving the model...");
			try {
				model.saveModel(outputDir, uniWordMap, uniItemMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Ouputting tagged Docs...");
			model.outTaggedDoc(docs, uniWordMap, uniItemMap, outputDir
					+ "/TaggedDocs/");
			/*
			 * // //FileUtil.printHash(doc.getWordMap());
			 */
		}

		docs.clear();
		System.out.println("done");
	}

	private static void setGammas(ArrayList<Float> gammas) {
		gammas.add(0.1f);
		gammas.add(0.01f);
	}

	public static void printDocs(ArrayList<Document> docs) {
		for (int i = 0; i < docs.size(); i++) {
			for (int j = 0; j < docs.get(i).getDocWords().length; j++) {
				for (int m = 0; m < docs.get(i).getDocWords()[j].length; m++) {
					System.out.print(docs.get(i).getDocWords()[j][m] + "\t");
				}
				for (int m = 0; m < docs.get(i).getDocItems()[j].length; m++) {
					System.out.print("I" + docs.get(i).getDocItems()[j][m]
							+ "\t");
				}
				System.out.println();
			}
		}
	}

	private static void getModelPara(String modelParas,
			ArrayList<String> modelSettings) {
		modelSettings.clear();
		// T , alpha , beta , gamma , iteration , saveStep, saveTimes
		modelSettings.clear();
		modelSettings.add("20");
		modelSettings.add("0.001");
		modelSettings.add("0.01");
		modelSettings.add("0.01");
		modelSettings.add("20");
		modelSettings.add("20");
		modelSettings.add("2");
		modelSettings.add("10");
		ArrayList<String> inputlines = new ArrayList<String>();
		FileUtil.readLines(modelParas, inputlines);
		for (int i = 0; i < inputlines.size(); i++) {
			int index = inputlines.get(i).indexOf(":");
			String para = inputlines.get(i).substring(0, index).trim()
					.toLowerCase();
			String value = inputlines.get(i)
					.substring(index + 1, inputlines.get(i).length()).trim()
					.toLowerCase();
			switch (ModelParas.valueOf(para)) {
			case topics:
				modelSettings.set(0, value);
				break;
			case alpha:
				modelSettings.set(1, value);
				break;
			case beta:
				modelSettings.set(2, value);
				break;
			case betab:
				modelSettings.set(3, value);
				break;
			case gamma:
				modelSettings.set(4, value);
				break;
			case iteration:
				modelSettings.set(5, value);
				break;
			case savestep:
				modelSettings.set(6, value);
				break;
			case savetimes:
				modelSettings.set(7, value);
				break;
			default:
				break;
			}
		}
	}

	public enum ModelParas {
		topics, alpha, beta, gamma, iteration, savestep, savetimes, gamma2, betab;
	}

	private static void test() throws Exception {
		new JC();
		System.out.println(JC.getCD());
		String dir = JC.getCD() + "\\data\\toyOutput\\modelRes\\";
		String[] files = FileUtil.listFiles(dir);
		// one reader for each file
		ArrayList<BufferedReader> readers = new ArrayList<BufferedReader>();

		for (String file : files) {
			// System.out.println();
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					dir + file)));
			readers.add(reader);
			System.out.println(file);
		}

		int count = 1;
		String line = null;
		while ((line = readers.get(0).readLine()) != null) {
			System.out.println(line);
			if (count++ > 4)
				break;
			// lines.add(line);
		}
		for (BufferedReader reader : readers)
			reader.close();
		System.exit(0);
	}
}
