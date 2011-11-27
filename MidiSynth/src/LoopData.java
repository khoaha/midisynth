import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;


public class LoopData {
	
	protected long seed;
	
	protected int length;

	protected int[] note;
	protected int[] time;
	protected int[] wait;
	
	public int noteAvg = 0;
	public int timeAvg = 0;
	public int waitAvg = 0;
	
	protected int[] noteDiv;
	protected int[] timeDiv;
	protected int[] waitDiv;
	
	public double noteStd = 0;
	public double timeStd = 0;
	public double waitStd = 0;
	
	public LoopData(long seed) {
		this.seed = seed;
		Random rand = new Random(seed);
		length = (int)(rand.nextDouble()*6)+4;

		note = new int[length];
		time = new int[length];
		wait = new int[length];
		
		noteDiv = new int[length];
		timeDiv = new int[length];
		waitDiv = new int[length];
		
		// Create values
		for (int i=0; i<length; i++) {
			note[i] = (int)(rand.nextDouble()*128);
			time[i] = (int)(rand.nextDouble()*500)+100;
			wait[i] = (int)(rand.nextDouble()*500)+100;
		}
		
		// Find averages
		for (int i=0; i<length; i++) {
			noteAvg += note[i];
			timeAvg += time[i];
			waitAvg += wait[i];
		}
		noteAvg /= length;
		timeAvg /= length;
		waitAvg /= length;
		
		// Find divergences
		for (int i=0; i<length; i++) {
			noteDiv[i] = note[i] - noteAvg;
			timeDiv[i] = time[i] - timeAvg;
			waitDiv[i] = wait[i] - waitAvg;
		}
		
		// Find standard deviation
		for (int i=0; i<length; i++) {
			noteStd += Math.pow(noteDiv[i], 2);
			timeStd += Math.pow(timeDiv[i], 2);
			waitStd += Math.pow(waitDiv[i], 2);
		}
		noteStd = Math.sqrt(noteStd / length);
		timeStd = Math.sqrt(timeStd / length);
		waitStd = Math.sqrt(waitStd / length);
	}
	
	public void printStats() {
		System.out.println(seed);
		System.out.println("Notes: "+Arrays.toString(note));
		System.out.println("N avg: "+Arrays.toString(noteDiv)+"  ("+noteAvg+") ["+noteStd+"]");
		System.out.println("Times: "+Arrays.toString(time));
		System.out.println("T avg: "+Arrays.toString(timeDiv)+"  ("+timeAvg+") ["+timeStd+"]");
		System.out.println("Waits: "+Arrays.toString(wait));
		System.out.println("W avg: "+Arrays.toString(waitDiv)+"  ("+waitAvg+") ["+waitStd+"]");
	}
}