import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Date;
import javax.sound.sampled.*;
import javax.swing.*;

import static javax.swing.WindowConstants.HIDE_ON_CLOSE;

public class ClockPanel extends JPanel {
    static final Color background = new Color(255, 255, 255);
    static final Color buttonBackground = new Color(189, 189, 189);
    private static ClockPanel clockPanel;
    private Clock clock;
    private static Date selectedTime;
    private static Thread timeThread;
    private static Thread clockThread;

    public ClockPanel(Clock сlock) {
        setBackground(background);
        setClock(сlock);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (clock != null) {
            drawClock(g, clock);
        }
    }

    private void drawCircle(Graphics g, Point center, int radius) {
        g.setColor(Color.RED);
        g.fillOval(center.x - radius / 2, center.y - radius / 2, radius, radius);
    }

    private Point getEndPoint(double angle, int radius) {
        Point O = new Point(getSize().width / 2, getSize().height / 2);
        int x = (int) (O.x + radius * Math.cos(angle));
        int y = (int) (O.y - radius * Math.sin(angle));
        return new Point(x, y);
    }

    private void drawClock(Graphics g, Clock сlock) {
        Point O = new Point(getSize().width / 2, getSize().height / 2);
        int radiusClock = Math.min(O.x, O.y) - 20;
        int radiusSecond = radiusClock - 10;
        int radiusMinute = radiusClock - 10;
        int radiusHour = radiusMinute - 20;
        double angle;
        for (int i = 1; i < 13; i++) {
            angle = Math.PI / 2 - i * Math.PI / 6;
            Point point = getEndPoint(angle, radiusClock);
            drawCircle(g, point, 8);
        }
        Graphics2D g2d = (Graphics2D) g;
        angle = Math.PI / 2 - (clock.getHours() + clock.getMinutes() / 60.0 + clock.getSeconds() / 3600.0) * Math.PI / 6;
        Point point = getEndPoint(angle, radiusHour);
        g2d.setColor(Color.GREEN);
        float thickness = 4.0f;
        BasicStroke stroke = new BasicStroke(thickness);
        g2d.setStroke(stroke);
        g2d.drawLine(O.x, O.y, point.x, point.y);

        angle = Math.PI / 2 - (clock.getMinutes() + clock.getSeconds() / 60.0) * Math.PI / 30;
        point = getEndPoint(angle, radiusMinute);
        g2d.setColor(Color.GRAY);
        thickness = 2.0f;
        stroke = new BasicStroke(thickness);
        g2d.setStroke(stroke);
        g2d.drawLine(O.x, O.y, point.x, point.y);

        angle = Math.PI / 2 - clock.getSeconds() * Math.PI / 30;
        point = getEndPoint(angle, radiusSecond);
        g2d.setColor(Color.RED);
        thickness = 2.0f;
        stroke = new BasicStroke(thickness);
        g2d.setStroke(stroke);
        g2d.drawLine(O.x, O.y, point.x, point.y);
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private static class StartActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            LocalTime currentTime = LocalTime.now();

            clockPanel.getClock().setHours(currentTime.getHour());
            clockPanel.getClock().setMinutes(currentTime.getMinute());
            clockPanel.getClock().setSeconds(currentTime.getSecond());
            timeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception exc) {
                        };
                        clockPanel.getClock().setSeconds(clockPanel.getClock().getSeconds() + 1);
                        if(clockPanel.getClock().getSeconds() == 0){
                            clockPanel.getClock().setMinutes(clockPanel.getClock().getMinutes() + 1);
                        }
                        if(clockPanel.getClock().getSeconds() == 0 && clockPanel.getClock().getMinutes() == 0){
                            clockPanel.getClock().setHours(clockPanel.getClock().getHours() + 1);
                        }
                        clockPanel.repaint();
                    }
                }
            });
            timeThread.start();
        }
    }

    private static class StopActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            timeThread.stop();
        }
    }

    private static class DialogActionListener implements ActionListener {
        JFrame frame;
        JLabel label;
        public DialogActionListener(JFrame frame, JLabel label){
            this.frame = frame;
            this.label = label;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            new DialogWindow(frame,label);
        }
    }

    private static class DialogWindow {
        Thread thread;
        JFrame frame;
        JDialog dialog;
        JLabel label;
        JButton button;
        public DialogWindow(JFrame frame, JLabel alarmLabel){
            this.frame = frame;
            dialog = new JDialog(frame,"Enter time",true);
            dialog.setSize(500, 250);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(HIDE_ON_CLOSE);
            dialog.setLayout(null);

            SpinnerDateModel model = new SpinnerDateModel();
            JSpinner timeSpinner = new JSpinner(model);
            Font spinnerFont = timeSpinner.getFont().deriveFont(Font.PLAIN, 25);
            timeSpinner.setFont(spinnerFont);

            JSpinner.DateEditor editor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss");
            editor.setSize(100,40);
            timeSpinner.setEditor(editor);
            timeSpinner.setBounds(20,50,400,50);
            dialog.add(timeSpinner);

            label = new JLabel("Выберите время: ");
            label.setBounds(20,20,450,25);

            button = new JButton("OK");
            button.setBounds(200,160,100,25);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedTime = (Date) timeSpinner.getValue();
                    dialog.setVisible(false);
                    clockThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int flag = 1;
                            alarmLabel.setVisible(true);
                            if (selectedTime.getHours() <= clockPanel.getClock().getHours() &&
                                    selectedTime.getMinutes() <= clockPanel.getClock().getMinutes() &&
                                    selectedTime.getSeconds() < clockPanel.getClock().getSeconds()) {
                                flag = 0;
                            }
                            while (!(selectedTime.getHours() == clockPanel.getClock().getHours() &&
                                    selectedTime.getMinutes() == clockPanel.getClock().getMinutes() &&
                                    selectedTime.getSeconds() == clockPanel.getClock().getSeconds())) {
                                if(flag == 0){
                                    int durationInSeconds = selectedTime.getHours()*3600+selectedTime.getMinutes()*60+selectedTime.getSeconds()+(23-clockPanel.getClock().getHours())*3600+(59-clockPanel.getClock().getMinutes())*60+(60-clockPanel.getClock().getSeconds());
                                    alarmLabel.setText("Будильник сработает через "+(durationInSeconds/3600)+ " ч "+(durationInSeconds%3600)/60+ " мин "+((durationInSeconds%3600)%60)+ " с");
                                    alarmLabel.repaint();
                                } else{
                                    int durationInSeconds = selectedTime.getHours()*3600+selectedTime.getMinutes()*60+selectedTime.getSeconds()-(clockPanel.getClock().getHours()*3600)-(clockPanel.getClock().getMinutes()*60)-(clockPanel.getClock().getSeconds());
                                    alarmLabel.setText("Будильник сработает через "+(durationInSeconds/3600)+ " ч "+(durationInSeconds%3600)/60+ " мин "+((durationInSeconds%3600)%60)+ " с");
                                    alarmLabel.repaint();
                                }

                                try {
                                    Thread.sleep(1000);
                                } catch (Exception exc) {
                                };
                            }
                            alarmLabel.setVisible(false);
                            new ClockWindow(frame);
                        }
                    });
                    clockThread.start();

                }
            });
            button.setBackground(buttonBackground);

            dialog.add(label);
            dialog.add(button);

            dialog.setVisible(true);
        }
    }

    private static class ClockWindow {
        JFrame frame;
        JDialog dialog;
        JLabel label;
        JButton button;
        public ClockWindow(JFrame frame){
            this.frame = frame;
            dialog = new JDialog(frame,"Message",true);
            dialog.setSize(400, 200);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(HIDE_ON_CLOSE);
            dialog.setLayout(null);



            label = new JLabel("Будильник сработал");
            Font clockFont = label.getFont().deriveFont(Font.PLAIN, 25);
            label.setFont(clockFont);
            label.setBounds(75,20,250,40);

            button = new JButton("OK");
            button.setBounds(150,100,100,25);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            button.setBackground(buttonBackground);

            dialog.add(label);
            dialog.add(button);
            playAudio();
            dialog.setVisible(true);
        }
    }

    private static void playAudio() {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File("C:\\Users\\hinol\\IdeaProjects\\lab6_Platonova\\src\\budilnik.wav"));

            AudioFormat format = audioIn.getFormat();

            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip) AudioSystem.getLine(info);

            clip.open(audioIn);
            clip.start();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Alarm");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(new Dimension(600,700));
        f.setLocationRelativeTo(null);
        f.setLayout(null);

        clockPanel = new ClockPanel(new Clock(0, 0,0));
        clockPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        clockPanel.setBounds(100,20,400,400);
        f.add(clockPanel);

        JButton startButton = new JButton("Start");
        startButton.addActionListener(new StartActionListener());
        startButton.setBounds(100,430,400,25);
        startButton.setBackground(buttonBackground);
        f.add(startButton);

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new StopActionListener());
        stopButton.setBounds(100,465,400,25);
        stopButton.setBackground(buttonBackground);
        f.add(stopButton);

        JLabel alarmLabel = new JLabel("");
        alarmLabel.setBounds(100,540,400,25);
        alarmLabel.setVisible(false);
        f.add(alarmLabel);


        JButton setButton = new JButton("Set an alarm");
        setButton.addActionListener(new DialogActionListener(f, alarmLabel));
        setButton.setBounds(100,500,400,25);
        setButton.setBackground(buttonBackground);
        f.add(setButton);
        f.setVisible(true);
    }
}