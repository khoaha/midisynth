import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class SequenceInferer {

	public static void main(String[] args) {
		final double matchFactor = .2;
		
		ArrayList<LoopData> loops = new ArrayList<LoopData>();
		
		try {
			Scanner scanner = new Scanner(new File("sample.txt"));
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
		
		// print
		for (LoopData s : basis)
			s.printStats();
	}
	
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
