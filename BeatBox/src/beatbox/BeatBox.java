package beatbox;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.io.*;
import java.net.Socket;
import javax.swing.event.*;

/**
 * @author Abhishek Arora
 */
public class BeatBox {
    
    JFrame theFrame = null;
    JPanel mainPanel = null;
    JTextField userMessage;
    ArrayList<JCheckBox> checkboxList = new ArrayList<>();
    JList incomingList;
    Vector<String> listVector = new Vector<String>();
    
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    
    String userName;
    int nextNum;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
     
    String[] instrumentNames = {
        "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
        "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cow Bell", "Vibra Slap",
        "Low-mid Tom", "High Agogo", "Open Hi Conga"
    };
    
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
    
    public static void main(String[] args) {
        String name;
        try {
            name = args[0];
        }
        catch(Exception ex) {
            name = "Guest";
        }
        
        new BeatBox().startUp(name);
    }
    
    public void startUp(String name) {
        userName = name;
        //open a connection to the server
        try {
            Socket sock = new Socket("139.59.26.127", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();   
        }
        catch(Exception ex) {
            System.out.println("Couldn't connect - you'll have to play alone.");
        }
        setUpMidi();
        buildGui();
    }
    
    public void buildGui() {
        theFrame = new JFrame("Cyber BeatBox"); // main frame 
        
        // background JPanel
        BorderLayout layout = new BorderLayout();// set JPanel to border layout
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); // set empty border around panel for padding
        
        /*
         *  Menu Bar
         */
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newMenuItem = new JMenuItem("New");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        
        newMenuItem.addActionListener(new NewMenuListener());
        openMenuItem.addActionListener(new OpenMenuListener());
        saveMenuItem.addActionListener(new SaveMenuListener());
        
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        
        
        menuBar.add(fileMenu);
        theFrame.setJMenuBar(menuBar);
                
        /*
         *  Right Side - Buttons
         */
        
        // button box layout
        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        
        // Create Buttons
        JButton start = new JButton("Start");
        JButton stop = new JButton("Stop");
        JButton upTempo = new JButton("Tempo Up");
        JButton downTempo = new JButton("Tempo Down");
        JButton randFill = new JButton("Random Fill");
        JButton sendIt = new JButton("SendIt");
        //JButton save = new JButton("Save");
        //JButton restore = new JButton("Restore");
        
        // Register ActionListeners to all buttons
        start.addActionListener(new MyStartListener());
        stop.addActionListener(new MyStopListener());
        upTempo.addActionListener(new MyUpTempoListener());
        downTempo.addActionListener(new MyDownTempoListener());
        randFill.addActionListener(new MyRandFillListener());
        sendIt.addActionListener(new MySendListener());
        //save.addActionListener(new MySaveListener());
        //restore.addActionListener(new MyRestoreListener());
        
        // Add all buttons to buttonBox
        buttonBox.add(start);
        buttonBox.add(stop);
        buttonBox.add(upTempo);
        buttonBox.add(downTempo);
        buttonBox.add(randFill);
        //buttonBox.add(save);
        //buttonBox.add(restore);
        
        // Messages        
        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector); // no data to start with

        userMessage = new JTextField();
        buttonBox.add(userMessage);
        
        buttonBox.add(sendIt);
        
        // Add buttonBox to JPanel background
        background.add(BorderLayout.EAST, buttonBox);
        
        
        /*
         *  Left Side - Instrument Labels
         */
        
        // labels box layout
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        
        // add instrument labels
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }
        
        // Add nameBox to JPanel background
        background.add(BorderLayout.WEST, nameBox);
          
        
        /*
         *  Center - Checkboxes
         */
        
        // create new grid layout
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        
        // create another panel
        mainPanel = new JPanel(grid);
         
        // Set padding around mainPanel
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        // create checkboxes, set them to false, add them to arraylist and to gui panel
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c); // add to ArrayList checkboxList
            mainPanel.add(c); // add to GUI Panel
        }
            
        // Add mainPanel to background Panel
        background.add(BorderLayout.CENTER, mainPanel);
         
        /*
         *  Configure the frame settings
         */
        
        theFrame.getContentPane().add(background);
        
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }
    
    public void setUpMidi() {
        try {
            
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            
            sequence = new Sequence(Sequence.PPQ, 4);
            
            track = sequence.createTrack();
            
            sequencer.setTempoInBPM(120);
            
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null;
        
        //get rid of old track, make a new one
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        
        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();
            
            int key = instruments[i];
            
            for (int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkboxList.get((i * 16) + j);
                if ( jc.isSelected() ) {
                    trackList.add(new Integer(key));
                }
                else {
                    trackList.add(null);
                }
            }
            
            makeTracks(trackList);
            
            track.add(makeEvent(176, 1, 127, 0, 16));
        }
        
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void makeTracks(ArrayList list) {
        Iterator it = list.iterator();
        for (int i = 0; i < 16; i++) {
            Integer num = (Integer) it.next();
            
            if (num != null) {
                int numKey = num.intValue();
                track.add(makeEvent(144, 9, numKey, 100, i));       
                track.add(makeEvent(128, 9, numKey, 100, i+1));
            }
        }
    }
    
    public void changeSequence(boolean[] checkboxState) {
        for ( int i = 0; i < 256; i++ ) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (checkboxState[i]) {
                check.setSelected(true);
            }
            else {
                check.setSelected(false);
            }
        }
    }
    
    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
            
        } 
        catch (Exception e) {
            e.printStackTrace(); 
        }
        return event;
    }
    
    // Inner Classes - Listeners
    class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }
    
    class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }
    
    class MyUpTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }
    
    class MyDownTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        }
    }
    
    class MyRandFillListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                check.setSelected(false);
                if ( ((int) (Math.random()*10)) == 1 ) {
                   check.setSelected(true);
                }
            }
            
            sequencer.stop();
            buildTrackAndStart();
        }
    }
    
    class NewMenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                check.setSelected(false);
            } 
        }
    }
    
    class SaveMenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            
            // Boolean array to hold the state of each checkbox
            boolean[] checkBoxState = new boolean[256];
            
            // Choose File
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                   checkBoxState[i] = true;
                }
            }
            
            try {
                FileOutputStream fileStream = new FileOutputStream(fileSave.getSelectedFile());
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkBoxState);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    class OpenMenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            boolean[] checkBoxState = null;
            
            // Choose File
            JFileChooser fileOpen = new JFileChooser();
            fileOpen.showOpenDialog(theFrame);
            
            // deserialize
            try {
                FileInputStream fileIn = new FileInputStream(fileOpen.getSelectedFile());
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkBoxState = (boolean[]) is.readObject();
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }

            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if ( checkBoxState[i] ) {
                   check.setSelected(true);
                }
                else {
                    check.setSelected(false);
                }
            }
            
            sequencer.stop();
            buildTrackAndStart();
        }
    }
    
    class MySendListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent a) {
            // make an arraylist of just the state of the checkboxes
            boolean[] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }
            String messageToSend = null;
            try {
                out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
                out.writeObject(checkboxState);
            }
            catch(Exception ex) {
                System.out.println("Sorry, we couldn't send it to the server.");
            }
            userMessage.setText("");
        }
    }
    
    class MyListSelectionListener implements ListSelectionListener{
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null) {
                    // Change the sequence
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }
    
    class RemoteReader implements Runnable {
        boolean[] checkboxState = null;
        String nameToShow = null;
        Object obj = null;
        @Override
        public void run() {
            try {
                while( (obj=in.readObject()) != null ) {
                    System.out.println("Got an object from server");
                    System.out.println(obj.getClass());
                    nameToShow = (String) obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
}
