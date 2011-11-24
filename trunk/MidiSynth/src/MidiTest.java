import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Random;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MidiTest {

	MidiThread thread;
	boolean on = false;
	String[] toggleText = {"Play", "Stop"};
	long seed;
	boolean seedSet = false;

	public static void main(String[] args) {        
		new MidiTest();
	}

	public MidiTest() {		
		try {
			Synthesizer synth = MidiSystem.getSynthesizer();
			synth.open();

			final MidiChannel[] mc = synth.getChannels();
			Instrument[] instr = synth.getDefaultSoundbank().getInstruments();		
			synth.loadInstrument(instr[90]);

			JFrame frame = new JFrame("Midi Sample");                
			JPanel pane = new JPanel();                         
			JButton button1 = new JButton(toggleText[0]);
			JButton button2 = new JButton("Set Seed");
			frame.getContentPane().add(pane);
			pane.add(button1);
			pane.add(button2);
			frame.pack();                                       
			frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
			frame.show();

			button1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!on) {
						thread = new MidiThread(mc, seedSet ? seed : ((long)((Math.random()-.5)*Long.MAX_VALUE*2)));
						thread.start();
						seedSet = false;
					} else {
						thread.stop();
					}
					on = !on;
					((JButton)(e.getSource())).setText(toggleText[on ? 1 : 0]);
				}});    // END OF THE ACTION LISTENER
			
			button2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					seed = Long.parseLong(JOptionPane.showInputDialog(((JButton)(e.getSource())).getParent(), "Input seed:"));
					seedSet = true;
				}});    // END OF THE ACTION LISTENER

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}       // END OF THE MAIN METHOD 
}

class MidiThread extends Thread {
	MidiChannel[] mc;
	Random rand;
	
	public MidiThread(MidiChannel[] mc, long seed) {
		this.mc = mc;
		rand = new Random(seed);
		System.out.println("Seed: "+seed);
	}
	
	public void run() {
		int length = (int)(rand.nextDouble()*6)+4;

		int[] note = new int[length];
		int[] time = new int[length];
		int[] wait = new int[length];

		for (int i=0; i<length; i++) {
			note[i] = (int)(rand.nextDouble()*128);
			time[i] = (int)(rand.nextDouble()*500)+100;
			wait[i] = (int)(rand.nextDouble()*500)+100;
		}

		System.out.println("Notes: "+Arrays.toString(note));
		System.out.println("Times: "+Arrays.toString(time));
		System.out.println("Waits: "+Arrays.toString(wait));

		while (true) {
			for (int i=0; i<length; i++) {
				mc[5].noteOn(note[i], time[i]);
				try {
					Thread.sleep(wait[i]);
				} catch (InterruptedException e1) {
				}
			}
		}
	}
}