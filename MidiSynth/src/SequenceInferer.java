import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

/**
 * Uses statistical inference to generate new loops that fit with a provided
 * set of loops.
 */
public class SequenceInferer {

	private enum Format {
		SEED, ARRAY;
	}

	public static void main(String[] args) {
		/* ArrayList<LoopData> set = matchLoops("arraysample.txt", Format.ARRAY); */
		ArrayList<LoopData> set = matchLoops("crowd1_sort.txt", Format.SEED);
		int[] shifts = alignLoops(set);
		int[][] loop = composeLoop(set, shifts);

		// print
		for (LoopData s : set) {
			s.printStats();
		}

		System.out.println("Final: ");
		for (int[] a : loop) {
			System.out.println(Arrays.toString(a));
		}

		// preview sound
		PreviewThread thread = new PreviewThread(loop);
		thread.start();
	}

	/**
	 * Returns a list of similar loops from the dataset within a given matching
	 * factor, based on the metrics of standard deviation in notes, note lengths,
	 * pauses, and difference between note lengths and pauses.
	 */
	protected static ArrayList<LoopData> matchLoops(String file, Format f) {
		final double matchFactor = .2;

		ArrayList<LoopData> loops = new ArrayList<LoopData>();

		try {
			Scanner scanner = new Scanner(new File(file));
			switch(f) {
			case SEED:
				while (scanner.hasNextLine()) {
					String str = scanner.nextLine();
					if (str.length() > 0) {
						String[] splitstr = str.split(" ");
						LoopData d = new LoopData(Long.parseLong(splitstr[0]), Integer.parseInt(splitstr[1]));
						loops.add(d);
						//d.printStats();
					}
				}
				break;
			case ARRAY:
				String rating = "", note, time, wait;
				/* Read in the format output by the sequencer */
				while(scanner.hasNextLine()) {
					/* read in, handling newlines separating data.
					 * There can be any number of newlines separating each three-line sequence */
					while(rating.length() == 0)
						rating = scanner.nextLine();
					note = scanner.nextLine();
					time = scanner.nextLine();
					wait = scanner.nextLine();

					/* remove leading and trailing []s */
					note = note.substring(1, note.length()-1);
					time = time.substring(1, time.length()-1);
					wait = wait.substring(1, wait.length()-1);

					String noteArr[] = note.split(", ");
					String timeArr[] = time.split(", ");
					String waitArr[] = wait.split(", ");

					LoopData d = new LoopData(parseStringArray(noteArr), parseStringArray(timeArr), parseStringArray(waitArr), Integer.parseInt(rating));

					loops.add(d);
					//d.printStats();
				}
			}

			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		/** select basis **/
		ArrayList<LoopData> basis = new ArrayList<LoopData>();
		// pick first one at random
		basis.add(loops.remove((int)(Math.random()*loops.size())));
		// find similarity in other loops
		ArrayList<Double> factor = new ArrayList<Double>();
		for (LoopData l : loops) {
			double noteStd = Math.abs(l.noteStd - basis.get(0).noteStd)/128;
			double timeStd = Math.abs(l.timeStd - basis.get(0).timeStd)/500;
			double waitStd = Math.abs(l.waitStd - basis.get(0).waitStd)/500;
			double paceStd = Math.abs((l.timeAvg - l.waitAvg) - (basis.get(0).timeAvg - basis.get(0).waitAvg))/256;
			factor.add((noteStd + timeStd + waitStd + paceStd)/4);
		}
		sortLoops(loops, factor, 0, loops.size()-1);
		// add closest matches as basis
		for (int i=0; i<matchFactor*loops.size(); i++)
			basis.add(loops.remove(0));

		return basis;
	}

	/**
	 * @param strArr
	 */
	private static int[] parseStringArray(String[] strArr) {
		int arr[] = new int[strArr.length];
		for(int i = 0; i < strArr.length; i ++)
			arr[i] = Integer.parseInt(strArr[i]);
		return arr;
	}

	/**
	 * Returns an array of indices representing the shifting needed for the longest
	 * loop to align with the loop at index i. Shifting is performed as to minimize
	 * the standard deviation of notes across tracks. Tracks are "scaled" to compensate
	 * for unequal lengths.
	 */
	protected static int[] alignLoops(ArrayList<LoopData> loops) {
		// find longest loop
		int size = 0;
		LoopData longest = null;
		for (LoopData l : loops) {
			if (l.length > size) {
				size = l.length;
				longest = l;
			}
		}

		// shift loops to align
		int shifts[] = new int[loops.size()];
		for (int n=0; n<loops.size(); n++) {
			LoopData l = loops.get(n);
			if (l == longest)
				continue;
			double minStd = Double.MAX_VALUE;
			// check all alignments
			for (int i=0; i<size; i++) {
				double std = 0;
				int[] dif = new int[size];
				// find divergences
				for (int j=0; j<size; j++) {
					double scaleIndex = (double) j * l.length / size;
					int leftNote = l.note[(int)scaleIndex];
					int rightNote = l.note[(int)Math.min(scaleIndex+1, l.length-1)];
					int tweenNote = leftNote + (int)((rightNote-leftNote)*(scaleIndex%1));
					dif[j] = tweenNote - longest.note[(i + j) % longest.note.length];
				}
				// find standard deviation
				for (int k=0; k<size; k++)
					std += Math.pow(dif[k], 2);
				std = Math.sqrt(std / size);
				// check if smallest
				if (std < minStd) {
					shifts[n] = i;
					minStd = std;
				}
			}
		}

		return shifts;
	}

	/**
	 * Returns an array of notes, note lengths, and pauses. Generates a new loop
	 * from the provided set of loops and their given shifts by inspecting patterns
	 * in the set's normalized note sequences and assigning like notes similar
	 * durations and pauses. The final composition is then de-normalized.
	 */
	protected static int[][] composeLoop(ArrayList<LoopData> loops, int[] shifts) {
		final int accuracy = 10;

		Random rand = new Random();

		// store and normalize ratings
		double[] rating = new double[loops.size()];
		double ratingSum = 0;
		double ratingAvg = 0;
		for (int i=0; i<rating.length; i++) {
			rating[i] = loops.get(i).rating;
			ratingSum += rating[i];
		}
		ratingAvg = ratingSum / rating.length;
		ratingSum = 0;
		for (int i=0; i<rating.length; i++) {
			rating[i] /= ratingAvg;
			ratingSum += rating[i];
		}

		// find longest loop
		int maxSize = 0;
		for (LoopData l : loops) {
			if (l.length > maxSize)
				maxSize = l.length;
		}

		// randomly copy size
		// Keep statically random
		int size = loops.get((int)(Math.random()*loops.size())).length;

		double[] noteNrm = new double[size];
		int[] note = new int[size];
		int[] time = new int[size];
		int[] wait = new int[size];

		// prepare for de-normalizing notes
		int allNoteAvg = 0;
		double variance = 0;
		for (int i=0; i<loops.size(); i++) {
			LoopData l = loops.get(i);
			allNoteAvg += l.noteAvg * rating[i];
			variance += Math.pow(l.noteStd, 2);
		}
		allNoteAvg /= ratingSum;
		variance = Math.sqrt(variance);
		// determine the note range
		// TODO: Consider a better algorithm?
		int minNote = (int)Math.max(allNoteAvg - variance*(cappedGaussianRandom(rand) + 1), 0);
		int maxNote = (int)Math.min(allNoteAvg + variance*(cappedGaussianRandom(rand) + 1), 128);

		// randomize first note
		// Leave statically random
		noteNrm[0] = Math.random();
		// set all notes, pauses, and durations
		for (int n=0; n<size; n++) {
			int index = 0;
			double minDif = Double.MAX_VALUE;
			// match note to closest across all loops	
			int offset = (int)(Math.random()*maxSize);
			for (int i=0; i<maxSize; i++) {
				double dif = 0;
				// Find divergences, to a degree of fuzziness
				for (int j=0; j<loops.size(); j++) {
					LoopData l = loops.get(j);
					int ind = (int)((double)l.length/maxSize*((i+offset+shifts[j])%maxSize));
					dif += Math.floor(Math.abs(l.noteNrm[ind] - noteNrm[n]) * accuracy) / accuracy;
				}
				// Find lowest difference
				if (dif < minDif) {
					minDif = dif;
					index = (i+offset)%maxSize;
				}
			}
			
			// find duration and pause averages
			int timeAvg = 0;
			int waitAvg = 0;
			double nextAvg = 0;
			for (int i=0; i<loops.size(); i++) {
				LoopData l = loops.get(i);
				int ind = (int)((double)l.length/maxSize*((index+shifts[i])%maxSize));
				timeAvg += l.time[ind] * rating[i];
				waitAvg += l.wait[ind] * rating[i];
				// prepare next note
				if (n < size-1)
					nextAvg += (l.noteNrm[(ind+1)%l.length] - l.noteNrm[ind]) * rating[i];
				}
			timeAvg /= ratingSum;
			waitAvg /= ratingSum;
			nextAvg /= ratingSum;

			// find standard deviations
			double timeStd = 0;
			double waitStd = 0;
			double nextStd = 0;
			for (int i=0; i<loops.size(); i++) {
				LoopData l = loops.get(i);
				int ind = (int)((double)l.length/maxSize*((index+shifts[i])%maxSize));
				timeStd += Math.pow(l.time[ind] - timeAvg, 2);
				waitStd += Math.pow(l.wait[ind] - waitAvg, 2);
				// prepare next note
				if (n < size-1)
					nextStd += Math.pow(l.noteNrm[ind] - l.noteNrm[(ind+1)%l.length] - nextAvg, 2);
			}
			timeStd = Math.sqrt(timeStd / loops.size());
			waitStd = Math.sqrt(waitStd / loops.size());
			nextStd = Math.sqrt(nextStd / loops.size());

			// set values
			time[n] = (int)Math.max(0, timeAvg + cappedGaussianRandom(rand)*timeStd);
			wait[n] = (int)Math.max(0, waitAvg + cappedGaussianRandom(rand)*waitStd);
			
			// pick next note
			if (n < size-1)
				noteNrm[n+1] = Math.min(128, Math.max(0, note[n] + nextAvg + cappedGaussianRandom(rand)*nextStd));
		}

		// re-normalize normals
		double normMin = Double.MAX_VALUE;
		double normMax = 0;
		for (double n : noteNrm) {
			normMin = Math.min(normMin, n);
			normMax = Math.max(normMax, n);
		}
		for (int i=0; i<size; i++)
			noteNrm[i] = (noteNrm[i] - normMin)/(normMax - normMin);

		// restore notes
		for (int i=0; i<size; i++)
			note[i] = (int) (minNote + (maxNote - minNote)*noteNrm[i]);

		// return all
		int[][] all = new int[3][size];
		all[0] = note;
		all[1] = time;
		all[2] = wait;

		return all;
	}

	/**
	 * Utility function, quicksorts array of loops based on array of factors.
	 */
	protected static void sortLoops(ArrayList<LoopData> loops, ArrayList<Double> factor, int base, int top){
		int low = base;
		int high = top;
		if (low >= top)
			return;
		double pivot = factor.get((low + high) / 2);
		while (low < high) {
			while (low < high && factor.get(low) < pivot)
				low++;
			while (low < high && factor.get(high) > pivot)
				high--;
			if (low < high) {
				LoopData temp = loops.get(low);
				loops.set(low, loops.get(high));
				loops.set(high, temp);
				double temp2 = factor.get(low);
				factor.set(low, factor.get(high));
				factor.set(high, temp2);
			}
		}
		if (high < low) {
			LoopData temp = loops.get(high);
			loops.set(high, loops.get(low));
			loops.set(low, temp);
			double temp2 = factor.get(high);
			factor.set(high, factor.get(low));
			factor.set(low, temp2);
		}
		sortLoops(loops, factor, base, low);
		sortLoops(loops, factor, low == base ? low+1 : low, top);
	}

	/**
	 * Returns a normally distributed value between -1 and 1.  
	 */
	protected static double cappedGaussianRandom(Random rand) {
		double i;
		do {
			i = rand.nextGaussian() / 3.0;
		} while(i < -1 || i > 1);
		return i;
	}

}

class PreviewThread extends Thread {
	int[][] sheet;

	public PreviewThread(int[][] sheet) {
		this.sheet = sheet;
	}

	public void run() {
		try {
			Synthesizer synth = MidiSystem.getSynthesizer();
			synth.open();

			final MidiChannel[] mc = synth.getChannels();
			Instrument[] instr = synth.getDefaultSoundbank().getInstruments();		
			synth.loadInstrument(instr[90]);

			while (true) {
				for (int i=0; i<sheet[0].length; i++) {
					mc[5].noteOn(sheet[0][i], sheet[1][i]);
					try {
						Thread.sleep(sheet[2][i]);
					} catch (InterruptedException e1) {
					}
				}
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}