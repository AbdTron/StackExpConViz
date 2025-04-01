import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Enumeration;

// This program shows how expressions are converted between prefix, infix, and postfix
// I used Swing for the UI and tried to make it look as nice as possible
public class StackExpConViz extends JFrame {
    // Class to store expression history
    private static class ExpressionHistoryRecord {
        private String inputExpression;
        private String result;
        private String conversionMode;

        public ExpressionHistoryRecord(String inputExpression, String result, String conversionMode) {
            this.inputExpression = inputExpression;
            this.result = result;
            this.conversionMode = conversionMode;
        }

        @Override
        public String toString() {
            return conversionMode + ": " + inputExpression + " → " + result;
        }

        public String getInputExpression() {
            return inputExpression;
        }
    }

    // Stack to store operands during conversion
    private ArrayList<String> stack;
    // UI panel to show the stack visually
    private JPanel stackPanel;
    private JTextField inputField;
    // Label to show error messages and status
    private JLabel messageLabel;
    // Shows what's at the top of the stack
    private JLabel topLabel;
    // Shows the final conversion result
    private JLabel resultLabel;
    // Shows the current expression being processed
    private JLabel expressionLabel;
    // Set of valid operators for the expressions
    private final Set<String> operators = new HashSet<>(Arrays.asList("+", "-", "*", "/", "^"));
    // Different types of brackets we support
    private final Set<String> brackets = new HashSet<>(Arrays.asList("(", ")", "[", "]", "{", "}"));
    // Operator precedence for infix conversions - higher number = higher precedence
    private final Map<String, Integer> precedence = new HashMap<>();
    // Array to hold the tokens of the input expression
    private String[] tokens;
    // Keeps track of which token we're currently processing
    private int currentTokenIndex;
    private JButton nextStepButton;
    private JButton autoConvertButton;
    // Whether we're converting to infix notation (true) or not (false)
    private boolean isInfixMode;
    // Whether the input is in postfix notation
    private boolean isPostfixInput;
    // Whether the input is in infix notation
    private boolean isInfixInput;
    private JComboBox<String> conversionModeCombo;
    // Panel to show notifications and step-by-step explanations
    private JPanel notificationPanel;
    private JTextPane notificationArea;
    // Shows the remaining expression to be processed
    private JLabel currentExpressionLabel;
    // Arrow pointing to the next token to process
    private JLabel nextOperationArrow;
    private JPanel expressionArrowPanel;
    // Flag to prevent button clicks during animations
    private boolean isAnimating = false;
    private StringBuilder notificationContent = new StringBuilder("<html><body>");
    private JButton examplesButton;
    // Added slider for animation speed control
    private JSlider animationSpeedSlider;
    // History of expressions and their results
    private ArrayList<ExpressionHistoryRecord> expressionHistory = new ArrayList<>();
    private JButton historyButton;
    private JPanel expressionBuilderPanel;
    private JButton infoButton;
    private final String[][] sampleExpressions = {
            {"(A + B) * (C - D)", "AB+CD-*", "*+AB-CD"},          // Expression 1 - correct
            {"A * B + C / D", "AB*CD/+", "+*AB/CD"},              // Expression 2 - correct
            {"A + B * (C ^ D - E)", "ABCD^E-*+", "+A*B^CD-E"},    // Expression 3 - corrected
            {"(A - B / C) * (D ^ E + F)", "ABC/-DE^F+*", "*-A/BC+^DEF"}, // Expression 4 - correct
            {"A + B * C - D", "ABC*+D-", "-+A*BCD"},              // Expression 5 - correct
            {"A + B * C / D", "ABC*D/+", "+A*B/CD"},              // Expression 6 - correct
            {"A - B * C + D", "ABC*-D+", "+-A*BCD"},              // Expression 7 - correct
            {"A + B * C - D / E", "ABC*+DE/-", "-+A*BC/DE"},      // Expression 8 - correct
            {"A + B * C - D", "ABC*+D-", "-+A*BCD"},              // Expression 9 - correct
            {"A + B * C ^ D - E", "ABCD^*+E-", "-+A*B^CDE"},      // Expression 10 - correct
            {"(A - B) / C * (D + E)", "AB-C/DE+*", "*/-ABC+DE"},  // Expression 11 - correct
            {"A + B * C ^ D", "ABCD^*+", "+A*B^CD"}               // Expression 12 - correct
    };
    // Default background color
    private Color backgroundColor = new Color(245, 245, 245);

    public StackExpConViz() {
        stack = new ArrayList<>();
        setTitle("Expression Converter Visualizer");
        setSize(1200, 800); // Increased window size for better visibility
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15)); // Increased spacing between components

        // Set up the operator precedence levels
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        precedence.put("^", 3);

        // Set a modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set background color for the main frame
        setBackground(backgroundColor);

        createInputPanel();
        createNotificationPanel();
        createStackPanel();
        createInfoPanel();
        createControlPanel();

        setLocationRelativeTo(null);

        // Update window title based on selected mode
        updateTitle();
    }

    private void createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(backgroundColor);

        // Top panel for controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        controlsPanel.setBackground(backgroundColor);

        String[] modes = {
                "Prefix to Postfix",
                "Prefix to Infix",
                "Postfix to Prefix",
                "Postfix to Infix",
                "Infix to Postfix",
                "Infix to Prefix",
                "String Reversal",
                "Bracket Balancing"
        };
        conversionModeCombo = new JComboBox<>(modes);
        conversionModeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        conversionModeCombo.setPreferredSize(new Dimension(200, 35));
        conversionModeCombo.setBackground(Color.WHITE);
        conversionModeCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Add mode change listener to update window title and expression builder
        conversionModeCombo.addActionListener(e -> {
            updateTitle();
            updateExpressionBuilder();
        });

        JLabel instructionLabel = new JLabel("Enter Expression:");
        instructionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        instructionLabel.setForeground(new Color(51, 51, 51));

        inputField = new JTextField(30);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setPreferredSize(new Dimension(300, 35));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Add Enter key listener to input field
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startConversion();
                }
            }
        });

        // Add document listener for real-time validation
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                validateExpressionInput();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                validateExpressionInput();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                validateExpressionInput();
            }
        });

        // Create new buttons with dark blue styling
        JButton startButton = new JButton("Start Conversion");
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(140, 35));
        startButton.setBackground(new Color(25, 55, 105)); // Dark navy blue
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.setOpaque(true);
        startButton.setContentAreaFilled(true);
        startButton.setBorderPainted(true);
        startButton.setBorder(BorderFactory.createLineBorder(new Color(15, 35, 70), 2));
        startButton.addActionListener(e -> startConversion());

        // Add keyboard shortcut and tooltip
        startButton.setMnemonic(KeyEvent.VK_S);
        startButton.setToolTipText("Start conversion (Alt+S or Ctrl+Enter)");

        // Add hover effect for start button
        addButtonHoverEffect(startButton);

        JButton resetButton = new JButton("Reset");
        resetButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        resetButton.setPreferredSize(new Dimension(100, 35));
        resetButton.setBackground(new Color(25, 55, 105)); // Dark navy blue
        resetButton.setForeground(Color.BLACK);
        resetButton.setFocusPainted(false);
        resetButton.setOpaque(true);
        resetButton.setContentAreaFilled(true);
        resetButton.setBorderPainted(true);
        resetButton.setBorder(BorderFactory.createLineBorder(new Color(15, 35, 70), 2));
        resetButton.addActionListener(e -> resetOperation());

        // Add keyboard shortcut and tooltip
        resetButton.setMnemonic(KeyEvent.VK_R);
        resetButton.setToolTipText("Reset all (Alt+R or Ctrl+R)");

        // Add hover effect for reset button
        addButtonHoverEffect(resetButton);

        // Create an examples button
        examplesButton = new JButton("Examples");
        examplesButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        examplesButton.setPreferredSize(new Dimension(100, 35));
        examplesButton.setBackground(new Color(25, 55, 105));
        examplesButton.setForeground(Color.BLACK);
        examplesButton.setFocusPainted(false);
        examplesButton.setOpaque(true);
        examplesButton.setContentAreaFilled(true);
        examplesButton.setBorderPainted(true);
        examplesButton.setBorder(BorderFactory.createLineBorder(new Color(15, 35, 70), 2));
        examplesButton.addActionListener(e -> showExamplesMenu());

        // Add keyboard shortcut and tooltip
        examplesButton.setMnemonic(KeyEvent.VK_E);
        examplesButton.setToolTipText("Show sample expressions (Alt+E)");

        // Add hover effect for examples button
        addButtonHoverEffect(examplesButton);

        // Create a history button
        historyButton = new JButton("History");
        historyButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyButton.setPreferredSize(new Dimension(100, 35));
        historyButton.setBackground(new Color(25, 55, 105));
        historyButton.setForeground(Color.BLACK);
        historyButton.setFocusPainted(false);
        historyButton.setOpaque(true);
        historyButton.setContentAreaFilled(true);
        historyButton.setBorderPainted(true);
        historyButton.setBorder(BorderFactory.createLineBorder(new Color(15, 35, 70), 2));
        historyButton.addActionListener(e -> showHistoryMenu());

        // Add keyboard shortcut and tooltip
        historyButton.setMnemonic(KeyEvent.VK_H);
        historyButton.setToolTipText("Show expression history (Alt+H)");

        // Add hover effect for history button
        addButtonHoverEffect(historyButton);

        // Info button for educational content
        infoButton = new JButton("Info");
        infoButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoButton.setPreferredSize(new Dimension(80, 35));
        infoButton.setBackground(new Color(34, 139, 34)); // Forest green
        infoButton.setForeground(Color.BLACK);
        infoButton.setFocusPainted(false);
        infoButton.setOpaque(true);
        infoButton.setContentAreaFilled(true);
        infoButton.setBorderPainted(true);
        infoButton.setBorder(BorderFactory.createLineBorder(new Color(0, 100, 0), 2)); // Darker green
        infoButton.addActionListener(e -> showEducationalInfo());

        // Add keyboard shortcut and tooltip
        infoButton.setMnemonic(KeyEvent.VK_I);
        infoButton.setToolTipText("Show educational information about notations (Alt+I)");

        // Add hover effect
        addButtonHoverEffect(infoButton, new Color(0, 100, 0), new Color(34, 139, 34));

        controlsPanel.add(conversionModeCombo);
        controlsPanel.add(Box.createHorizontalStrut(20));
        controlsPanel.add(instructionLabel);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(inputField);
        controlsPanel.add(Box.createHorizontalStrut(20));
        controlsPanel.add(startButton);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(resetButton);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(examplesButton);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(historyButton);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(infoButton);

        // Expression arrow panel
        expressionArrowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        expressionArrowPanel.setVisible(false);
        expressionArrowPanel.setBackground(backgroundColor);

        currentExpressionLabel = new JLabel();
        currentExpressionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        currentExpressionLabel.setForeground(new Color(51, 51, 51));

        nextOperationArrow = new JLabel("↑");
        nextOperationArrow.setFont(new Font("Segoe UI", Font.BOLD, 24));
        nextOperationArrow.setForeground(new Color(46, 139, 87));

        JPanel arrowTextPanel = new JPanel(new BorderLayout());
        arrowTextPanel.setOpaque(false);
        arrowTextPanel.add(nextOperationArrow, BorderLayout.CENTER);

        expressionArrowPanel.add(currentExpressionLabel);
        expressionArrowPanel.add(arrowTextPanel);

        inputPanel.add(controlsPanel);
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(expressionArrowPanel);

        // Create expression builder panel
        createExpressionBuilder();
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(expressionBuilderPanel);

        add(inputPanel, BorderLayout.NORTH);
    }

    // Create the expression builder panel with buttons for operators and operands
    private void createExpressionBuilder() {
        expressionBuilderPanel = new JPanel();
        expressionBuilderPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        expressionBuilderPanel.setBackground(backgroundColor);
        expressionBuilderPanel.setBorder(BorderFactory.createTitledBorder("Expression Builder"));

        // Initially populate with buttons appropriate for the selected mode
        updateExpressionBuilder();
    }

    // Update the expression builder based on the selected conversion mode
    private void updateExpressionBuilder() {
        if (expressionBuilderPanel == null) return;

        expressionBuilderPanel.removeAll();
        int modeIndex = conversionModeCombo.getSelectedIndex();

        // Add label explaining the builder
        JLabel builderLabel = new JLabel("Click buttons to build your expression:");
        builderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        expressionBuilderPanel.add(builderLabel);

        // Common variables for all modes
        JButton[] variableButtons = new JButton[6];
        for (int i = 0; i < variableButtons.length; i++) {
            char variable = (char)('A' + i);
            variableButtons[i] = createExpressionButton(String.valueOf(variable));
            variableButtons[i].setToolTipText("Variable '" + variable + "' (operand)");
            expressionBuilderPanel.add(variableButtons[i]);
        }

        expressionBuilderPanel.add(Box.createHorizontalStrut(10));

        // Operator buttons with tooltips showing precedence
        String[] operators = {"+", "-", "*", "/", "^"};
        String[] operatorNames = {"Addition", "Subtraction", "Multiplication", "Division", "Exponentiation"};
        int[] precedenceValues = {1, 1, 2, 2, 3};

        for (int i = 0; i < operators.length; i++) {
            JButton button = createExpressionButton(operators[i]);
            button.setForeground(new Color(220, 53, 69)); // Red for operators

            // Set educational tooltip with precedence information
            button.setToolTipText("<html>" + operatorNames[i] + " operator<br>" +
                    "Precedence: " + precedenceValues[i] +
                    (precedenceValues[i] == 3 ? " (highest)" :
                            precedenceValues[i] == 1 ? " (lowest)" : "") + "</html>");

            expressionBuilderPanel.add(button);
        }

        // Add special tokens based on the selected mode
        if (modeIndex == 4 || modeIndex == 5) {  // Infix modes
            expressionBuilderPanel.add(Box.createHorizontalStrut(10));

            // Add brackets for infix expressions with tooltips
            JButton openBracket = createExpressionButton("(");
            openBracket.setToolTipText("Opening bracket - groups expressions and overrides precedence");

            JButton closeBracket = createExpressionButton(")");
            closeBracket.setToolTipText("Closing bracket - closes a grouped expression");

            expressionBuilderPanel.add(openBracket);
            expressionBuilderPanel.add(closeBracket);
        }

        // Add space button and backspace
        expressionBuilderPanel.add(Box.createHorizontalStrut(10));

        JButton spaceButton = new JButton("Space");
        spaceButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        spaceButton.addActionListener(e -> inputField.setText(inputField.getText() + " "));
        expressionBuilderPanel.add(spaceButton);

        JButton backspaceButton = new JButton("Backspace");
        backspaceButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backspaceButton.addActionListener(e -> {
            String text = inputField.getText();
            if (!text.isEmpty()) {
                inputField.setText(text.substring(0, text.length() - 1));
            }
        });
        expressionBuilderPanel.add(backspaceButton);

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearButton.addActionListener(e -> inputField.setText(""));
        expressionBuilderPanel.add(clearButton);

        expressionBuilderPanel.revalidate();
        expressionBuilderPanel.repaint();
    }

    // Helper method to create a button for the expression builder
    private JButton createExpressionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(45, 35));
        button.setMargin(new Insets(2, 2, 2, 2));

        // Add action to insert the text into the input field
        button.addActionListener(e -> {
            inputField.setText(inputField.getText() + text);
            inputField.requestFocus();
        });

        return button;
    }

    private void createStackPanel() {
        stackPanel = new JPanel();
        stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.Y_AXIS));
        stackPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Stack Visualization"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        stackPanel.setBackground(Color.WHITE);

        // Create a fixed-size panel to contain the stack - increased height for more elements
        JPanel fixedSizePanel = new JPanel();
        fixedSizePanel.setPreferredSize(new Dimension(450, 700)); // Increased height from 550 to 700
        fixedSizePanel.setLayout(new BorderLayout());
        fixedSizePanel.setBackground(Color.WHITE);

        // Create a panel that will align stack elements to the bottom
        JPanel stackAlignPanel = new JPanel();
        stackAlignPanel.setLayout(new BoxLayout(stackAlignPanel, BoxLayout.Y_AXIS));
        stackAlignPanel.add(Box.createVerticalGlue()); // This pushes everything to the bottom
        stackAlignPanel.add(stackPanel);
        stackAlignPanel.setBackground(Color.WHITE);

        fixedSizePanel.add(stackAlignPanel, BorderLayout.CENTER);

        // Create wrapper panel for the entire content
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.Y_AXIS));
        wrapperPanel.setBackground(backgroundColor);

        // Add extra space at the top to move the stack lower - increased to 150 pixels
        wrapperPanel.add(Box.createVerticalStrut(150));

        JPanel stackWrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        stackWrapperPanel.add(fixedSizePanel);
        wrapperPanel.add(stackWrapperPanel);

        // Add credit label with improved styling
        JLabel creditLabel = new JLabel("Made by Abdullah Irshad ©");
        creditLabel.setForeground(new Color(46, 139, 87));
        creditLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JPanel creditPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        creditPanel.setBackground(backgroundColor);
        creditPanel.add(creditLabel);
        wrapperPanel.add(creditPanel);

        add(wrapperPanel, BorderLayout.CENTER);
    }

    private void createInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Conversion Information"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        infoPanel.setBackground(Color.WHITE);

        expressionLabel = new JLabel("Current Expression: ");
        topLabel = new JLabel("Top of Stack: ");
        resultLabel = new JLabel("Final Result: ");
        messageLabel = new JLabel("");
        messageLabel.setForeground(new Color(220, 53, 69)); // Bootstrap danger red

        // Style all labels
        for (JLabel label : new JLabel[]{expressionLabel, topLabel, resultLabel, messageLabel}) {
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setForeground(new Color(51, 51, 51));
        }

        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(expressionLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(topLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(resultLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(messageLabel);

        add(infoPanel, BorderLayout.EAST);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(backgroundColor);

        // Button panel for Next Step and Auto Convert buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(backgroundColor);

        nextStepButton = new JButton("Next Step");
        autoConvertButton = new JButton("Auto Convert");

        styleButton(nextStepButton);
        styleButton(autoConvertButton);

        // Add keyboard shortcuts
        nextStepButton.setMnemonic(KeyEvent.VK_N); // Alt+N
        autoConvertButton.setMnemonic(KeyEvent.VK_A); // Alt+A

        // Add tooltips with shortcut info
        nextStepButton.setToolTipText("Process next token (Alt+N)");
        autoConvertButton.setToolTipText("Automatically convert expression (Alt+A)");

        nextStepButton.addActionListener(e -> processNextStep());
        autoConvertButton.addActionListener(e -> autoConvert());

        nextStepButton.setEnabled(false);
        autoConvertButton.setEnabled(false);

        buttonPanel.add(nextStepButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(autoConvertButton);

        // Animation speed slider
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        sliderPanel.setBackground(backgroundColor);

        JLabel sliderLabel = new JLabel("Animation Speed:");
        sliderLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sliderLabel.setForeground(new Color(51, 51, 51));

        animationSpeedSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 5);
        animationSpeedSlider.setPreferredSize(new Dimension(200, 40));
        animationSpeedSlider.setBackground(backgroundColor);
        animationSpeedSlider.setMajorTickSpacing(1);
        animationSpeedSlider.setPaintTicks(true);
        animationSpeedSlider.setPaintLabels(true);
        animationSpeedSlider.setSnapToTicks(true);

        // Create a label table for the slider
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(1, new JLabel("Slow"));
        labelTable.put(5, new JLabel("Medium"));
        labelTable.put(10, new JLabel("Fast"));
        animationSpeedSlider.setLabelTable(labelTable);

        sliderPanel.add(sliderLabel);
        sliderPanel.add(animationSpeedSlider);

        // Add panels to the control panel
        controlPanel.add(buttonPanel);
        controlPanel.add(sliderPanel);

        add(controlPanel, BorderLayout.SOUTH);

        // Add global keyboard shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            // Only process when key is released
            if (e.getID() == KeyEvent.KEY_RELEASED) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) {
                    resetOperation();
                    return true;
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startConversion();
                    return true;
                }
            }
            return false;
        });
    }

    private void createNotificationPanel() {
        notificationPanel = new JPanel();
        notificationPanel.setLayout(new BorderLayout());
        notificationPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Operation Steps"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        notificationPanel.setPreferredSize(new Dimension(350, 200));
        notificationPanel.setBackground(Color.WHITE);

        notificationArea = new JTextPane();
        notificationArea.setContentType("text/html");
        notificationArea.setEditable(false);
        notificationArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        notificationArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        notificationArea.setBackground(new Color(248, 248, 248));
        notificationArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JScrollPane scrollPane = new JScrollPane(notificationArea);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        notificationPanel.add(scrollPane, BorderLayout.CENTER);

        add(notificationPanel, BorderLayout.WEST);
    }

    private String[] tokenizeExpression(String input) {
        // If the input is infix, we need special handling for brackets and operators
        if (isInfixInput) {
            return tokenizeInfixExpression(input);
        }

        // First, remove all brackets from the input for prefix/postfix expressions
        String cleanedInput = input;
        for (String bracket : brackets) {
            cleanedInput = cleanedInput.replace(bracket, "");
        }

        // Now tokenize the cleaned input
        ArrayList<String> tokenList = new ArrayList<>();

        // For prefix and postfix inputs, split by whitespace first if it contains spaces
        if (cleanedInput.contains(" ")) {
            String[] spaceSplit = cleanedInput.trim().split("\\s+");
            for (String token : spaceSplit) {
                if (!token.isEmpty()) {
                    tokenList.add(token);
                }
            }
        } else {
            // If no spaces, tokenize character by character (for inputs like "+AB" or "AB+")
            for (int i = 0; i < cleanedInput.length(); i++) {
                char c = cleanedInput.charAt(i);

                // Skip spaces
                if (Character.isWhitespace(c)) {
                    continue;
                }

                String token = String.valueOf(c);

                if (operators.contains(token)) {
                    // If it's an operator, add it as a separate token
                    tokenList.add(token);
                } else {
                    // Otherwise it's an operand (like A, B, etc.)
                    tokenList.add(token);
                }
            }
        }

        // Print the tokens for debugging - helps me see what's happening
        StringBuilder debugMsg = new StringBuilder("Tokens: ");
        for (String token : tokenList) {
            debugMsg.append(token).append(" ");
        }
        System.out.println(debugMsg.toString());

        return tokenList.toArray(new String[0]);
    }

    private String[] tokenizeInfixExpression(String input) {
        ArrayList<String> tokenList = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();

        System.out.println("Debug: Starting to tokenize infix expression: " + input);

        // For infix input, we need to handle multi-character tokens
        // and keep track of brackets and operators
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String charStr = String.valueOf(c);

            System.out.println("Debug: Processing character: " + charStr);

            // If it's a space, finish the current token and skip the space
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    tokenList.add(currentToken.toString());
                    System.out.println("Debug: Added token from space: " + currentToken.toString());
                    currentToken.setLength(0);
                }
                continue;
            }

            // Special handling for operators and brackets
            if (operators.contains(charStr) || brackets.contains(charStr)) {
                // If we have a pending token, add it first
                if (currentToken.length() > 0) {
                    tokenList.add(currentToken.toString());
                    System.out.println("Debug: Added pending token: " + currentToken.toString());
                    currentToken.setLength(0);
                }
                // Add the operator or bracket as its own token
                tokenList.add(charStr);
                System.out.println("Debug: Added operator/bracket: " + charStr);
            } else {
                // For operands (like variables or numbers), keep building the token
                currentToken.append(c);
            }
        }

        // Add any remaining token (in case there's no space at the end)
        if (currentToken.length() > 0) {
            tokenList.add(currentToken.toString());
            System.out.println("Debug: Added final token: " + currentToken.toString());
        }

        // Print tokens for debugging
        StringBuilder debugMsg = new StringBuilder("Debug: Final tokens: ");
        for (String token : tokenList) {
            debugMsg.append(token).append(" ");
        }
        System.out.println(debugMsg.toString());

        return tokenList.toArray(new String[0]);
    }

    private boolean isValidExpression(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return false;
        }

        // Call the appropriate validation method based on the input type
        if (isInfixInput) {
            return isValidInfixExpression(tokens);
        } else if (isPostfixInput) {
            return isValidPostfixExpression(tokens);
        } else {
            return isValidPrefixExpression(tokens);
        }
    }

    private boolean isValidInfixExpression(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            System.out.println("Debug: Tokens array is null or empty");
            return false;
        }

        System.out.println("Debug: Starting validation of infix expression");
        System.out.println("Debug: Tokens to validate:");
        for (String token : tokens) {
            System.out.print(token + " ");
        }
        System.out.println();

        // First, check if all brackets are balanced
        Stack<String> bracketStack = new Stack<>();
        for (String token : tokens) {
            if (token.equals("(") || token.equals("[") || token.equals("{")) {
                // Push opening brackets onto stack
                bracketStack.push(token);
                System.out.println("Debug: Pushed opening bracket: " + token);
            } else if (token.equals(")") || token.equals("]") || token.equals("}")) {
                // For closing brackets, check if they match with the last opening bracket
                if (bracketStack.isEmpty()) {
                    System.out.println("Debug: Invalid - extra closing bracket: " + token);
                    return false;
                }

                String openBracket = bracketStack.pop();
                if ((token.equals(")") && !openBracket.equals("(")) ||
                        (token.equals("]") && !openBracket.equals("[")) ||
                        (token.equals("}") && !openBracket.equals("{"))) {
                    System.out.println("Debug: Invalid - mismatched brackets: " + openBracket + " and " + token);
                    return false;
                }
                System.out.println("Debug: Matched brackets: " + openBracket + " and " + token);
            }
        }

        // If there are still brackets on the stack, we're missing closing brackets
        if (!bracketStack.isEmpty()) {
            System.out.println("Debug: Invalid - missing closing brackets");
            return false;
        }

        // Now check for proper operator-operand sequence
        boolean expectingOperand = true; // Start expecting an operand
        int openBrackets = 0;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            System.out.println("Debug: Processing token " + i + ": " + token + ", expectingOperand=" + expectingOperand);

            if (brackets.contains(token)) {
                if (token.equals("(") || token.equals("[") || token.equals("{")) {
                    openBrackets++;
                    expectingOperand = true; // After an open bracket, expect an operand
                    System.out.println("Debug: Found opening bracket, now expecting operand");
                } else {
                    openBrackets--;
                    expectingOperand = false; // After a closing bracket, expect an operator
                    System.out.println("Debug: Found closing bracket, now expecting operator");
                }
            } else if (operators.contains(token)) {
                if (expectingOperand) {
                    // If we expect an operand but got an operator, it's invalid
                    System.out.println("Debug: Invalid - expected operand but got operator: " + token);
                    return false;
                }
                expectingOperand = true; // After an operator, expect an operand
                System.out.println("Debug: Found operator, now expecting operand");
            } else {
                // This is an operand
                if (!expectingOperand && i > 0) {
                    // If we expect an operator but got an operand, it's invalid
                    System.out.println("Debug: Invalid - expected operator but got operand: " + token);
                    return false;
                }
                expectingOperand = false; // After an operand, expect an operator
                System.out.println("Debug: Found operand, now expecting operator");
            }
        }

        // The expression should end with an operand or closing bracket
        if (expectingOperand) {
            System.out.println("Debug: Invalid - expression ends with an operator");
            return false;
        }

        // Ensure all brackets are balanced
        if (openBrackets != 0) {
            System.out.println("Debug: Invalid - unbalanced brackets (openBrackets=" + openBrackets + ")");
            return false;
        }

        System.out.println("Debug: Expression is valid!");
        return true; // If we passed all checks, the expression is valid
    }

    private boolean isValidPostfixExpression(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return false;
        }

        // Special case: simple expressions like "AB+" or "A B +" (two operands, one operator)
        if (tokens.length == 3 && !isOperator(tokens[0]) && !isOperator(tokens[1]) && isOperator(tokens[2])) {
            return true;
        }

        // For valid postfix expressions, we need:
        // 1. At least one operator
        // 2. The number of operands = number of operators + 1
        int operatorCount = 0;
        int operandCount = 0;

        for (String token : tokens) {
            if (isOperator(token)) {
                operatorCount++;
            } else {
                operandCount++;
            }
        }

        // Check the operand-operator balance
        if (operandCount != operatorCount + 1) {
            System.out.println("Invalid: operands=" + operandCount + ", operators=" + operatorCount);
            return false;
        }

        // Now simulate evaluating the expression with a stack
        // This checks that operators have enough operands to work with
        ArrayList<String> validationStack = new ArrayList<>();

        for (String token : tokens) {
            if (isOperator(token)) {
                // Each operator needs two operands
                if (validationStack.size() < 2) {
                    System.out.println("Invalid structure: not enough operands for operator " + token);
                    return false;
                }

                // Pop two operands and push a dummy result
                validationStack.remove(validationStack.size() - 1);
                validationStack.remove(validationStack.size() - 1);
                validationStack.add("result"); // Just a placeholder
            } else {
                // Push operand onto stack
                validationStack.add(token);
            }
        }

        // A valid postfix expression should leave exactly one result on the stack
        return validationStack.size() == 1;
    }

    private boolean isValidPrefixExpression(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return false;
        }

        // Special case: unary operator (e.g., "-A")
        if (tokens.length == 2 && isOperator(tokens[0]) && !isOperator(tokens[1])) {
            return true;
        }

        // Special case: one operator and two operands (e.g., "+AB" or "+ A B")
        if (tokens.length == 3 && isOperator(tokens[0]) && !isOperator(tokens[1]) && !isOperator(tokens[2])) {
            return true;
        }

        // Similar to postfix, check operand-operator balance
        int operatorCount = 0;
        int operandCount = 0;

        for (String token : tokens) {
            if (isOperator(token)) {
                operatorCount++;
            } else {
                operandCount++;
            }
        }

        // Same rule: operands = operators + 1
        if (operandCount != operatorCount + 1) {
            System.out.println("Invalid: operands=" + operandCount + ", operators=" + operatorCount);
            return false;
        }

        // For prefix expressions, we evaluate from right to left
        // So we need to do the validation differently
        int count = 0;

        // Traverse from right to left for prefix expression
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i];
            if (isOperator(token)) {
                // For each operator, we need two operands
                count--;
            } else {
                // For each operand, increment count
                count++;
            }

            // If count ever goes below 1, we have invalid expression
            // (except at the very end when processing the first operator)
            if (count < 1 && i > 0) {
                System.out.println("Invalid structure at position " + i);
                return false;
            }
        }

        // Final count should be 1 for a valid expression (one final result)
        return count == 1;
    }

    private String[] convertInfixToPostfix(String[] infixTokens) {
        System.out.println("Debug: Starting infix to postfix conversion for: " + Arrays.toString(infixTokens));

        ArrayList<String> postfix = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : infixTokens) {
            System.out.println("Debug: Processing token: " + token);

            if (isOperator(token)) {
                while (!stack.isEmpty() && !isOpenBracket(stack.peek()) &&
                        precedence.getOrDefault(stack.peek(), 0) >= precedence.getOrDefault(token, 0)) {
                    String popped = stack.pop();
                    postfix.add(popped);
                    System.out.println("Debug: Popped operator with higher/equal precedence: " + popped);
                }
                stack.push(token);
                System.out.println("Debug: Pushed operator to stack: " + token);
            }
            else if (isOpenBracket(token)) {
                stack.push(token);
                System.out.println("Debug: Pushed open bracket to stack: " + token);
            }
            else if (isCloseBracket(token)) {
                while (!stack.isEmpty() && !isOpenBracket(stack.peek())) {
                    String popped = stack.pop();
                    postfix.add(popped);
                    System.out.println("Debug: Popped operator until matching bracket: " + popped);
                }
                if (!stack.isEmpty() && isOpenBracket(stack.peek())) {
                    stack.pop(); // Discard the open bracket
                    System.out.println("Debug: Discarded opening bracket");
                }
            }
            else {
                // Operand
                postfix.add(token);
                System.out.println("Debug: Added operand to postfix: " + token);
            }

            System.out.println("Debug: Current postfix: " + postfix);
            System.out.println("Debug: Current stack: " + stack);
        }

        // Pop any remaining operators from the stack
        while (!stack.isEmpty()) {
            if (isOpenBracket(stack.peek())) {
                String discarded = stack.pop(); // Discard any remaining open brackets
                System.out.println("Debug: Discarded unclosed bracket: " + discarded);
            } else {
                String popped = stack.pop();
                postfix.add(popped);
                System.out.println("Debug: Popped remaining operator: " + popped);
            }
        }

        System.out.println("Debug: Final postfix expression: " + postfix);

        // For postfix, we just need to combine the tokens to a single array
        String[] result = postfix.toArray(new String[0]);

        // Individual postfix tokens
        ArrayList<String> postfixTokens = new ArrayList<>();
        for (String token : result) {
            // If it's a multi-character token, we need to split it
            // But for our purposes, we can just add single characters
            if (token.length() > 1) {
                for (int i = 0; i < token.length(); i++) {
                    postfixTokens.add(String.valueOf(token.charAt(i)));
                }
            } else {
                postfixTokens.add(token);
            }
        }

        System.out.println("Debug: Final postfix tokens: " + postfixTokens);

        return postfixTokens.toArray(new String[0]);
    }

    private String[] convertInfixToPrefix(String[] infixTokens) {
        System.out.println("Debug: Starting infix to prefix conversion for: " + Arrays.toString(infixTokens));

        // APPROACH: Convert infix to postfix first, then reverse the result
        // First, let's create a copy of the infixTokens array
        ArrayList<String> strippedInfixTokens = new ArrayList<>();

        // Copy and filter out brackets first for clearer debug output
        for (String token : infixTokens) {
            strippedInfixTokens.add(token);
        }

        System.out.println("Debug: Initial infix tokens: " + strippedInfixTokens);

        // 1. Convert infix to postfix
        ArrayList<String> postfix = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : infixTokens) {
            System.out.println("Debug: Processing token for postfix conversion: " + token);

            if (isOperator(token)) {
                while (!stack.isEmpty() && !isOpenBracket(stack.peek()) &&
                        precedence.getOrDefault(stack.peek(), 0) >= precedence.getOrDefault(token, 0)) {
                    postfix.add(stack.pop());
                }
                stack.push(token);
                System.out.println("Debug: Pushed operator to stack: " + token);
            }
            else if (isOpenBracket(token)) {
                stack.push(token);
                System.out.println("Debug: Pushed open bracket to stack: " + token);
            }
            else if (isCloseBracket(token)) {
                while (!stack.isEmpty() && !isOpenBracket(stack.peek())) {
                    postfix.add(stack.pop());
                }
                if (!stack.isEmpty() && isOpenBracket(stack.peek())) {
                    stack.pop(); // Remove the open bracket
                }
                System.out.println("Debug: Processed closing bracket");
            }
            else {
                // Operand
                postfix.add(token);
                System.out.println("Debug: Added operand to postfix: " + token);
            }

            System.out.println("Debug: Current postfix: " + postfix);
            System.out.println("Debug: Current stack: " + stack);
        }

        // Pop any remaining operators from the stack
        while (!stack.isEmpty()) {
            if (isOpenBracket(stack.peek())) {
                stack.pop(); // Discard any remaining open brackets
            } else {
                postfix.add(stack.pop());
            }
        }

        System.out.println("Debug: Final postfix: " + postfix);

        // 2. Convert postfix to prefix
        Stack<String> conversionStack = new Stack<>();
        for (String token : postfix) {
            if (isOperator(token)) {
                // For operators, pop two operands
                if (conversionStack.size() < 2) {
                    System.out.println("Debug: Error - not enough operands for operator " + token);
                    return new String[]{"Error"};
                }
                String operand2 = conversionStack.pop();
                String operand1 = conversionStack.pop();

                // In prefix, the operator comes first, followed by operands
                String prefixExpr = token + operand1 + operand2;
                conversionStack.push(prefixExpr);
                System.out.println("Debug: Created prefix expression: " + prefixExpr);
            } else {
                // For operands, just push to stack
                conversionStack.push(token);
                System.out.println("Debug: Pushed operand to conversion stack: " + token);
            }

            System.out.println("Debug: Current conversion stack: " + conversionStack);
        }

        // The final result should be on top of the stack
        if (conversionStack.isEmpty()) {
            System.out.println("Debug: Error - empty conversion stack");
            return new String[]{"Error"};
        }

        String prefixExpression = conversionStack.pop();
        System.out.println("Debug: Final prefix expression: " + prefixExpression);

        // Convert the single prefix expression string into individual tokens
        ArrayList<String> prefixTokens = new ArrayList<>();
        for (int i = 0; i < prefixExpression.length(); i++) {
            prefixTokens.add(String.valueOf(prefixExpression.charAt(i)));
        }

        System.out.println("Debug: Final prefix tokens: " + prefixTokens);

        return prefixTokens.toArray(new String[0]);
    }

    // Helper method for converting infix to postfix (used by convertInfixToPostfix)
    private String[] convertToPostfix(String[] infixTokens) {
        ArrayList<String> postfixOutput = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        // Loop through each token in the infix expression
        for (String token : infixTokens) {
            if (isOperator(token)) {
                // For operators, we need to handle precedence
                // Pop operators with higher/equal precedence before pushing this one
                while (!operatorStack.isEmpty() &&
                        !isOpenBracket(operatorStack.peek()) &&
                        hasHigherPrecedence(operatorStack.peek(), token)) {
                    postfixOutput.add(operatorStack.pop());
                }
                operatorStack.push(token);
            } else if (isOpenBracket(token)) {
                // For open brackets, just push to stack
                operatorStack.push(token);
            } else if (isCloseBracket(token)) {
                // For closing brackets, pop operators until matching open bracket
                while (!operatorStack.isEmpty() && !isOpenBracket(operatorStack.peek())) {
                    postfixOutput.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty() && isOpenBracket(operatorStack.peek())) {
                    operatorStack.pop(); // Discard the open bracket
                }
            } else {
                // For operands, add directly to output
                postfixOutput.add(token);
            }
        }

        // Pop any remaining operators from the stack
        while (!operatorStack.isEmpty()) {
            postfixOutput.add(operatorStack.pop());
        }

        return postfixOutput.toArray(new String[0]);
    }

    // Helper method to check if a token is an opening bracket
    private boolean isOpenBracket(String token) {
        return token.equals("(") || token.equals("[") || token.equals("{");
    }

    // Helper method to check if a token is a closing bracket
    private boolean isCloseBracket(String token) {
        return token.equals(")") || token.equals("]") || token.equals("}");
    }

    private boolean hasHigherPrecedence(String op1, String op2) {
        // Use >= for left-associative operators (standard behavior for most operators)
        // For expressions like a+b+c, we want (a+b)+c, not a+(b+c)
        int p1 = precedence.getOrDefault(op1, 0);
        int p2 = precedence.getOrDefault(op2, 0);
        System.out.println("Debug: Comparing precedence of " + op1 + "(" + p1 + ") and " + op2 + "(" + p2 + ")");

        return p1 >= p2;
    }

    private void startConversion() {
        // Clear everything first
        resetOperation();

        String input = inputField.getText().trim();
        if (input.isEmpty()) {
            showMessage("Please enter an expression!");
            return;
        }

        // Handle bracket balancing mode
        if (conversionModeCombo.getSelectedIndex() == 7) {
            // For bracket balancing, each character is a token
            tokens = new String[input.length()];
            for (int i = 0; i < input.length(); i++) {
                tokens[i] = String.valueOf(input.charAt(i));
            }
            currentTokenIndex = 0;
            isPostfixInput = true; // We'll process from left to right
            expressionArrowPanel.setVisible(true);
            updateExpressionAndArrow();
            nextStepButton.setEnabled(true);
            autoConvertButton.setEnabled(true);
            expressionLabel.setText("Ready to check bracket balance. Click 'Next Step' or 'Auto Convert'");
            return;
        }

        // Handle string reversal mode
        if (conversionModeCombo.getSelectedIndex() == 6) {
            // For string reversal, each character is a token
            tokens = new String[input.length()];
            for (int i = 0; i < input.length(); i++) {
                tokens[i] = String.valueOf(input.charAt(i));
            }
            currentTokenIndex = 0;
            isPostfixInput = true; // We'll process from left to right
            expressionArrowPanel.setVisible(true);
            updateExpressionAndArrow();
            nextStepButton.setEnabled(true);
            autoConvertButton.setEnabled(true);
            expressionLabel.setText("Ready to start string reversal. Click 'Next Step' or 'Auto Convert'");
            return;
        }

        // Set input type flags based on conversion mode
        int modeIndex = conversionModeCombo.getSelectedIndex();
        isInfixInput = modeIndex == 4 || modeIndex == 5; // Infix to Prefix or Infix to Postfix
        isPostfixInput = modeIndex == 2 || modeIndex == 3; // Postfix to Prefix, Postfix to Infix
        // Infix to Postfix uses postfix processing (left to right), add it separately
        isPostfixInput = isPostfixInput || modeIndex == 5;
        isInfixMode = modeIndex == 1 || modeIndex == 3; // Prefix to Infix or Postfix to Infix

        System.out.println("Debug: Conversion mode index: " + modeIndex);
        System.out.println("Debug: isInfixInput: " + isInfixInput);
        System.out.println("Debug: isPostfixInput: " + isPostfixInput);
        System.out.println("Debug: isInfixMode: " + isInfixMode);

        // Split the input into tokens
        tokens = tokenizeExpression(input);

        // Handle infix inputs specially - they need conversion first
        if (isInfixInput) {
            System.out.println("Debug: Validating infix expression");
            // Validate the infix expression
            if (!isValidInfixExpression(tokens)) {
                showMessage("Invalid Infix Expression!");
                return;
            }

            // Figure out which conversion we're doing
            boolean toPrefix = modeIndex == 4; // Infix to Prefix
            boolean toPostfix = modeIndex == 5; // Infix to Postfix

            System.out.println("Debug: Converting infix to " + (toPrefix ? "prefix" : "postfix"));

            // Convert the infix to the target notation
            if (toPrefix) {
                tokens = convertInfixToPrefix(tokens);
                isPostfixInput = false; // Prefix notation is processed from right to left
            } else if (toPostfix) {
                tokens = convertInfixToPostfix(tokens);
                isPostfixInput = true; // Postfix notation is processed from left to right
            }

            // We've converted the infix input to either prefix or postfix
            // So now we need to update the flags
            isInfixInput = false;

            // Filter out any brackets from the result for cleaner processing
            ArrayList<String> filteredTokens = new ArrayList<>();
            for (String token : tokens) {
                if (!brackets.contains(token)) {
                    filteredTokens.add(token);
                }
            }
            tokens = filteredTokens.toArray(new String[0]);

        } else {
            // For direct prefix/postfix input
            // Make sure we have at least 2 tokens
            if (tokens.length < 2) {
                showMessage("Expression must have at least one operator and one operand!");
                return;
            }

            // Validate the expression
            if (!isValidExpression(tokens)) {
                showMessage("Invalid Expression!");
                return;
            }
        }

        // Set the starting index based on whether we're processing prefix or postfix
        if (isPostfixInput) {
            currentTokenIndex = 0; // Start from beginning for postfix
        } else {
            currentTokenIndex = tokens.length - 1; // Start from end for prefix
        }

        // Show the expression with the arrow pointing to the first token
        expressionArrowPanel.setVisible(true);
        updateExpressionAndArrow();
        nextStepButton.setEnabled(true);
        autoConvertButton.setEnabled(true);
        expressionLabel.setText("Ready to start conversion. Click 'Next Step' or 'Auto Convert'");
    }

    // Updates the display to show which token we're processing next
    private void updateExpressionAndArrow() {
        if (currentTokenIndex >= 0 && currentTokenIndex < tokens.length) {
            StringBuilder expr = new StringBuilder();

            if (isPostfixInput) {
                // For postfix, show tokens from current to end
                for (int i = currentTokenIndex; i < tokens.length; i++) {
                    expr.append(tokens[i]);
                    if (i < tokens.length - 1) expr.append(" ");
                }
                currentExpressionLabel.setText("Remaining: " + expr.toString());
            } else {
                // For prefix, show tokens from current to beginning
                // We need to show them in the order they will be processed
                ArrayList<String> remainingTokens = new ArrayList<>();
                for (int i = currentTokenIndex; i >= 0; i--) {
                    remainingTokens.add(tokens[i]);
                }
                // Reverse the list to show in processing order
                Collections.reverse(remainingTokens);
                for (int i = 0; i < remainingTokens.size(); i++) {
                    expr.append(remainingTokens.get(i));
                    if (i < remainingTokens.size() - 1) expr.append(" ");
                }
                currentExpressionLabel.setText("Remaining: " + expr.toString());
            }

            // Add a tooltip to the arrow to explain what's going to happen
            String nextToken = tokens[currentTokenIndex];
            String operation = isOperator(nextToken) ?
                    "Will process operator '" + nextToken + "'" :
                    "Will push '" + nextToken + "' to stack";
            nextOperationArrow.setToolTipText(operation);
        } else {
            // No more tokens to process
            expressionArrowPanel.setVisible(false);
        }
    }

    // Process the next token in the expression
    private void processNextStep() {
        if (isAnimating) {
            return; // Don't process steps if already animating
        }

        // Handle bracket balancing mode
        if (conversionModeCombo.getSelectedIndex() == 7) {
            if (currentTokenIndex >= tokens.length) {
                // All brackets processed - check if stack is empty
                if (stack.isEmpty()) {
                    showMessage("Expression is balanced!");
                    resultLabel.setText("Final Result: Expression is balanced");
                    disableControls();
                } else {
                    // Stack not empty means we have unclosed brackets
                    showMessage("Unbalanced: Missing closing brackets");
                    resultLabel.setText("Final Result: Expression is unbalanced");
                    disableControls();
                }
                return;
            }

            String token = tokens[currentTokenIndex];
            expressionLabel.setText("Processing character: " + token);

            if (isOpenBracket(token)) {
                // Push opening bracket to stack
                stack.add(token);
                updateStackVisual();
                addNotification("Pushed opening bracket: " + token);
                topLabel.setText("Top of Stack: " + token);
                currentTokenIndex++;
                updateExpressionAndArrow();
                return;
            } else if (isCloseBracket(token)) {
                if (stack.isEmpty()) {
                    showMessage("Unbalanced: Extra closing bracket '" + token + "'");
                    resultLabel.setText("Final Result: Expression is unbalanced");
                    disableControls();
                    return;
                }

                String topBracket = stack.get(stack.size() - 1);
                if ((token.equals(")") && topBracket.equals("(")) ||
                        (token.equals("]") && topBracket.equals("[")) ||
                        (token.equals("}") && topBracket.equals("{"))) {
                    // Matching brackets found - animate the matching process
                    animateBracketMatching(topBracket, token);
                    return;
                } else {
                    showMessage("Unbalanced: Mismatched brackets '" + topBracket + "' and '" + token + "'");
                    resultLabel.setText("Final Result: Expression is unbalanced");
                    disableControls();
                    return;
                }
            }

            currentTokenIndex++;
            updateExpressionAndArrow();
            return;
        }

        // Handle string reversal mode
        if (conversionModeCombo.getSelectedIndex() == 6) {
            if (currentTokenIndex >= tokens.length) {
                // All characters processed, now pop them to get reversed string
                if (stack.isEmpty()) {
                    showMessage("String Reversal Complete!");
                    disableControls();
                    return;
                }

                // Pop the top character and show it being removed
                String poppedChar = stack.remove(stack.size() - 1);
                updateStackVisual();
                addNotification("Popped character: " + poppedChar);

                // Build the reversed string gradually
                StringBuilder reversed = new StringBuilder();
                // Get the current reversed string from the result label
                String currentReversed = resultLabel.getText().replace("Reversed String: ", "");
                if (currentReversed.isEmpty()) {
                    reversed.append(poppedChar);
                } else {
                    reversed.append(currentReversed).append(poppedChar);
                }
                resultLabel.setText("Reversed String: " + reversed.toString());

                // Print the current state of the reversed string to console
                System.out.println("String Reversal: " + reversed.toString());
                return;
            }

            String token = tokens[currentTokenIndex];
            expressionLabel.setText("Processing character: " + token);

            // Push each character to stack
            stack.add(token);
            updateStackVisual();
            addNotification("Pushed character: " + token);
            topLabel.setText("Top of Stack: " + token);

            currentTokenIndex++;
            updateExpressionAndArrow();
            return;
        }

        // Check if we've processed all tokens
        if (isPostfixInput && currentTokenIndex >= tokens.length ||
                !isPostfixInput && currentTokenIndex < 0) {
            checkFinalResult();
            return;
        }

        String token = tokens[currentTokenIndex];
        expressionLabel.setText("Processing token: " + token);

        if (isOperator(token)) {
            // If it's an operator, we need to pop operands and do the conversion
            processOperator(token);
        } else {
            // If it's an operand, just push it to the stack
            pushToStack(token);
        }

        // Move to the next token
        if (isPostfixInput) {
            currentTokenIndex++;
        } else {
            currentTokenIndex--;
        }

        updateExpressionAndArrow();

        // Check if we're done processing
        if (isPostfixInput && currentTokenIndex >= tokens.length ||
                !isPostfixInput && currentTokenIndex < 0) {
            checkFinalResult();
        }
    }

    // Do the whole conversion automatically with animation
    private void autoConvert() {
        nextStepButton.setEnabled(false);
        autoConvertButton.setEnabled(false);

        // Calculate animation speed based on slider value (inverse relationship)
        // Slider: 1 (slow) = 2000ms, 10 (fast) = 200ms
        int sliderValue = animationSpeedSlider.getValue();
        int animationSpeed = 2200 - (sliderValue * 200); // 2000ms to 200ms

        // Create a timer that processes one token
        Timer timer = new Timer(animationSpeed, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean hasMoreTokens = isPostfixInput ?
                        currentTokenIndex < tokens.length :
                        currentTokenIndex >= 0;

                if (hasMoreTokens) {
                    if (!isAnimating) {
                        processNextStep();
                    }
                } else {
                    ((Timer)e.getSource()).stop();
                    cleanup();
                }
            }
        });
        timer.start();
    }

    // Clean up any leftover animation components
    private void cleanup() {
        JLayeredPane layeredPane = getLayeredPane();
        Component[] components = layeredPane.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel && comp.getBackground().equals(new Color(46, 139, 87))) {
                layeredPane.remove(comp);
            }
        }
        layeredPane.repaint();
    }

    // Show error messages or status messages temporarily
    private void showMessage(String message) {
        messageLabel.setText(message);
        // Set error messages to red, success messages to green
        if (message.contains("Invalid") || message.contains("Unbalanced") || message.contains("unbalanced") || message.contains("Error")) {
            messageLabel.setForeground(new Color(220, 53, 69)); // Red for errors
        } else if (message.contains("Complete") || message.contains("balanced")) {
            messageLabel.setForeground(new Color(40, 167, 69)); // Green for success
        }

        Timer timer = new Timer(3000, e -> messageLabel.setText(""));
        timer.setRepeats(false);
        timer.start();
    }

    // Check if we have a valid result at the end
    private void checkFinalResult() {
        if (stack.size() == 1) {
            // Success - we should have exactly one item on the stack
            String result = stack.get(0);
            resultLabel.setText("Final Result: " + result);
            resultLabel.setForeground(new Color(40, 167, 69)); // Green for success
            showMessage("Conversion Complete!");

            // Add to history
            String input = inputField.getText().trim();
            String mode = (String) conversionModeCombo.getSelectedItem();
            expressionHistory.add(new ExpressionHistoryRecord(input, result, mode));
        } else if (!isAnimating) {
            // Only show error if we're not in the middle of an animation
            resultLabel.setForeground(new Color(220, 53, 69)); // Red for errors
            showMessage("Invalid Expression!");
        }
        disableControls();
    }

    // Disable the step buttons when we're done
    private void disableControls() {
        nextStepButton.setEnabled(false);
        autoConvertButton.setEnabled(false);
    }

    // This is the hardest part - animating when we process an operator
    private void processOperator(String operator) {
        if (stack.size() < 2) {
            // We need at least 2 operands to apply an operator
            showMessage("Invalid Expression!");
            disableControls();
            return;
        }

        isAnimating = true;  // Set at start of operation
        nextStepButton.setEnabled(false);
        autoConvertButton.setEnabled(false);
        // Clear any error messages that might be displayed
        messageLabel.setText("");

        addNotification("Starting new operation with operator '" + operator + "'");
        String operand1 = stack.get(stack.size() - 1);
        String operand2 = stack.get(stack.size() - 2);

        // Create a Glass Pane to hold all animation elements
        JPanel glassPane = new JPanel(null); // null layout for absolute positioning
        glassPane.setOpaque(false);
        setGlassPane(glassPane);
        glassPane.setVisible(true);

        // Create animated labels to show the operands moving
        JLabel floatingLabel1 = createFloatingLabel(operand1);
        JLabel floatingLabel2 = createFloatingLabel(operand2);

        // Calculate positions for the animation
        Point stackLocation = stackPanel.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(stackLocation, glassPane);

        Point startPos = new Point(stackLocation.x + 50, stackLocation.y + 10);
        Point rightPos = new Point(startPos.x + 400, startPos.y);
        Point belowPos = new Point(rightPos.x, rightPos.y + 60);

        // Add labels to glass pane with correct starting positions
        floatingLabel1.setLocation(startPos);
        glassPane.add(floatingLabel1);

        floatingLabel2.setLocation(startPos);
        floatingLabel2.setVisible(false);  // Hide initially
        glassPane.add(floatingLabel2);

        // Access stack elements
        JPanel topElement = (JPanel)stackPanel.getComponent(0);

        // Animation timer for smoother sequencing
        final Timer[] sequence = new Timer[1];
        final int[] step = {0};
        final String[] resultValue = {""};

        sequence[0] = new Timer(50, e -> {
            step[0]++;

            // Step 1-20: Pop and move first operand
            if (step[0] == 1) {
                addNotification("Popping first operand: " + operand1);
                fadeOutElement(topElement, null);
            } else if (step[0] == 20) {
                stack.remove(stack.size() - 1);
                updateStackVisual();
            } else if (step[0] > 1 && step[0] <= 40) {
                // Animate first operand moving right
                float progress = (step[0] - 1) / 40.0f;
                int x = (int)(startPos.x + (rightPos.x - startPos.x) * progress);
                floatingLabel1.setLocation(x, startPos.y);
            }

            // Step 41-60: Pop and show second operand
            else if (step[0] == 41) {
                addNotification("Popping second operand: " + operand2);
                floatingLabel2.setVisible(true);
                if (stackPanel.getComponentCount() > 0) {
                    fadeOutElement((JPanel)stackPanel.getComponent(0), null);
                }
            } else if (step[0] == 60) {
                stack.remove(stack.size() - 1);
                updateStackVisual();
            } else if (step[0] > 60 && step[0] <= 80) {
                // Animate second operand moving below first
                float progress = (step[0] - 60) / 20.0f;
                int x = (int)(startPos.x + (belowPos.x - startPos.x) * progress);
                int y = (int)(startPos.y + (belowPos.y - startPos.y) * progress);
                floatingLabel2.setLocation(x, y);
            }

            // Step 81-100: Create result
            else if (step[0] == 81) {
                addNotification("Combining operands with operator '" + operator + "'");

                // Create result expression based on conversion mode
                String result;
                if (isPostfixInput) {
                    if (isInfixMode) {
                        // Postfix to Infix: operand2 operator operand1 (with parentheses)
                        result = "(" + operand2 + " " + operator + " " + operand1 + ")";
                    } else {
                        // Postfix to Prefix: operator operand2 operand1
                        result = operator + " " + operand2 + " " + operand1;
                    }
                } else {
                    if (isInfixMode) {
                        // Prefix to Infix: operand1 operator operand2 (with parentheses)
                        result = "(" + operand1 + " " + operator + " " + operand2 + ")";
                    } else {
                        // Prefix to Postfix: operand1 operand2 operator
                        result = operand1 + " " + operand2 + " " + operator;
                    }
                }

                // Create and position result label above the operands
                JLabel resultLabel = createFloatingLabel(result);
                Point resultPos = new Point(
                        (rightPos.x + belowPos.x) / 2,
                        Math.min(rightPos.y, belowPos.y) - 70
                );
                resultLabel.setLocation(resultPos);
                glassPane.add(resultLabel);
                glassPane.revalidate();
                glassPane.repaint();

                // Save result for step 3
                glassPane.putClientProperty("resultLabel", resultLabel);
                glassPane.putClientProperty("resultValue", result);
                resultValue[0] = result;
                addNotification("Created expression: " + result);

                // Add detailed explanation of the operation with specific rule
                addDetailedExplanation("operator", operator, operand1, operand2, result);
            }

            // Step 101-130: Move result to stack
            else if (step[0] >= 101 && step[0] <= 130) {
                JLabel resultLabel = (JLabel)glassPane.getClientProperty("resultLabel");
                if (resultLabel != null) {
                    float progress = (step[0] - 100) / 30.0f;
                    int x = (int)(resultLabel.getX() + (startPos.x - resultLabel.getX()) * progress);
                    int y = (int)(resultLabel.getY() + (startPos.y - resultLabel.getY()) * progress);
                    resultLabel.setLocation(x, y);

                    // Keep operands visible
                    floatingLabel1.setVisible(true);
                    floatingLabel2.setVisible(true);
                }
            }

            // Final step: Add result to stack and clean up
            else if (step[0] > 130) {
                addNotification("Placing result back in stack");
                String result = (String)glassPane.getClientProperty("resultValue");

                // Make sure to update the actual stack
                stack.add(result);
                updateStackVisual();
                topLabel.setText("Top of Stack: " + result);
                addNotification("Pushed '" + result + "' onto stack");
                addOperationSeparator();

                // Clean up animation
                sequence[0].stop();
                glassPane.removeAll();
                glassPane.setVisible(false);

                isAnimating = false;
                nextStepButton.setEnabled(true);
                autoConvertButton.setEnabled(true);
            }
        });

        // Start the animation
        sequence[0].start();
    }

    // Make an element fade out gradually
    private void fadeOutElement(JPanel element, Runnable onComplete) {
        Timer fadeTimer = new Timer(50, null);
        float[] alpha = {1.0f};

        fadeTimer.addActionListener(e -> {
            alpha[0] -= 0.1f;
            if (alpha[0] <= 0) {
                ((Timer)e.getSource()).stop();
                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                element.setBackground(new Color(46, 139, 87, (int)(alpha[0] * 255)));
                element.repaint();
            }
        });

        fadeTimer.start();
    }

    // Create a floating label for animating stack operations
    private JLabel createFloatingLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        label.setBackground(new Color(46, 139, 87));
        label.setOpaque(true);
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        label.setSize(350, 50); // Match stack element size
        return label;
    }

    // Push a value to the stack (used for operands)
    private void pushToStack(String value) {
        isAnimating = true;
        stack.add(value);
        updateStackVisual();
        topLabel.setText("Top of Stack: " + value);
        addNotification("Pushed '" + value + "' onto stack");
        // Add detailed explanation for operand
        addDetailedExplanation("operand", "", value, "", "");
        addOperationSeparator();
        isAnimating = false;
    }

    // Helper method to check if a token is an operator
    private boolean isOperator(String token) {
        return token.length() == 1 && operators.contains(token);
    }

    // Reset everything to start a new conversion
    private void resetOperation() {
        stack.clear();
        updateStackVisual();
        topLabel.setText("Top of Stack: ");
        resultLabel.setText("Final Result: ");
        expressionLabel.setText("Current Expression: ");
        messageLabel.setText("");
        notificationContent = new StringBuilder("<html><body>");
        notificationArea.setText(notificationContent.toString() + "</body></html>");
        currentTokenIndex = -1;
        tokens = null;
        expressionArrowPanel.setVisible(false);
        disableControls();
    }

    // Update the visual representation of the stack
    private void updateStackVisual() {
        stackPanel.removeAll();

        // Add a scrollable container for many stack elements
        if (stack.size() > 10) {
            JPanel scrollContent = new JPanel();
            scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
            scrollContent.setBackground(Color.WHITE);

            // Add stack elements to scroll content
            for (int i = stack.size() - 1; i >= 0; i--) {
                JPanel element = createStackElement(stack.get(i));
                scrollContent.add(element);
                scrollContent.add(Box.createVerticalStrut(5));
            }

            JScrollPane scrollPane = new JScrollPane(scrollContent);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(null);
            scrollPane.setPreferredSize(new Dimension(400, 650)); // Increased from 500 to 650
            stackPanel.add(scrollPane);
        } else {
            // For smaller stacks, display directly without scrolling
            for (int i = stack.size() - 1; i >= 0; i--) {
                JPanel element = createStackElement(stack.get(i));
                stackPanel.add(element);
                stackPanel.add(Box.createVerticalStrut(5));
            }
        }

        stackPanel.revalidate();
        stackPanel.repaint();
    }

    // Helper method to create stack element panels consistently
    private JPanel createStackElement(String value) {
        JPanel element = new JPanel(new GridBagLayout());
        element.setMaximumSize(new Dimension(400, 50));
        element.setPreferredSize(new Dimension(400, 50));
        element.setBackground(new Color(46, 139, 87));
        element.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 120, 80), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JLabel label = new JLabel(value);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        element.add(label);

        return element;
    }

    // Make all buttons look nice with a green color
    private void styleButton(JButton button) {
        // Set basic button properties
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 35));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);

        // Use navy blue for better contrast than green
        Color buttonColor = new Color(25, 55, 105); // Dark navy blue
        Color borderColor = new Color(15, 35, 70);  // Darker blue for border

        // Set fixed appearance
        button.setBackground(buttonColor);
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Add hover effect to the button
        addButtonHoverEffect(button);
    }

    // Update the window title based on the current conversion mode
    private void updateTitle() {
        String title = "";
        switch (conversionModeCombo.getSelectedIndex()) {
            case 0:
                title = "Prefix to Postfix Converter Visualizer";
                break;
            case 1:
                title = "Prefix to Infix Converter Visualizer";
                break;
            case 2:
                title = "Postfix to Prefix Converter Visualizer";
                break;
            case 3:
                title = "Postfix to Infix Converter Visualizer";
                break;
            case 4:
                title = "Infix to Prefix Converter Visualizer";
                break;
            case 5:
                title = "Infix to Postfix Converter Visualizer";
                break;
            case 6:
                title = "String Reversal Visualizer";
                break;
            case 7:
                title = "Bracket Balancing Visualizer";
                break;
        }
        setTitle(title);
    }

    // Add a step to the notification area with colored tokens
    private void addNotification(String message) {
        String coloredMessage = message;

        // Color operators red
        for (String operator : operators) {
            coloredMessage = coloredMessage.replace(
                    "'" + operator + "'",
                    "'<font color='red'>" + operator + "</font>'"
            );
        }

        // Color operands blue
        if (message.contains("operand:")) {
            coloredMessage = coloredMessage.replaceAll(
                    ": '([^']*)'",
                    ": '<font color='blue'>$1</font>'"
            );
        }

        // Color the final expression
        if (message.contains("Pushed '")) {
            String[] parts = message.split("'");
            if (parts.length >= 2) {
                String expr = parts[1];
                String coloredExpr = colorExpression(expr);
                coloredMessage = parts[0] + "'" + coloredExpr + "'";
            }
        }

        notificationContent.append("→ ").append(coloredMessage).append("<br>");
        notificationArea.setText(notificationContent.toString() + "</body></html>");
        notificationArea.setCaretPosition(notificationArea.getDocument().getLength());
    }

    // Color parts of an expression to make it easier to read
    private String colorExpression(String expr) {
        StringBuilder result = new StringBuilder();
        String[] tokens = expr.split(" ");
        for (String token : tokens) {
            if (isOperator(token)) {
                result.append("<font color='red'>").append(token).append("</font>");
            } else {
                result.append("<font color='blue'>").append(token).append("</font>");
            }
            result.append(" ");
        }
        return result.toString().trim();
    }

    // Add a separator line in the notification area
    private void addOperationSeparator() {
        notificationContent.append("<br>----------------------------------------<br><br>");
        notificationArea.setText(notificationContent.toString() + "</body></html>");
        notificationArea.setCaretPosition(notificationArea.getDocument().getLength());
    }

    // New method to animate bracket matching
    private void animateBracketMatching(String openBracket, String closeBracket) {
        isAnimating = true;
        nextStepButton.setEnabled(false);
        autoConvertButton.setEnabled(false);
        messageLabel.setText("");

        // Create a Glass Pane for animation
        JPanel glassPane = new JPanel(null);
        glassPane.setOpaque(false);
        setGlassPane(glassPane);
        glassPane.setVisible(true);

        // Calculate positions
        Point stackLocation = stackPanel.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(stackLocation, glassPane);
        Point stackPos = new Point(stackLocation.x + 100, stackLocation.y + 10); // Moved right by 50 pixels
        Point comparePos = new Point(stackPos.x + 300, stackPos.y); // Increased distance for comparison

        // Create labels for both brackets at the start
        JLabel openLabel = new JLabel(openBracket, SwingConstants.CENTER);
        JLabel closeLabel = new JLabel(closeBracket, SwingConstants.CENTER);

        // Style both labels
        for (JLabel label : new JLabel[]{openLabel, closeLabel}) {
            label.setFont(new Font("Segoe UI", Font.BOLD, 18));
            label.setForeground(Color.WHITE);
            label.setBackground(new Color(46, 139, 87));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            label.setSize(350, 50); // Match stack element size
        }

        // Position initial labels
        closeLabel.setLocation(stackPos.x, stackPos.y - 50); // Start above stack
        openLabel.setLocation(stackPos.x, stackPos.y); // Start at stack position
        openLabel.setVisible(false); // Hide initially

        glassPane.add(openLabel);
        glassPane.add(closeLabel);

        // Animation sequence
        Timer[] sequence = new Timer[1];
        int[] step = {0};

        sequence[0] = new Timer(100, e -> {
            step[0]++;

            // Phase 1: Push closing bracket to stack (steps 1-20)
            if (step[0] <= 20) {
                float progress = step[0] / 20.0f;
                int y = (int)(stackPos.y - 50 + progress * 50);
                closeLabel.setLocation(stackPos.x, y);

                if (step[0] == 20) {
                    // Actually add to stack
                    stack.add(closeBracket);
                    updateStackVisual();
                    addNotification("Pushed closing bracket '" + closeBracket + "' to stack");
                }
            }
            // Phase 2: Pop brackets and move to comparison (steps 21-40)
            else if (step[0] == 21) {
                // Make opening bracket visible
                openLabel.setVisible(true);

                // Remove from stack
                stack.remove(stack.size() - 1); // Remove closing bracket
                stack.remove(stack.size() - 1); // Remove opening bracket
                updateStackVisual();
                addNotification("Comparing brackets '" + openBracket + "' and '" + closeBracket + "'");
            }
            else if (step[0] > 21 && step[0] <= 40) {
                float progress = (step[0] - 21) / 19.0f;
                int x = (int)(stackPos.x + (comparePos.x - stackPos.x) * progress);

                openLabel.setLocation(x, stackPos.y);
                closeLabel.setLocation(x, stackPos.y + 60); // Increased vertical separation
            }
            // Phase 3: Show match animation (steps 41-60)
            else if (step[0] == 41) {
                addNotification("Brackets match!");
                // Change color to indicate match
                openLabel.setBackground(new Color(46, 139, 87));
                closeLabel.setBackground(new Color(46, 139, 87));
            }
            // Phase 4: Fade out (steps 61-80)
            else if (step[0] > 60 && step[0] <= 80) {
                float progress = (step[0] - 60) / 20.0f;
                int alpha = (int)(255 * (1 - progress));

                openLabel.setBackground(new Color(46, 139, 87, alpha));
                closeLabel.setBackground(new Color(46, 139, 87, alpha));

                // Also fade the text
                openLabel.setForeground(new Color(255, 255, 255, alpha));
                closeLabel.setForeground(new Color(255, 255, 255, alpha));
            }
            // Cleanup
            else if (step[0] > 80) {
                sequence[0].stop();
                glassPane.removeAll();
                glassPane.setVisible(false);

                isAnimating = false;
                nextStepButton.setEnabled(true);
                autoConvertButton.setEnabled(true);

                currentTokenIndex++;
                updateExpressionAndArrow();
            }
        });

        sequence[0].start();
    }

    // Helper method to add hover effect to buttons
    private void addButtonHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(15, 35, 70)); // Darker blue on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(25, 55, 105)); // Return to original blue
            }
        });
    }

    // Method to display a popup menu with example expressions
    private void showExamplesMenu() {
        JPopupMenu examplesMenu = new JPopupMenu("Examples");

        // Determine which sample expressions to show based on conversion mode
        int modeIndex = conversionModeCombo.getSelectedIndex();
        int exprIndex = 0; // Default to first sample for special modes

        // Special handling for String Reversal and Bracket Balancing
        if (modeIndex == 6) { // String Reversal
            JMenuItem item1 = new JMenuItem("Hello World");
            JMenuItem item2 = new JMenuItem("Java Programming");
            JMenuItem item3 = new JMenuItem("Stack Visualizer");

            item1.addActionListener(e -> inputField.setText(item1.getText()));
            item2.addActionListener(e -> inputField.setText(item2.getText()));
            item3.addActionListener(e -> inputField.setText(item3.getText()));

            examplesMenu.add(item1);
            examplesMenu.add(item2);
            examplesMenu.add(item3);
        }
        else if (modeIndex == 7) { // Bracket Balancing
            JMenuItem item1 = new JMenuItem("(()())");
            JMenuItem item2 = new JMenuItem("({[]})");
            JMenuItem item3 = new JMenuItem("([)]"); // Unbalanced example

            item1.addActionListener(e -> inputField.setText(item1.getText()));
            item2.addActionListener(e -> inputField.setText(item2.getText()));
            item3.addActionListener(e -> inputField.setText(item3.getText()));

            examplesMenu.add(item1);
            examplesMenu.add(item2);
            examplesMenu.add(item3);
        }
        else {
            // For regular conversion modes, select the appropriate expression column
            // If "to Postfix" or "to Prefix" mode, show Infix expressions
            if (modeIndex == 4 || modeIndex == 5) {
                exprIndex = 0; // Show infix expressions (first column)
            }
            // If "Postfix to Infix" or "Postfix to Prefix" mode, show postfix expressions
            else if (modeIndex == 3 || modeIndex == 2) {
                exprIndex = 1; // Show postfix expressions (second column)
            }
            // If "Prefix to Postfix" or "Prefix to Infix" mode, show prefix expressions
            else if (modeIndex == 0 || modeIndex == 1) {
                exprIndex = 2; // Show prefix expressions (third column)
            }

            // Add all sample expressions of the selected type to the menu
            for (int i = 0; i < sampleExpressions.length; i++) {
                String expr = sampleExpressions[i][exprIndex];
                JMenuItem item = new JMenuItem(expr);
                item.addActionListener(e -> inputField.setText(item.getText()));
                examplesMenu.add(item);
            }
        }

        // Show the menu near the examples button
        examplesMenu.show(examplesButton, 0, examplesButton.getHeight());
    }

    // New method to add a detailed explanation with rule information
    private void addDetailedExplanation(String action, String operator, String operand1, String operand2, String result) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("<div style='padding: 5px; background-color: #f8f9fa; border-left: 4px solid #6c757d; margin: 5px 0;'>");
        explanation.append("<b>Detailed Explanation:</b><br>");

        String rule = "";

        if (action.equals("operator")) {
            if (isPostfixInput && isInfixMode) {
                // Postfix to Infix rule
                rule = "For postfix to infix conversion when an operator '" + operator +
                        "' is encountered, we pop two operands from the stack and create an infix expression " +
                        "by placing the operator between them with parentheses: (operand2 " + operator + " operand1)";
            }
            else if (!isPostfixInput && isInfixMode) {
                // Prefix to Infix rule
                rule = "For prefix to infix conversion when an operator '" + operator +
                        "' is encountered, we pop two operands from the stack and create an infix expression " +
                        "by placing the operator between them with parentheses: (operand1 " + operator + " operand2)";
            }
            else if (isPostfixInput && !isInfixMode) {
                // Postfix to Prefix rule
                rule = "For postfix to prefix conversion when an operator '" + operator +
                        "' is encountered, we pop two operands from the stack and create a prefix expression " +
                        "by placing the operator first followed by the operands: " + operator + " operand2 operand1";
            }
            else {
                // Prefix to Postfix rule
                rule = "For prefix to postfix conversion when an operator '" + operator +
                        "' is encountered, we pop two operands from the stack and create a postfix expression " +
                        "by placing the operands first followed by the operator: operand1 operand2 " + operator;
            }

            explanation.append(rule).append("<br><br>");
            explanation.append("Operands: <font color='blue'>" + operand1 + "</font> and <font color='blue'>" +
                    operand2 + "</font><br>");
            explanation.append("Operator: <font color='red'>" + operator + "</font><br>");
            explanation.append("Result: <font color='green'>" + result + "</font>");
        }
        else if (action.equals("operand")) {
            if (isPostfixInput) {
                rule = "In postfix conversion, when an operand is encountered, we simply push it onto the stack.";
            } else {
                rule = "In prefix conversion, when an operand is encountered, we simply push it onto the stack.";
            }
            explanation.append(rule);
        }

        explanation.append("</div>");

        notificationContent.append(explanation);
        notificationArea.setText(notificationContent.toString() + "</body></html>");
        notificationArea.setCaretPosition(notificationArea.getDocument().getLength());
    }

    // Show the history of expressions
    private void showHistoryMenu() {
        JPopupMenu historyMenu = new JPopupMenu("History");

        if (expressionHistory.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No history available");
            emptyItem.setEnabled(false);
            historyMenu.add(emptyItem);
        } else {
            // Add a clear history option
            JMenuItem clearItem = new JMenuItem("Clear History");
            clearItem.setForeground(new Color(220, 53, 69));
            clearItem.addActionListener(e -> {
                expressionHistory.clear();
                showMessage("History cleared");
            });
            historyMenu.add(clearItem);
            historyMenu.addSeparator();

            // Show most recent entries first (up to 10)
            int start = Math.max(0, expressionHistory.size() - 10);
            for (int i = expressionHistory.size() - 1; i >= start; i--) {
                ExpressionHistoryRecord record = expressionHistory.get(i);
                JMenuItem item = new JMenuItem(record.toString());
                item.addActionListener(e -> inputField.setText(record.getInputExpression()));
                historyMenu.add(item);
            }
        }

        // Show the menu near the history button
        historyMenu.show(historyButton, 0, historyButton.getHeight());
    }

    public static void main(String[] args) {
        // Suppress various warning messages from Java/Swing
        // (These were annoying me during debugging)
        System.setProperty("java.awt.suppressSwingDropSupport", "true");

        // Redirect error stream to suppress specific warnings
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // Do nothing - suppress output
            }
        }));

        SwingUtilities.invokeLater(() -> {
            StackExpConViz visualizer = new StackExpConViz();
            visualizer.setVisible(true);
            // Restore error stream after window is shown
            System.setErr(originalErr);
        });
    }

    // Validate expressions as the user types
    private void validateExpressionInput() {
        String input = inputField.getText().trim();
        int modeIndex = conversionModeCombo.getSelectedIndex();

        // Don't validate empty input
        if (input.isEmpty()) {
            inputField.setBackground(Color.WHITE);
            return;
        }

        // Handle special modes differently
        if (modeIndex == 6) { // String Reversal - any string is valid
            inputField.setBackground(new Color(223, 240, 216)); // Light green
            messageLabel.setText("Valid input for string reversal");
            messageLabel.setForeground(new Color(40, 167, 69)); // Green
            return;
        } else if (modeIndex == 7) { // Bracket Balancing - validate only brackets
            boolean valid = validateBracketBalancing(input);
            updateValidationUI(valid, valid ? "Balanced brackets" : "Unbalanced brackets");
            return;
        }

        // For regular conversion modes, validate based on the selected mode
        try {
            String[] tokens;
            boolean isValid = false;

            // Determine the input format based on the conversion mode
            boolean isInfixFormat = (modeIndex == 4 || modeIndex == 5); // Infix to Prefix/Postfix
            boolean isPostfixFormat = (modeIndex == 2 || modeIndex == 3); // Postfix to Prefix/Infix
            boolean isPrefixFormat = (modeIndex == 0 || modeIndex == 1); // Prefix to Postfix/Infix

            // Tokenize the input based on its format
            if (isInfixFormat) {
                tokens = tokenizeInfixExpression(input);
                isValid = isValidInfixExpression(tokens);
            } else if (isPostfixFormat) {
                tokens = tokenizeExpression(input);
                isValid = isValidPostfixExpression(tokens);
            } else if (isPrefixFormat) {
                tokens = tokenizeExpression(input);
                isValid = isValidPrefixExpression(tokens);
            }

            // Update UI to show validation result
            updateValidationUI(isValid, isValid ? "Valid expression" : "Invalid expression");

        } catch (Exception ex) {
            // Handle any tokenization or validation errors
            updateValidationUI(false, "Invalid expression");
        }
    }

    // Helper to validate bracket balancing
    private boolean validateBracketBalancing(String input) {
        Stack<Character> stack = new Stack<>();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')' || c == ']' || c == '}') {
                if (stack.isEmpty()) {
                    return false;
                }

                char top = stack.pop();
                if ((c == ')' && top != '(') ||
                        (c == ']' && top != '[') ||
                        (c == '}' && top != '{')) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    // Update UI elements based on validation result
    private void updateValidationUI(boolean isValid, String message) {
        if (isValid) {
            inputField.setBackground(new Color(223, 240, 216)); // Light green
            messageLabel.setText(message);
            messageLabel.setForeground(new Color(40, 167, 69)); // Green
        } else {
            inputField.setBackground(new Color(242, 222, 222)); // Light red
            messageLabel.setText(message);
            messageLabel.setForeground(new Color(220, 53, 69)); // Red
        }
    }

    // Helper method to add hover effect to buttons with custom colors
    private void addButtonHoverEffect(JButton button, Color hoverColor, Color defaultColor) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(defaultColor);
            }
        });
    }

    // Show educational information about notation types and conversion rules
    private void showEducationalInfo() {
        // Choose content based on the selected conversion mode
        int modeIndex = conversionModeCombo.getSelectedIndex();
        String title = "About " + conversionModeCombo.getSelectedItem();

        StringBuilder content = new StringBuilder();
        content.append("<html><body style='width: 400px; padding: 10px;'>");
        content.append("<h2>").append(title).append("</h2>");

        // Add general information about notation types
        content.append("<h3>Notation Types:</h3>");
        content.append("<ul>");
        content.append("<li><b>Infix Notation:</b> Operators are placed between operands (e.g., A + B)</li>");
        content.append("<li><b>Prefix Notation:</b> Operators are placed before operands (e.g., + A B)</li>");
        content.append("<li><b>Postfix Notation:</b> Operators are placed after operands (e.g., A B +)</li>");
        content.append("</ul>");

        // Add operator precedence information
        content.append("<h3>Operator Precedence (highest to lowest):</h3>");
        content.append("<ol>");
        content.append("<li>^ (Exponentiation)</li>");
        content.append("<li>*, / (Multiplication, Division)</li>");
        content.append("<li>+, - (Addition, Subtraction)</li>");
        content.append("</ol>");

        // Add mode-specific information
        content.append("<h3>Conversion Rules:</h3>");

        switch (modeIndex) {
            case 0: // Prefix to Postfix
                content.append("<p>To convert <b>Prefix to Postfix</b>:</p>");
                content.append("<ol>");
                content.append("<li>Read the prefix expression from <b>right to left</b>.</li>");
                content.append("<li>For each token:</li>");
                content.append("<ul>");
                content.append("<li>If it's an operand, push onto the stack.</li>");
                content.append("<li>If it's an operator, pop two operands, create 'operand1 operand2 operator', and push the result back.</li>");
                content.append("</ul>");
                content.append("<li>The final element on the stack is the postfix expression.</li>");
                content.append("</ol>");
                break;

            case 1: // Prefix to Infix
                content.append("<p>To convert <b>Prefix to Infix</b>:</p>");
                content.append("<ol>");
                content.append("<li>Read the prefix expression from <b>right to left</b>.</li>");
                content.append("<li>For each token:</li>");
                content.append("<ul>");
                content.append("<li>If it's an operand, push onto the stack.</li>");
                content.append("<li>If it's an operator, pop two operands, create '(operand1 operator operand2)', and push the result back.</li>");
                content.append("</ul>");
                content.append("<li>The final element on the stack is the infix expression.</li>");
                content.append("</ol>");
                break;

            case 2: // Postfix to Prefix
                content.append("<p>To convert <b>Postfix to Prefix</b>:</p>");
                content.append("<ol>");
                content.append("<li>Read the postfix expression from <b>left to right</b>.</li>");
                content.append("<li>For each token:</li>");
                content.append("<ul>");
                content.append("<li>If it's an operand, push onto the stack.</li>");
                content.append("<li>If it's an operator, pop two operands, create 'operator operand2 operand1', and push the result back.</li>");
                content.append("</ul>");
                content.append("<li>The final element on the stack is the prefix expression.</li>");
                content.append("</ol>");
                break;

            case 3: // Postfix to Infix
                content.append("<p>To convert <b>Postfix to Infix</b>:</p>");
                content.append("<ol>");
                content.append("<li>Read the postfix expression from <b>left to right</b>.</li>");
                content.append("<li>For each token:</li>");
                content.append("<ul>");
                content.append("<li>If it's an operand, push onto the stack.</li>");
                content.append("<li>If it's an operator, pop two operands, create '(operand2 operator operand1)', and push the result back.</li>");
                content.append("</ul>");
                content.append("<li>The final element on the stack is the infix expression.</li>");
                content.append("</ol>");
                break;

            case 4: // Infix to Prefix
                content.append("<p>To convert <b>Infix to Prefix</b>:</p>");
                content.append("<ol>");
                content.append("<li>REVERSE the input string.</li>");
                content.append("<li>Swap ( with ) and vice versa during the reversal.</li>");
                content.append("<li>FOR each token in the reversed input:</li>");
                content.append("<ul>");
                content.append("<li>IF token is an operand: APPEND token to the result.</li>");
                content.append("<li>ELSE IF token is ) (closing bracket in reversed form): PUSH token to the stack.</li>");
                content.append("<li>ELSE IF token is ( (opening bracket in reversed form):</li>");
                content.append("<ul>");
                content.append("<li>POP from the stack and APPEND to result until ) is found.</li>");
                content.append("<li>POP ) from the stack.</li>");
                content.append("</ul>");
                content.append("<li>ELSE IF token is an operator:</li>");
                content.append("<ul>");
                content.append("<li>WHILE stack is not empty AND top of the stack has greater or equal precedence:</li>");
                content.append("<li>POP from the stack and APPEND to result.</li>");
                content.append("<li>PUSH the operator to the stack.</li>");
                content.append("</ul>");
                content.append("</ul>");
                content.append("<li>WHILE stack is not empty: POP from the stack and APPEND to result.</li>");
                content.append("<li>REVERSE the result.</li>");
                content.append("</ol>");
                break;

            case 5: // Infix to Postfix
                content.append("<p>To convert <b>Infix to Postfix</b>:</p>");
                content.append("<ol>");
                content.append("<li>FOR each token in the input:</li>");
                content.append("<ul>");
                content.append("<li>IF token is an operand: APPEND token to the result.</li>");
                content.append("<li>ELSE IF token is ( (opening bracket): PUSH token to the stack.</li>");
                content.append("<li>ELSE IF token is ) (closing bracket):</li>");
                content.append("<ul>");
                content.append("<li>POP from the stack and APPEND to result until ( is found.</li>");
                content.append("<li>POP ( from the stack.</li>");
                content.append("</ul>");
                content.append("<li>ELSE IF token is an operator:</li>");
                content.append("<ul>");
                content.append("<li>WHILE stack is not empty AND top of the stack has greater or equal precedence:</li>");
                content.append("<li>POP from the stack and APPEND to result.</li>");
                content.append("<li>PUSH the operator to the stack.</li>");
                content.append("</ul>");
                content.append("</ul>");
                content.append("<li>WHILE stack is not empty: POP from the stack and APPEND to result.</li>");
                content.append("</ol>");
                break;

            case 6: // String Reversal
                content.append("<p><b>String Reversal</b> uses a stack to reverse a string by:</p>");
                content.append("<ol>");
                content.append("<li>Pushing each character onto a stack (from left to right).</li>");
                content.append("<li>Popping each character to get the reversed string (LIFO structure).</li>");
                content.append("</ol>");
                break;

            case 7: // Bracket Balancing
                content.append("<p><b>Bracket Balancing</b> uses a stack to check if brackets are balanced:</p>");
                content.append("<ol>");
                content.append("<li>Push each opening bracket onto the stack.</li>");
                content.append("<li>For each closing bracket, check if it matches the top bracket on the stack.</li>");
                content.append("<li>If matched, pop the opening bracket; otherwise, the expression is unbalanced.</li>");
                content.append("<li>After processing all characters, the stack should be empty for a balanced expression.</li>");
                content.append("</ol>");
                break;
        }

        content.append("<div style='margin-top: 15px; font-style: italic;'>These operations are visualized step-by-step in the application.</div>");
        content.append("</body></html>");

        // Create and show the dialog
        JOptionPane.showMessageDialog(
                this,
                new JLabel(content.toString()),
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
