package com.luoshaui.beatbox;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.CharBuffer;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.ChangedCharSetException;

public class BeatBox {
	JFrame theFrame;
	JPanel mainPanel;
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkBoxList;
	int nextNum;
	Vector<String> listVector = new Vector<>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
	
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySquence = null;
	Track track;
	
	String[] instrumentName = {"Bass Drum","Closed Hi-Hat","Open Hi-Hat","Acoustic Snare","Crash Cymbal","Hand Clap","High Tom","Hi Bongo","Maracas","Whistle","Low Conga","Cowbell","Vibraslap","Low-mid Tom","High Agogo","Open Hi Conga"};
	int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
	
	public void startUp(String name){
		userName = name;
		//open connection to the server
		try{
			Socket sock = new Socket("127.0.0.1", 4244);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader());
			remote.start();
		}catch(Exception e){
			System.out.println("couldn't connect - you'll have to play alone");
		}
		setUpMidi();
		buildGUI();
	}//close
	
	public void buildGUI(){
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); 
		theFrame.addWindowListener(new WindowAdapter() {
			@SuppressWarnings("unused")
			public void WindowClosing(WindowEvent e){
				int exi = JOptionPane.showConfirmDialog(null, "确定要退出吗？","提醒",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
				if(exi ==JOptionPane.YES_OPTION)System.exit(0);
				else{
					return ;
				}
			}
		});
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		checkBoxList = new ArrayList<>();
		
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);
		
		JButton sendIt = new JButton("Send");
		sendIt.addActionListener(new MySendListener());
		buttonBox.add(sendIt);
		
		userMessage = new JTextField();
		buttonBox.add(userMessage);
		
		incomingList = new JList();
		incomingList.addListSelectionListener(new MyListSelectionListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane theList = new JScrollPane(incomingList);
		buttonBox.add(theList);
		incomingList.setListData(listVector);
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i = 0; i < 16; i++){
			nameBox.add(new Label(instrumentName[i]));
		}
		
		background.add(BorderLayout.EAST,buttonBox);
		background.add(BorderLayout.WEST,nameBox);
		
		theFrame.getContentPane().add(background);
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER,mainPanel);
		
		for(int i = 0;i < 256;i++){
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkBoxList.add(c);
			mainPanel.add(c);
		}
		
		theFrame.setBounds(50,50,300,300);
		theFrame.pack();
		theFrame.setVisible(true);
		
	}//close buildGUI
	
	public void setUpMidi(){
		try{
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		}catch(Exception e){
			e.printStackTrace();
		}
	}//close setUpMidi
	
	public void buildTrackAndStart(){
		ArrayList<Integer> trackList = null;//this will hold the instruments for each
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i = 0; i < 16;i++){
			
			trackList = new ArrayList<>();
			
			for(int j = 0; j< 16; j++){
				JCheckBox jc = (JCheckBox) checkBoxList.get(j+(16*i));
				if(jc.isSelected()){
					int key = instruments[i];
					trackList.add(new Integer(key));
				}else{
					trackList.add(null);//because this slot should be empty in the track
				}
			}//close j-loop
			makeTracks(trackList);
		}//close i-loop
		track.add(makeEvent(192,9,1,0,15));
		try{
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		}catch(Exception e){
			e.printStackTrace();
		}
	}//close buildTrackAndStart
	
	public class MyStartListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			buildTrackAndStart();
		}
	}
	
	public class MyStopListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			sequencer.stop();
		}
	}
	
	public class MyUpTempoListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor*1.03));
		}
	}
	
	public class MyDownTempoListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor*0.97));
		}
	}
	
	public class MySendListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			//make an arraylist of just the state of the checkbox
			boolean[] checkBoxState = new boolean[256];
			for(int i = 0;i < 256;i++){
				JCheckBox check = (JCheckBox) checkBoxList.get(i);
				if(check.isSelected()){
					checkBoxState[i] = true;
				}
			}//close loop
			String messageToSend = null;
			try{
				out.writeObject(userName+nextNum++ +": "+userMessage.getText());
				out.writeObject(checkBoxState);
			}catch(Exception e1){
				System.out.println("Sorry dude.Could not send it to the server");
			}
			userMessage.setText("");
		}
	}//close inner class
	
	public class MyListSelectionListener implements ListSelectionListener{

		@Override
		public void valueChanged(ListSelectionEvent e) {
			// TODO Auto-generated method stub
			if(!e.getValueIsAdjusting()){
				String selected = (String) incomingList.getSelectedValue();
				if(selected!=null){
					//now go to the map,and change the sequence
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}//close inner class
	
	public class RemoteReader implements Runnable{
		boolean[] checkBoxState = null;
		String nameToShow =null;
		Object obj = null;
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
				while((obj= in.readObject()) != null){
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					nameToShow = (String)obj;
					checkBoxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkBoxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}//close while
			}catch(Exception e){
				e.printStackTrace();
			}
		}//close run
	}//close inner class
	
	public class MyPlayMineListenner implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			if(mySquence !=null){
				sequence = mySquence; //restore to my original
			}
		}//close method
	}//close inner class
	
	public void changeSequence(boolean[] checkBoxState){
		for(int i = 0;i < 256;i++){
			JCheckBox check = (JCheckBox) checkBoxList.get(i);
			if(checkBoxState[i]){
				check.setSelected(true);
			}else{
				check.setSelected(false);
			}
		}//close loop
	}//close method
	
	public void makeTracks(ArrayList list){
		Iterator it = list.iterator();
		for(int i = 0;i < 16;i++) {
			Integer num = (Integer)it.next();
			if(num!=null){
				int numKey = num.intValue();
				track.add(makeEvent(144,9,numKey,100,i));
				track.add(makeEvent(128,9,numKey,100,i+1));
			}
		}//close loop
	}//close method
	
	public MidiEvent makeEvent(int comd,int chan,int one, int two,int tick){
		MidiEvent event = null;
		try{
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		}catch(Exception e){}
		return event;
	}
}
