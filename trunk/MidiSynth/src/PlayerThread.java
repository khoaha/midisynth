import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

class PlayerThread extends Thread {
	private int[][] sheet;
	private boolean running;
	
	public PlayerThread(int[][] sheet) {
		this.sheet = sheet;
		running = true;
	}

	public void run() {
		try {
			Synthesizer synth = MidiSystem.getSynthesizer();
			synth.open();

			final MidiChannel[] mc = synth.getChannels();
			Instrument[] instr = synth.getDefaultSoundbank().getInstruments();		
			synth.loadInstrument(instr[90]);

			while (running) {
				for (int i=0; i<sheet[0].length; i++) {
					if(!running) {
						break;
					}
					
					mc[5].noteOn(sheet[0][i], sheet[1][i]);
					try {
						Thread.sleep(sheet[2][i]);
					} catch (InterruptedException e1) {
					}
				}
			}
			mc[5].allSoundOff();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void shutdown() {
		running = false;
	}
}