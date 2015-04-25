package Model;
/*
 * JC
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import common.ComUtil;
import common.FileUtil;
import common.MatrixUtil;

public class Model {
	// document related parameters
	// public Document Doc;
	public int T; // no of topics
	public int U; // no of users
	public int V; // vocabulary size
	public int M; // no of items

	// hyperparameters
	public float alpha;
	public float beta;
	public float betaB;
	public float gamma;
	public float eta;

	// model parameters
	public int niters; // number of Gibbs sampling iteration

	// Estimated/Inferenced parameters
	public float[][] theta; // user-topic distribution, U*T
	public float[] phi; // label distribution 0 or 1, 1*2
	public float[] vPhiB; // background word distribution, 1*V
	public float[][] vPhi; // topic-word distribution, T*V
	public float[][] psi; // topic-item distribution, size T x M

	// Temp variables while sampling
	// public boolean[][][][] sampleY; // s x U x N_u x N_w
	// public int[][][] sampleZ; // s x U x N
	public boolean y[][][]; // U x N_u x N_w
	public int Z[][]; // U x N
	public int NW[]; // 1 x V
	public int NTW[][]; // T x V, sum is: SNTW[T]
	public int NTI[][]; // T x M, sum is: SNTI[T]
	public long NY[]; // 1 x 2
	public int NUT[][]; // sum U x T, sum is: SNUT[U]
	public double SNTW[];
	public double SNTI[];
	public double SNUT[];

	public Model(int no, float alphaV, float betaV, float beta2, float gammaV) {
		T = no;
		alpha = alphaV;
		beta = betaV;
		betaB = beta2;
		gamma = gammaV;
		eta = (float) (50.0 / T);
	}

	public void getZY(String modelres) throws Exception {
		System.out.println("reading Z and Y");
		ArrayList<String> zdata = new ArrayList<String>();
		ArrayList<String> ydata = new ArrayList<String>();
		// sequential read
		boolean zun = false, yflag = false, ydataflag = false;
		BufferedReader reader = new BufferedReader(new FileReader(new File(modelres)));
		
		String line = null;
		while ((line = reader.readLine()) != null) {
			if(line.contains("Z[u][n]:")) {
				zun = true;
			}
			if(line.contains("user")) {
				zun = false;
				yflag = true;
			}
			if(zun == true && yflag == false && !line.contains("Z[u][n]:")) {
				// add zun data
				zdata.add(line);
			}
			// add y
			if(zun == false && yflag == true ) {
				// process zdata
				processZdata(zdata);
				ydataflag = true;
			}
			if(ydataflag)
				ydata.add(line);
		}
		System.out.println("processing y data");
		processydata(ydata);
		reader.close();
	}

	private void processydata(ArrayList<String> ydata) {
		// assign label y
		ArrayList<String> tokens = new ArrayList<String>();
		y = new boolean[Z.length][][];
		int base = 1;
		
		for (int i = 0; i < Z.length; i++) {
			y[i] = new boolean[Z[i].length][];			
			// tokenize data
			for (int j = 0; j < Z[i].length; j++) {
				FileUtil.tokenize(ydata.get(base + j), tokens);
				y[i][j] = new boolean[tokens.size()];
				for (int k = 0; k < tokens.size(); k++) {
					int v = Integer.parseInt(tokens.get(k));
					if(v == 1)
						y[i][j][k] = true;
					else
						y[i][j][k] = false;
				}
				tokens.clear();
			}
			// update base;
			base += Z[i].length + 1;
		}
	}

	private void processZdata(ArrayList<String> zdata) {
		ArrayList<String> tokens = new ArrayList<String>();
		Z = new int[zdata.size()][];
		for (int i = 0; i < zdata.size(); i++) {
			// get data line
			FileUtil.tokenize(zdata.get(i), tokens);
			
			Z[i] = new int[tokens.size()];
			for (int j = 0; j < Z[i].length; j++) {
				Z[i][j] = Integer.parseInt(tokens.get(j));
			}
			tokens.clear();
		}
	}

	/**
	 * initialize the model
	 * 
	 * @param vocabSize
	 * @param itemNo
	 */
	protected boolean init(ArrayList<Document> docs, int vocabSize, int itemNo) {
		U = docs.size();
		V = vocabSize;
		M = itemNo;

		// assign topics
		Z = new int[U][];
		for (int i = 0; i < U; i++) {
			Z[i] = new int[docs.get(i).getDocWords().length];
			for (int j = 0; j < Z[i].length; j++) {
				Z[i][j] = (int) Math.floor(Math.random() * T);
				if (Z[i][j] < 0)
					Z[i][j] = 0;
				if (Z[i][j] > T - 1)
					Z[i][j] = (int) (T - 1);
			}
		}

		// assign label y
		y = new boolean[U][][];
		for (int i = 0; i < U; i++) {
			y[i] = new boolean[docs.get(i).getDocWords().length][];
			for (int j = 0; j < docs.get(i).getDocWords().length; j++) {
				y[i][j] = new boolean[docs.get(i).getDocWords()[j].length];
				for (int k = 0; k < docs.get(i).getDocWords()[j].length; k++) {
					if (Math.random() > 0.5) {
						y[i][j][k] = true;
					} else {
						y[i][j][k] = false;
					}
				}
			}
		}
		cleanTempPrmts(docs);
		computeTempPrmts(docs, Z, y);
		computeSum(docs, T);
		return true;
	}

	// initialization using existing Z and Y
	protected boolean init2(ArrayList<Document> docs, int vocabSize, int itemNo) {
		U = docs.size();
		V = vocabSize;
		M = itemNo;

		cleanTempPrmts(docs);
		computeTempPrmts(docs);
		computeSum(docs, T);
		return true;
	}
	
	public void cleanTempPrmts(ArrayList<Document> docs) {
		// initial parameters NW[] NWT[][] NIT[][] NY[] NUT[][]
		NW = new int[V];
		vPhiB = new float[V];
		for (int i = 0; i < V; i++) {
			NW[i] = 0;
			vPhiB[i] = 0.0f;
		}
		NTW = new int[T][];
		vPhi = new float[T][];
		for (int t = 0; t < T; t++) {
			NTW[t] = new int[V];
			vPhi[t] = new float[V];
			for (int i = 0; i < V; i++) {
				NTW[t][i] = 0;
				vPhi[t][i] = 0.0f;
			}
		}

		NTI = new int[T][];
		psi = new float[T][];
		for (int t = 0; t < T; t++) {
			NTI[t] = new int[M];
			psi[t] = new float[M];
			for (int i = 0; i < M; i++) {
				NTI[t][i] = 0;
				psi[t][i] = 0.0f;
			}
		}

		NY = new long[2];
		phi = new float[2];
		NY[0] = 0;
		NY[1] = 0;
		phi[0] = 0.0f;
		phi[1] = 0.0f;

		NUT = new int[U][];
		theta = new float[U][T];
		for (int i = 0; i < U; i++) {
			NUT[i] = new int[T];
			theta[i] = new float[T];
			for (int t = 0; t < T; t++) {
				NUT[i][t] = 0;
				theta[i][t] = 0.0f;
			}
		}
	}

	public void inference(int iteration, ArrayList<Document> docs,
			int saveStep, int saveTimes, String outputDir) {
		if (iteration < saveStep * saveTimes) {
			System.err.println("iteration should be at least: " + saveStep
					* saveTimes);
			System.exit(0);
		}
		int no = 0;
		// sampleY = new boolean[saveTimes][][][];
		// sampleZ = new int[saveTimes][][];
		for (int i = 0; i < iteration; i++) {
			System.out.println("iteration " + i);

			if (iteration % 10 == 0) {
				if (!checkEqual(NUT, SNUT, "NUT")
						|| !checkEqual(NTW, SNTW, "NTW")
						|| !checkEqual(NTI, SNTI, "NTI")) {
					try {
						System.err.println("Error!!!");
						saveModelRes(outputDir + "/model-error" + (i + 1));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			for (int u = 0; u < U; u++) {
				// sample from p(z_{u,n}|c_-{u,n},w)
				for (int n = 0; n < docs.get(u).getDocWords().length; n++) {
					SampleTopic(docs.get(u).getDocWords()[n], docs.get(u)
							.getDocItems()[n], u, n);
					for (int l = 0; l < docs.get(u).getDocWords()[n].length; l++)
						SampleLabel(docs.get(u).getDocWords()[n][l], u, n, l);
				}
			}

			if (i >= (iteration - 1 - (saveStep * (saveTimes - 1))))
				if ((iteration - i - 1) % saveStep == 0) {
					System.out.println("Saveing the model at " + (i + 1)
							+ "-th iteration");
					// outputModelRes();
					try {
						saveModelRes(outputDir + "/model-" + (i + 1));
						// sampleY[no] = y;
						// sampleZ[no] = Z;
					} catch (Exception e) {
						e.printStackTrace();
					}
					no++;
				}
		}
	}

	public void inference(int iteration, ArrayList<Document> docs,
			int saveStep, int saveTimes, String outputDir, int startPos,
			ArrayList<Document> testDocs, ArrayList<Double> prep) {
		if (iteration < saveStep * saveTimes) {
			System.err.println("iteration should be at least: " + saveStep
					* saveTimes);
			System.exit(0);
		}
		int no = 0;
		for (int i = 0; i < iteration; i++) {
			System.out.println("iteration " + i);

			if (iteration % 10 == 0) {
				if (!checkEqual(NUT, SNUT, "NUT")
						|| !checkEqual(NTW, SNTW, "NTW")
						|| !checkEqual(NTI, SNTI, "NTI")) {
					try {
						System.err.println("Error!!!");
						saveModelRes(outputDir + "/model-error" + (i + 1));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// compute the model parameter and perplexity
				computeModelParameter();
			}

			for (int u = 0; u < U; u++) {
				// sample from p(z_{u,n}|c_-{u,n},w)
				for (int n = 0; n < docs.get(u).getDocWords().length; n++) {
					SampleTopic(docs.get(u).getDocWords()[n], docs.get(u)
							.getDocItems()[n], u, n);
					for (int l = 0; l < docs.get(u).getDocWords()[n].length; l++)
						SampleLabel(docs.get(u).getDocWords()[n][l], u, n, l);
				}
			}

			if (i >= (iteration - 1 - (saveStep * (saveTimes - 1))))
				if ((iteration - i - 1) % saveStep == 0) {
					System.out.println("Saveing the model at " + (i + 1)
							+ "-th iteration");
					// outputModelRes();
					try {
						saveModelRes(outputDir + "/model-" + (i + 1));
					} catch (Exception e) {
						e.printStackTrace();
					}
					no++;
				}
		}
	}

	private double computeSumD(double[] valueT, int[] flag, int minFlag) {
		double res = 0.0d;
		if(valueT.length != flag.length) {
			System.err.println("Inequal length! error!");
		} else {
//			double log10 = Math.log(10);
			for(int i = 0; i < valueT.length; i++) {
				res += valueT[i]/Math.pow(1E150, flag[i] - minFlag);
			}
		}
		return res;
	}

	public void computeModelParameter() {
		System.out.println("computing model parameters...");
		for (int w = 0; w < V; w++) {
			vPhiB[w] = (float) ((NW[w] + betaB) / (NY[0] + V * betaB));
		}
		for (int t = 0; t < T; t++) {
			for (int w = 0; w < V; w++)
				vPhi[t][w] = (float) ((NTW[t][w] + beta) / (SNTW[t] + V * beta));
		}
		for (int t = 0; t < T; t++) {
			for (int m = 0; m < M; m++) {
				psi[t][m] = (float) ((NTI[t][m] + alpha) / (SNTI[t] + M * alpha));
			}
		}
		for (int i = 0; i < 2; i++) {
			phi[i] = (float) ((NY[i] + gamma) / (NY[0] + NY[1] + 2 * gamma));
		}
		for (int u = 0; u < U; u++) {
			for (int t = 0; t < T; t++) {
				theta[u][t] = (float) ((NUT[u][t] + eta) / (SNUT[u] + T * eta));
			}
		}
		System.out.println("model parameters are computed");
	}

	private void computeSum(ArrayList<Document> docs, int T) {
		SNUT = new double[docs.size()];
		for (int i = 0; i < docs.size(); i++) {
			SNUT[i] = MatrixUtil.sumRow(NUT, i);
		}
		SNTW = new double[T];
		SNTI = new double[T];
		for (int t = 0; t < T; t++) {
			SNTW[t] = MatrixUtil.sumRow(NTW, t);
			SNTI[t] = MatrixUtil.sumRow(NTI, t);
		}
	}

	private void computeTempPrmts(ArrayList<Document> docs) {
		for (int i = 0; i < U; i++) {
			for (int j = 0; j < Z[i].length; j++) {
				NUT[i][Z[i][j]]++;
			}
			for (int j = 0; j < y[i].length; j++) {
				for (int k = 0; k < y[i][j].length; k++) {
					if (y[i][j][k]) {
						NTW[Z[i][j]][docs.get(i).getDocWords()[j][k]]++;
						NY[1]++;
					} else {
						NW[docs.get(i).getDocWords()[j][k]]++;
						NY[0]++;
					}
				}
			}
			for (int j = 0; j < docs.get(i).getDocItems().length; j++) {
				for (int k = 0; k < docs.get(i).getDocItems()[j].length; k++)
					NTI[Z[i][j]][docs.get(i).getDocItems()[j][k]]++;
			}
		}
	}

	private void computeTempPrmts(ArrayList<Document> docs, int[][] newZ,
			boolean[][][] newY) {
		for (int i = 0; i < U; i++) {
			for (int j = 0; j < newZ[i].length; j++) {
				NUT[i][newZ[i][j]]++;
			}
			for (int j = 0; j < newY[i].length; j++) {
				for (int k = 0; k < newY[i][j].length; k++) {
					if (newY[i][j][k]) {
						NTW[Z[i][j]][docs.get(i).getDocWords()[j][k]]++;
						NY[1]++;
					} else {
						NW[docs.get(i).getDocWords()[j][k]]++;
						NY[0]++;
					}
				}
			}
			for (int j = 0; j < docs.get(i).getDocItems().length; j++) {
				for (int k = 0; k < docs.get(i).getDocItems()[j].length; k++)
					NTI[Z[i][j]][docs.get(i).getDocItems()[j][k]]++;
			}
		}
	}

	// public void averageSamples(ArrayList<Document> docs) {
	// // get a new Y and z from sampleY
	// // int newY[][][] = new int[y.length][][];
	// boolean newY[][][] = y;
	// int newZ[][] = Z;
	// int size = sampleY.length;
	// int sample[] = new int[size];
	// for (int u = 0; u < Z.length; u++) {
	// for (int n = 0; n < Z[u].length; n++) {
	// for (int s = 0; s < size; s++) {
	// sample[s] = sampleZ[s][u][n];
	// }
	// newZ[u][n] = ComUtil.getFrequentElement(sample);
	// }
	// }
	// for (int u = 0; u < y.length; u++) {
	// // newY[u] = new int[y[u].length][];
	// for (int n = 0; n < y[u].length; n++) {
	// // newY[u][n] = new int[y[u][n].length];
	// for (int l = 0; l < y[u][n].length; l++) {
	// for (int s = 0; s < size; s++) {
	// if (sampleY[s][u][n][l])
	// sample[s] = 1;
	// else
	// sample[s] = 0;
	// }
	// newY[u][n][l] = ComUtil.getFrequentElementBinary(sample);
	// }
	// }
	// }
	// cleanTempPrmts(docs);
	// // update NW[V](y=0) NTW[T][W](y=1) NTI[T][M] NY[2] NUT[U][T]
	// Z = newZ;
	// y = newY;
	// computeTempPrmts(docs, Z, y);
	// }

	public void getResFromLastIteration(ArrayList<Document> docs) {
		System.out.println("getting results from last interation...");
		cleanTempPrmts(docs);
		computeTempPrmts(docs, Z, y);
	}

	private void outputErr(int word, int u, int n, int l) {
		// output Z
		System.out.println("word: " + word);
		System.out.println("Z[u][n]: " + u + " " + n + " " + l);
		System.out.println("toipc: " + Z[u][n]);
		if (y[u][n][l])
			System.out.println("y: 1");
		else
			System.out.println("y: 0");
		MatrixUtil.printArray(Z);
		for (int i = 0; i < U; i++) {
			System.out.println("user " + i + ": ");
			MatrixUtil.printArray(y[i]);
		}
		System.out.println("NW: ");
		MatrixUtil.printArray(NW);
		System.out.println("NTW: ");
		MatrixUtil.printArray(NTW);
		System.out.println("NTI: ");
		MatrixUtil.printArray(NTI);
		System.out.println("NY: ");
		MatrixUtil.printArray(NY);
		System.out.println("NUT: ");
		MatrixUtil.printArray(NUT);
	}

	private void outputErr(ArrayList<Integer> tempUniqueWords,
			ArrayList<Integer> tempCounts, int[] words, int[] items, int u,
			int n) {
		FileUtil.print(tempUniqueWords);
		FileUtil.print(tempCounts);
		System.out.println("words");
		MatrixUtil.printArray(words);
		System.out.println("items");
		MatrixUtil.printArray(items);
		// output Z
		System.out.println("Z[u][n]: " + u + " " + n + " topic: " + Z[u][n]);
		MatrixUtil.printArray(Z);
		for (int i = 0; i < U; i++) {
			System.out.println("user " + i + ": ");
			MatrixUtil.printArray(y[i]);
		}
		System.out.println("NW: ");
		MatrixUtil.printArray(NW);
		System.out.println("NTW: ");
		MatrixUtil.printArray(NTW);
		System.out.println("NTI: ");
		MatrixUtil.printArray(NTI);
		System.out.println("NY: ");
		MatrixUtil.printArray(NY);
		System.out.println("NUT: ");
		MatrixUtil.printArray(NUT);
		System.out.println("SNUT: ");
		MatrixUtil.printArray(SNUT);
	}

	private void outputErr(ArrayList<Document> docs) {
		//
		for (int i = 0; i < docs.size(); i++) {
			System.out.println("user " + i + ": ");
			for (int j = 0; j < docs.get(i).getDocWords().length; j++) {
				String tmpline = "Topic " + Z[i][j] + ": ";
				for (int k1 = 0; k1 < docs.get(i).getDocWords()[j].length; k1++) {
					if (y[i][j][k1])
						tmpline += (docs.get(i).getDocWords()[j][k1] + "")
								.concat("_1 ");
					else
						tmpline += (docs.get(i).getDocWords()[j][k1] + "")
								.concat("_0 ");
				}
				for (int k2 = 0; k2 < docs.get(i).getDocItems()[j].length; k2++) {
					tmpline += "I" + docs.get(i).getDocItems()[j][k2] + " ";
				}
				System.out.println(tmpline);
			}
		}
		for (int i = 0; i < U; i++) {
			System.out.println("user " + i + ": ");
			System.out.println("doc words");
			MatrixUtil.printArray(docs.get(i).getDocWords());
			System.out.println("doc items");
			MatrixUtil.printArray(docs.get(i).getDocItems());
		}
		// output Z
		System.out.println("Z[u][n]: ");
		MatrixUtil.printArray(Z);
		for (int i = 0; i < U; i++) {
			System.out.println("user " + i + ": ");
			MatrixUtil.printArray(y[i]);
		}
		System.out.println("NW: ");
		MatrixUtil.printArray(NW);
		System.out.println("NTW: ");
		MatrixUtil.printArray(NTW);
		System.out.println("NTI: ");
		MatrixUtil.printArray(NTI);
		System.out.println("NY: ");
		MatrixUtil.printArray(NY);
		System.out.println("NUT: ");
		MatrixUtil.printArray(NUT);
	}

	private boolean SampleTopic(int[] words, int[] items, int u, int n) {
		int topic = Z[u][n];
		// get words and their count in [u,n]
		ArrayList<Integer> tempUniqueWords = new ArrayList<Integer>();
		ArrayList<Integer> tempCounts = new ArrayList<Integer>();
		uniqe(words, y[u][n], tempUniqueWords, tempCounts);

		// assume the current topic assignment is hidden
		// update NTW[T][W](y=1) NTI[T][M] NUT[U][T] in {u,n}
		if (NUT[u][topic] == 0) {
			System.err.println("NUT " + NUT[u][topic]);
			outputErr(tempUniqueWords, tempCounts, words, items, u, n);
		}
		for (int w1 = 0; w1 < tempUniqueWords.size(); w1++) {
			if (NTW[topic][tempUniqueWords.get(w1)] < tempCounts.get(w1)) {
				System.err.println("NTW !! error!");
				outputErr(tempUniqueWords, tempCounts, words, items, u, n);
			}
		}
		for (int k = 0; k < items.length; k++) {
			if (NTI[topic][items[k]] == 0) {
				System.err.println("NTI =0 !! error!");
				outputErr(tempUniqueWords, tempCounts, words, items, u, n);
			}
		}
		NUT[u][topic]--;
		SNUT[u]--;
		for (int w1 = 0; w1 < tempUniqueWords.size(); w1++) {
			NTW[topic][tempUniqueWords.get(w1)] -= tempCounts.get(w1);
			SNTW[topic] -= tempCounts.get(w1);
		}
		for (int k = 0; k < items.length; k++) {
			NTI[topic][items[k]]--;
			SNTI[topic]--;
		}

		// get p(Z_{u,n} = z|Z_c, W, Y, I)
		double[] pt = new double[T];
		// double NUTsumRowU = MatrixUtil.sumRow(NUT, u);
		double NUTsumRowU = SNUT[u];
		// checkEqual(NUTsumRowU, MatrixUtil.sumRow(NUT, u), "NUT");
		for (int i = 0; i < T; i++) {
			int wcount = 0;
			double p1 = (double) (NUT[u][i] + eta) / (NUTsumRowU + T * eta);
			double p2 = 1.0D;
			for (int w = 0; w < tempUniqueWords.size(); w++) {
				int tempvalue = NTW[i][tempUniqueWords.get(w)];
				// double sumRow = MatrixUtil.sumRow(NTW, i);
				double sumRow = SNTW[i];
				// checkEqual(sumRow, MatrixUtil.sumRow(NTW, i), "NTW");
				for (int numC = 0; numC < tempCounts.get(w); numC++) {
					p2 = p2
							* ((double) (tempvalue + beta + numC) / ((double) sumRow
									+ V * beta + wcount));
					wcount++;
				}
			}
			// assume items only appear once
			double p3 = 1.0D;
			// double sumRow = MatrixUtil.sumRow(NTI, i);
			double sumRow = SNTI[i];
			// checkEqual(sumRow, MatrixUtil.sumRow(NTI, i), "NTI");
			for (int j = 0; j < items.length; j++) {
				p3 = p3
						* ((double) (NTI[i][items[j]] + alpha) / ((double) sumRow
								+ M * alpha + j));
			}
			pt[i] = p1 * p2 * p3;
		}

		// cummulate multinomial parameters
		int sample = ComUtil.sample(pt, T);
		assert (sample >= 0 && sample < T) : "sample value error:" + sample;

		Z[u][n] = sample;
		topic = sample;

		// update NTW[T][W](y=1) NTI[T][M] NUT[U][T] in {u,n}
		NUT[u][topic]++;
		SNUT[u]++;
		for (int w1 = 0; w1 < tempUniqueWords.size(); w1++) {
			NTW[topic][tempUniqueWords.get(w1)] += tempCounts.get(w1);
			SNTW[topic] += tempCounts.get(w1);
		}
		for (int k = 0; k < items.length; k++) {
			NTI[topic][items[k]]++;
			SNTI[topic]++;
		}

		tempUniqueWords.clear();
		tempCounts.clear();
		return true;
	}

	// SampleLabel(docs.get(u).getDocWords()[n][l], u, n, l);
	private void SampleLabel(int word, int u, int n, int l) {
		// remove current y label
		if (y[u][n][l]) {
			if (NY[1] == 0) {
				System.err.println("NY ");
				outputErr(word, u, n, l);
			}
			if (NTW[Z[u][n]][word] == 0) {
				System.err.println("NTW error ");
				outputErr(word, u, n, l);
			}
			NY[1]--;
			NTW[Z[u][n]][word]--;
			SNTW[Z[u][n]]--; // important!!
		} else {
			if (NY[0] == 0) {
				System.err.println("NY ");
				outputErr(word, u, n, l);
			}
			if (NW[word] == 0) {
				System.err.println("NW error ");
				outputErr(word, u, n, l);
			}
			NY[0]--;
			NW[word]--;
		}

		// p(0) and p(1)
		double pt[] = new double[2];

		double p0 = (double) (NY[0] + gamma) / (NY[0] + NY[1] + 2 * gamma);
		double p2 = 1.0d;
		p2 = (double) (NW[word] + betaB) / (NY[0] + V * betaB);
		double p3 = 1.0d;
		double sumRow = SNTW[Z[u][n]];
		p3 = (double) (NTW[Z[u][n]][word] + beta) / (sumRow + V * beta);

		pt[0] = p0 * p2;
		pt[1] = pt[0] + (1 - p0) * p3;

		// cummulate multinomial parameters
		int sample = ComUtil.sample(pt, 2);
		assert (sample >= 0 && sample < 2) : "sample value error:" + sample;

		if (sample == 1) {
			NY[1]++;
			NTW[Z[u][n]][word]++;
			SNTW[Z[u][n]]++;
			y[u][n][l] = true;
		} else {
			NY[0]++;
			NW[word]++;
			y[u][n][l] = false;
		}
	}

	private boolean checkEqual(int[][] a, double[] b, String string) {
		for (int i = 0; i < a.length; i++) {
			double c = MatrixUtil.sumRow(a, i);
			if (c != b[i]) {
				System.out.println(string + "\t" + c + "\t" + b[i]);
				return false;
			}
		}
		return true;
	}

	public static void uniqe(int[] words, boolean[] y2,
			ArrayList<Integer> tempUniqueWords, ArrayList<Integer> tempCounts) {
		for (int i = 0; i < words.length; i++) {
			if (y2[i]) {
				if (tempUniqueWords.contains(words[i])) {
					int index = tempUniqueWords.indexOf(words[i]);
					tempCounts.set(index, tempCounts.get(index) + 1);
				} else {
					tempUniqueWords.add(words[i]);
					tempCounts.add(1);
				}
			}
		}
	}

	/**
	 * output Model paramters
	 * */
	public void outputModelRes() {
		// output Z
		System.out.println("Z[u][n]: ");
		MatrixUtil.printArray(Z);
		for (int i = 0; i < U; i++) {
			System.out.println("user " + i + ": ");
			MatrixUtil.printArray(y[i]);
		}
	}

	public void outTaggedDoc(ArrayList<Document> docs,
			ArrayList<String> uniWordMap, ArrayList<String> uniItemMap,
			String outputDir) {
		ArrayList<String> datalines = new ArrayList<String>();
		for (int i = 0; i < docs.size(); i++) {
			for (int j = 0; j < docs.get(i).getDocWords().length; j++) {
				String tmpline = "Topic " + Z[i][j] + ": ";
				for (int k1 = 0; k1 < docs.get(i).getDocWords()[j].length; k1++) {
					if (y[i][j][k1])
						tmpline += uniWordMap.get(
								docs.get(i).getDocWords()[j][k1]).concat("_1 ");
					else
						tmpline += uniWordMap.get(
								docs.get(i).getDocWords()[j][k1]).concat("_0 ");
				}
				for (int k2 = 0; k2 < docs.get(i).getDocItems()[j].length; k2++) {
					tmpline += uniItemMap.get(docs.get(i).getDocItems()[j][k2])
							+ " ";
				}
				datalines.add(tmpline);
			}
			FileUtil.writeLines(outputDir + docs.get(i).getId(),
					datalines);
			datalines.clear();
		}
	}

	void saveModelRes(String string) throws Exception {
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(new File(string)));
		writer.write("Z[u][n]: \n");
		for (int i = 0; i < Z.length; i++) {
			for (int j = 0; j < Z[i].length; j++)
				writer.write(Z[i][j] + "\t");
			writer.write("\n");
		}
		for (int i = 0; i < y.length; i++) {
			writer.write("user " + i + ": \n");
			if(y[i] != null) {
				for (int j = 0; j < y[i].length; j++) {
					if(y[i][j] != null) {
						for (int k = 0; k < y[i][j].length; k++) {
							if (y[i][j][k])
								writer.write("1\t");
							else
								writer.write("0\t");
						}
					}
					writer.write("\n");
				}
			}
		}
		writer.flush();
		writer.close();
	}

	public boolean saveModel(String output) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				output + "model.vPhiB")));
		ModelComFunc.writeData(vPhiB, writer);
		writer.close();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.phi")));
		ModelComFunc.writeData(phi, writer);
		writer.close();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.theta")));
		ModelComFunc.writeData(theta, writer);
		writer.close();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model-topic-words.txt")));
		for (int t = 0; t < vPhi.length; t++) {
			ModelComFunc.writeData(vPhi[t], writer);
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.vPhi")));
		ModelComFunc.writeData(vPhi, writer);
		writer.close();
		
		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.psi")));
		ModelComFunc.writeData(psi, writer);
		writer.close();
		return true;
	}
	
	public boolean saveModel(String output, ArrayList<String> uniWordMap,
			ArrayList<String> uniItemMap) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				output + "model.vPhiB")));
		ArrayList<Integer> rankList = new ArrayList<Integer>();
		ComUtil.getTop(vPhiB, rankList, 100);
		ModelComFunc.writeData(vPhiB, uniWordMap, rankList, writer, "");
		writer.close();
		rankList.clear();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.phi")));
		ModelComFunc.writeData(phi, writer);
		writer.close();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.theta")));
		ModelComFunc.writeData(theta, writer);
		writer.close();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model-topic-words.txt")));
		for (int t = 0; t < vPhi.length; t++) {
			ComUtil.getTop(vPhi[t], rankList, 20);
			writer.write("Topic " + t + "\n");
			ModelComFunc.writeData(vPhi[t], uniWordMap, rankList, writer, "\t");
			rankList.clear();
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.vPhi")));
		ModelComFunc.writeData(vPhi, writer);
		writer.close();

		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model-topic-behavior.txt")));
		for (int t = 0; t < psi.length; t++) {
			ComUtil.getTop(psi[t], rankList, 10);
			writer.write("Topic " + t + "\n");
			ModelComFunc.writeData(psi[t], uniItemMap, rankList, writer, "\t");
			rankList.clear();
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter(new File(output
				+ "model.psi")));
		ModelComFunc.writeData(psi, writer);
		writer.close();
		return true;
	}

	private boolean checkEqual(double a, double b, String string) {
		if (a != b) {
			System.out.println(string + "\t" + a + "\t" + b);
			return false;
		} else {
			return true;
		}
	}

	public void output(ArrayList<Document> docs, ArrayList<String> uniWords,
			ArrayList<String> uniItems, String outputDir) {
		ArrayList<String> datalines = new ArrayList<String>();
		for (int i = 0; i < docs.size(); i++) {
			for (int j = 0; j < docs.get(i).getDocWords().length; j++) {
				String tmpline = "";
				for (int k1 = 0; k1 < docs.get(i).getDocWords()[j].length; k1++) {
					tmpline += uniWords.get(docs.get(i).getDocWords()[j][k1])
							+ " ";
				}
				for (int k2 = 0; k2 < docs.get(i).getDocItems()[j].length; k2++) {
					tmpline += uniItems.get(docs.get(i).getDocItems()[j][k2])
				}
				datalines.add(tmpline);
			}
			FileUtil.writeLines(outputDir + docs.get(i).getId() + ".txt",
					datalines);
			datalines.clear();
		}
	}

}
