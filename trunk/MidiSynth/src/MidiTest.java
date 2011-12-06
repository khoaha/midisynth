import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class MidiTest {

	MidiThread thread;
	boolean on = false;
	int i, count;
	String[] toggleText = {"Play", "Stop"};
	static long seed;
	boolean seedSet = false;
	
	protected String filename;
	protected Format fileformat;
	
	MidiChannel[] mc;

	public static void main(String[] args) {        
		new MidiTest(Format.NOTFILE, "crowd1_sort.txt");
	}

	public MidiTest(Format fileformat, String filename) {		
		try {
			this.fileformat = fileformat;
			this.filename = filename;
			
			Synthesizer synth = MidiSystem.getSynthesizer();
			synth.open();
			
			mc = synth.getChannels();
			Instrument[] instr = synth.getDefaultSoundbank().getInstruments();		
			synth.loadInstrument(instr[90]);

			JFrame frame = new JFrame("Midi Sample");                
			
			JPanel pane = new JPanel();
			
			generateGUI(frame, pane);

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();                                       
			frame.setVisible(true);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}       // END OF THE MAIN METHOD 

	/**
	 * @param frame
	 * @param pane
	 */
	protected void generateGUI(JFrame frame, JPanel pane) {
		JPanel ratePanel = createRatePanel();

		JButton button1 = new JButton(toggleText[0]);
		JButton button2 = new JButton("Set Seed");
		frame.getContentPane().add(pane);
		pane.add(button1);
		pane.add(button2);
		pane.add(ratePanel);
		
		addPlayButtonListener(button1);

		button2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				seed = Long.parseLong(JOptionPane.showInputDialog(((JButton)(e.getSource())).getParent(), "Input seed:"));
				seedSet = true;
			}});    // END OF THE ACTION LISTENER
	}

	/**
	 * @param button1
	 */
	protected void addPlayButtonListener(JButton button1) {
		button1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!on) {
					createMidiThread(fileformat, filename);
				} else {
					thread.shutdown();
				}
				on = !on;
				((JButton)(e.getSource())).setText(toggleText[on ? 1 : 0]);
			}});    // END OF THE ACTION LISTENER
	}

	/**
	 * @return
	 */
	protected JPanel createRatePanel() {
		JPanel ratePanel = new JPanel();
		ButtonGroup ratingGroup = new ButtonGroup();
		JRadioButton[] rating = new JRadioButton[10];

		for(i = 0; i < rating.length; i++){
			rating[i] = new JRadioButton("" + (i+1));
			rating[i].setMnemonic(49 + i);
			if(i == 9)
				rating[i].setMnemonic(48);
			rating[i].addActionListener(new ActionListener() {

				int id = i+1;
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						PrintWriter out = new PrintWriter(new FileOutputStream("rates.txt", true));
						out.println(seed + " " + id);
						out.close();

					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}

				}

			});
			ratingGroup.add(rating[i]);
			ratePanel.add(rating[i]);
		}
		return ratePanel;
	}

	/**
	 * 
	 */
	protected void createMidiThread(Format fileformat, String filename) {
		/* For creating random loops, uncomment and add another field to Format */
		switch(fileformat) {
		case NOTFILE:
			if(!seedSet)
				seed = ((long)((Math.random()-.5)*Long.MAX_VALUE*2));
			thread = new MidiThread(mc, seed);
			seedSet = false;
			break;
		case ARRAY:
		case SEED:
			thread = new MidiThread(mc, filename, fileformat);
		}
		
		thread.start();
	}
}