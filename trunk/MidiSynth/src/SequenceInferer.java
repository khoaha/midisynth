/**
 * Uses statistical inference to generate new loops that fit with a provided
 * set of loops.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;


public class SequenceInferer {

	public static void main(String[] args) {
		ArrayList<LoopData> set = matchLoops("sample.txt");
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
	protected static ArrayList<LoopData> matchLoops(String file) {
		final double matchFactor = .2;

		ArrayList<LoopData> loops = new ArrayList<LoopData>();

		try {
			Scanner scanner = new Scanner(new File(file));
			while (scanner.hasNextLine()) {
				String str = scanner.nextLine();
				if (str.length() > 0) {
					LoopData d = new LoopData(Long.parseLong(str));
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
			size = Math.max(size, l.length);
			longest = l;
		}
		// shift loops to align
		int shifts[] = new int[loops.size()];
		for (int n=0; n<loops.size(); n++) {
			LoopData l = loops.get(n);
			double minStd = Double.MAX_VALUE;
			// check all alignments
			for (int i=0; i<size; i++) {
				double std = 0;
				int[] dif = new int[size];
				// find divergences
				for (int j=0; j<size; j++) {
					double scaleIndex = (double) j * l.length / size;
					int tweenNote = (int)((1 + scaleIndex%1)*l.note[(int)scaleIndex] + (1 - scaleIndex%1)*l.note[(int)Math.min(scaleIndex+1, l.length-1)]);
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

		// find longest loop
		int maxSize = 0;
		LoopData longest = null;
		for (LoopData l : loops) {
			maxSize = Math.max(maxSize, l.length);
			longest = l;
		}

		// randomly copy size
		int size = loops.get((int)(Math.random()*loops.size())).length;

		double[] noteNrm = new double[size];
		int[] note = new int[size];
		int[] time = new int[size];
		int[] wait = new int[size];

		// prepare for de-normalizing notes
		int allNoteAvg = 0;
		double variance = 0;
		for (LoopData l : loops) {
			allNoteAvg += l.noteAvg;
			variance += Math.pow(l.noteStd, 2);
		}
		allNoteAvg /= loops.size();
		variance = Math.sqrt(variance);
		// determine the note range
		int minNote = (int)Math.max(allNoteAvg - 2*variance*Math.random(), 0);
		int maxNote = (int)Math.min(allNoteAvg + 2*variance*Math.random(), 128);

		// randomize first note
		noteNrm[0] = Math.random();
		// set all notes, pauses, and durations
		for (int n=0; n<size; n++) {
			int index = 0;
			double[] dif = new double[maxSize];
			// Find divergences, to a degree of fuzziness
			for (int i=0; i<loops.size(); i++) {
				int ind = (int)(double)loops.get(i).length/maxSize*((index+shifts[i])%maxSize);
				dif[i] = Math.floor((longest.noteNrm[i] - noteNrm[ind]) * accuracy) / accuracy;
			}
			double minDif = Double.MAX_VALUE;
			int shift = (int)(Math.random()*maxSize);
			// Find lowest difference
			for (int i=0; i<loops.size(); i++) {
				if (dif[(i+shift)%maxSize] < minDif) {
					minDif = dif[(i+shift)%maxSize];
					index = (i+shift)%maxSize;
				}
			}
			// find duration and pause averages
			int timeAvg = 0;
			int waitAvg = 0;
			double nextAvg = 0;
			for (int i=0; i<loops.size(); i++) {
				LoopData l = loops.get(i);
				int ind = (int)(double)l.length/maxSize*((index+shifts[i])%maxSize);
				timeAvg += l.time[ind];
				waitAvg += l.wait[ind];
				// prepare next note
				if (n < size-1)
					nextAvg += l.noteNrm[ind] - l.noteNrm[(ind+1)%l.length];
			}
			timeAvg /= loops.size();
			waitAvg /= loops.size();
			nextAvg /= loops.size();
			// find standard deviations
			double timeStd = 0;
			double waitStd = 0;
			double nextStd = 0;
			for (int i=0; i<loops.size(); i++) {
				LoopData l = loops.get(i);
				int ind = (int)(double)l.length/maxSize*((index+shifts[i])%maxSize);
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
			time[n] = (int)(timeAvg + (2*Math.random()-1)*timeStd);
			wait[n] = (int)(waitAvg + (2*Math.random()-1)*waitStd);

			// pick next note
			if (n < size-1)
				noteNrm[n+1] = note[n] + nextAvg + (2*Math.random()-1)*nextStd;
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