package doomstal.launcher;

import java.awt.Toolkit;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class MainFrame extends JFrame {

    private final Launcher launcher;

    private boolean startAllowed = false;

    public MainFrame(Launcher launcher) {
        this.launcher = launcher;
        initComponents();
    }

    public void allowStart() {
        startAllowed = true;
        startButton.setEnabled(true);
    }

    public void disallowStart() {
        startAllowed = false;
        startButton.setEnabled(false);
    }

    public void disableAll() {
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        startButton.setEnabled(false);
        optionsButton.setEnabled(false);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setProgress(int value, int min, int max) {
        progressBar.setMinimum(min);
        progressBar.setMaximum(max);
        progressBar.setValue(value);
        if(value == min || value == max) {
            progressBar.setStringPainted(false);
        }
    }

    public void setProgressString(String text) {
        progressBar.setStringPainted(true);
        progressBar.setString(text);
    }

    public void updateOptions() {
        memoryField.setText(launcher.options.memory);
        gcField.setText(launcher.options.gc);
        widthField.setText(""+launcher.options.width);
        heightField.setText(""+launcher.options.height);
    }

    public void showOptions() {
        updateOptions();
        optionsDialog.setVisible(true);
    }

    private void closeOptions() {
        optionsDialog.setVisible(false);
        launcher.options.setMemory(memoryField.getText());
        launcher.options.setGc(gcField.getText());
        launcher.options.setWidth(widthField.getText());
        launcher.options.setHeight(heightField.getText());
        launcher.saveOptions();
    }

    private void start() {
        if(!startAllowed) return;
        if(launcher.login(usernameField.getText(), new String(passwordField.getPassword()))) {
            launcher.start();
        }
    }

    private void initComponents() {

        // setIconImage(Toolkit.getDefaultToolkit().getImage("icon.png"));
        try {
            setIconImage(ImageIO.read(ClassLoader.getSystemClassLoader().getResource("icon.png")));
        } catch(IOException e) {
            System.err.println("could not load icon.png");
        }

        optionsDialog = new JDialog();

        KeyAdapter optionsKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if(evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    closeOptions();
                }
            }
        };

        memoryLabel = new JLabel();
        memoryField = new JTextField();
        memoryField.addKeyListener(optionsKeyAdapter);
        ((AbstractDocument)memoryField.getDocument()).setDocumentFilter(new MemoryValueFilter(memoryField));

        gcLabel = new JLabel();
        gcField = new JTextField();
        gcField.addKeyListener(optionsKeyAdapter);
        ((AbstractDocument)gcField.getDocument()).setDocumentFilter(new MemoryValueFilter(gcField));

        widthLabel = new JLabel();
        widthField = new JTextField();
        widthField.addKeyListener(optionsKeyAdapter);
        ((AbstractDocument)widthField.getDocument()).setDocumentFilter(new SizeValueFilter(widthField));

        heightLabel = new JLabel();
        heightField = new JTextField();
        heightField.addKeyListener(optionsKeyAdapter);
        ((AbstractDocument)heightField.getDocument()).setDocumentFilter(new SizeValueFilter(heightField));

        saveButton = new JButton();

        KeyAdapter launcherKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if(evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    start();
                }
            }
        };

        usernameLabel = new JLabel();
        usernameField = new JTextField();
        usernameField.addKeyListener(launcherKeyAdapter);
        passwordLabel = new JLabel();
        passwordField = new JPasswordField();
        passwordField.addKeyListener(launcherKeyAdapter);
        startButton = new JButton();
        startButton.setEnabled(false);
        optionsButton = new JButton();
        statusLabel = new JLabel();
        progressBar = new JProgressBar();

        optionsDialog.setTitle(Strings.optionsTitle);
        optionsDialog.setModalityType(ModalityType.APPLICATION_MODAL);
        optionsDialog.setResizable(false);

        memoryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        memoryLabel.setText(Strings.memory);

        gcLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gcLabel.setText(Strings.gc);

        widthLabel.setHorizontalAlignment(SwingConstants.CENTER);
        widthLabel.setText(Strings.width);

        heightLabel.setHorizontalAlignment(SwingConstants.CENTER);
        heightLabel.setText(Strings.height);

        saveButton.setText(Strings.optionsSave);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeOptions();
            }
        });

        GroupLayout optionsDialogLayout = new GroupLayout(optionsDialog.getContentPane());
        optionsDialog.getContentPane().setLayout(optionsDialogLayout);
        optionsDialogLayout.setHorizontalGroup(
          optionsDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(optionsDialogLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(optionsDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(memoryLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(memoryField)
              .addComponent(gcLabel, GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
              .addComponent(gcField)
              .addComponent(widthField)
              .addComponent(heightLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(heightField)
              .addComponent(widthLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
          .addGroup(optionsDialogLayout.createSequentialGroup()
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(saveButton)
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        optionsDialogLayout.setVerticalGroup(
          optionsDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(optionsDialogLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(memoryLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(memoryField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(gcLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(gcField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(widthLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(widthField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(heightLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(heightField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(saveButton)
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        optionsDialog.pack();
        optionsDialog.setLocationRelativeTo(null);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle(Strings.title);
        setResizable(false);

        usernameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        usernameLabel.setText(Strings.username);

        passwordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        passwordLabel.setText(Strings.password);

        startButton.setText(Strings.play);
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                start();
            }
        });

        optionsButton.setText(Strings.options);
        optionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showOptions();
            }
        });

        statusLabel.setText(Strings.loading);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
          layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
              .addComponent(statusLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(usernameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(usernameField)
              .addComponent(passwordLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(passwordField)
              .addGroup(layout.createSequentialGroup()
                .addComponent(startButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(optionsButton))
              .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE))
            .addContainerGap())
        );
        layout.setVerticalGroup(
          layout.createParallelGroup(GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(usernameLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(usernameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(passwordLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addComponent(startButton)
              .addComponent(optionsButton))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(statusLabel)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }

    class MemoryValueFilter extends DocumentFilter {
        private final JTextField field;

        public MemoryValueFilter(JTextField field) {
            this.field = field;
        }

        @Override
        public void insertString(DocumentFilter.FilterBypass fp, int offset, String string, AttributeSet aset) throws BadLocationException {
            String value = field.getText();
            String newValue = value.substring(0, offset) + string + value.substring(offset);
            if(launcher.options.validMemoryValue(newValue)) {
                super.insertString(fp, offset, string, aset);
            }
        }
        @Override
        public void replace(DocumentFilter.FilterBypass fp, int offset, int length, String string, AttributeSet aset) throws BadLocationException {
            String value = field.getText();
            String newValue = value.substring(0, offset) + string + value.substring(offset+length);
            if(launcher.options.validMemoryValue(newValue)) {
                super.replace(fp, offset, length, string, aset);
            }
        }
        @Override
        public void remove(DocumentFilter.FilterBypass fp, int offset, int length) throws BadLocationException {
            String value = field.getText();
            String newValue = value.substring(0, offset) + value.substring(offset+length);
            if(launcher.options.validMemoryValue(newValue)) {
                super.remove(fp, offset, length);
            }
        }
    }

    class SizeValueFilter extends DocumentFilter {
        private final JTextField field;

        public SizeValueFilter(JTextField field) {
            this.field = field;
        }

        @Override
        public void insertString(DocumentFilter.FilterBypass fp, int offset, String string, AttributeSet aset) throws BadLocationException {
            String value = field.getText();
            String newValue = value.substring(0, offset) + string + value.substring(offset);
            if(launcher.options.validMemoryValue(newValue)) {
                super.insertString(fp, offset, string, aset);
            }
        }
        @Override
        public void replace(DocumentFilter.FilterBypass fp, int offset, int length, String string, AttributeSet aset) throws BadLocationException {
            String value = field.getText();
            String newValue = value.substring(0, offset) + string + value.substring(offset+length);
            if(launcher.options.validSizeValue(newValue)) {
                super.replace(fp, offset, length, string, aset);
            }
        }
        @Override
        public void remove(DocumentFilter.FilterBypass fp, int offset, int length) throws BadLocationException {
            String value = field.getText();
            String newValue = value.substring(0, offset) + value.substring(offset+length);
            if(launcher.options.validSizeValue(newValue)) {
                super.remove(fp, offset, length);
            }
        }
    }

    private JTextField gcField;
    private JLabel gcLabel;
    private JTextField heightField;
    private JLabel heightLabel;
    private JTextField memoryField;
    private JLabel memoryLabel;
    private JButton optionsButton;
    private JDialog optionsDialog;
    private JPasswordField passwordField;
    private JLabel passwordLabel;
    private JProgressBar progressBar;
    private JButton saveButton;
    private JButton startButton;
    private JLabel statusLabel;
    private JTextField usernameField;
    private JLabel usernameLabel;
    private JTextField widthField;
    private JLabel widthLabel;
}
