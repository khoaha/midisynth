import java.util.Arrays;
import java.util.Random;

import javax.sound.midi.MidiChannel;

class MidiThread extends Thread {
	MidiChannel[] mc;
	Random rand;
	protected Format fileformat;
	protected String filename;
	
	protected PlayerThread player;
	
	int[][] loop;

	public MidiThread(MidiChannel[] mc, long seed) {
		this.mc = mc;
		rand = new Random(seed);
		System.out.println("Seed: "+seed);
		fileformat = Format.NOTFILE;
	}
	
	/**
	 * 
	 * @param mc
	 * @param filename
	 * @param fileformat
	 * @param loop loop is a [3][] array
	 */
	public MidiThread(MidiChannel[] mc, String filename, Format fileformat) {
		this.mc = mc;
		this.fileformat = fileformat;
		this.filename = filename;
	}

	public void run() { 
		this.loop = new int[3][];
		switch(fileformat) {
		case NOTFILE:
			int length;
			
			length = (int)(rand.nextDouble()*5)+6;

			loop[0] = new int[length]; /* note */
			loop[1] = new int[length]; /* time */
			loop[2] = new int[length]; /* wait */

			createLoop(length, loop[0], loop[1], loop[2]);
			break;
		case ARRAY:
		case SEED:
			loop = SequenceInferer.createLoopFromFile(filename, fileformat);
		}
		
		player = new PlayerThread(loop);
		player.start();
		
		try {
			player.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param length
	 * @param note
	 * @param time
	 * @param wait
	 */
	protected void createLoop(int length, int[] note, int[] time, int[] wait) {
		for (int i=0; i<length; i++) {
			note[i] = (int)(rand.nextDouble()*128);
			time[i] = (int)(rand.nextDouble()*500)+100;
			wait[i] = (int)(rand.nextDouble()*500)+100;
		}

		System.out.println("Notes: "+Arrays.toString(note));
		System.out.println("Times: "+Arrays.toString(time));
		System.out.println("Waits: "+Arrays.toString(wait));
	}
	
	public void shutdown() {
		player.shutdown();
	}
	
	public int[][] getLoop() {
		return loop;
	}
}