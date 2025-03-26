import javax.swing.*;
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

// This program shows how expressions are converted between prefix, infix, and postfix
// I used Swing for the UI and tried to make it look as nice as possible
public class StackExpConViz extends JFrame {
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

    public StackExpConViz() {
        stack = new ArrayList<>();
        setTitle("Expression Converter Visualizer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Set up the operator precedence levels
        // + and - have lowest precedence (1)
        precedence.put("+", 1);
        precedence.put("-", 1);
        // * and / have medium precedence (2)
        precedence.put("*", 2);
        precedence.put("/", 2);
        // ^ (exponent) has highest precedence (3)
        precedence.put("^", 3);

        createInputPanel();
        createNotificationPanel();
        createStackPanel();
        createInfoPanel();
        createControlPanel();

        setLocationRelativeTo(null); // Center the window on screen
    }

    private void createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        // Top panel for controls - this has the dropdown and text field
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // These are all the conversion types we support
        String[] modes = {
                "Prefix to Postfix",
                "Prefix to Infix",
                "Postfix to Prefix",
                "Postfix to Infix",
                "Infix to Prefix",
                "Infix to Postfix",
                "String Reversal",  // Added new mode
                "Bracket Balancing"  // Added new mode
        };
        conversionModeCombo = new JComboBox<>(modes);
        conversionModeCombo.addActionListener(e -> {
            int selectedIndex = conversionModeCombo.getSelectedIndex();
            // Update flags based on the selected conversion mode
            isInfixMode = selectedIndex == 1 || selectedIndex == 3; // Infix output
            isPostfixInput = selectedIndex == 2 || selectedIndex == 3; // Postfix input
            isInfixInput = selectedIndex >= 4 && selectedIndex < 6; // Infix input
            updateTitle();
        });

        JLabel instructionLabel = new JLabel("Enter Expression:");
        inputField = new JTextField(30);
        JButton startButton = new JButton("Start Conversion");
        JButton resetButton = new JButton("Reset");

        styleButton(startButton);
        styleButton(resetButton);

        startButton.addActionListener(e -> startConversion());
        resetButton.addActionListener(e -> resetOperation());

        controlsPanel.add(conversionModeCombo);
        controlsPanel.add(instructionLabel);
        controlsPanel.add(inputField);
        controlsPanel.add(startButton);
        controlsPanel.add(resetButton);

        // This panel shows the current expression with an arrow pointing to the next token
        expressionArrowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        expressionArrowPanel.setVisible(false); // Hidden initially until conversion starts

        currentExpressionLabel = new JLabel();
        currentExpressionLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Green arrow that points to the next token we'll process
        nextOperationArrow = new JLabel("↑");
        nextOperationArrow.setFont(new Font("Arial", Font.BOLD, 24));
        nextOperationArrow.setForeground(new Color(46, 139, 87));

        JPanel arrowTextPanel = new JPanel(new BorderLayout());
        arrowTextPanel.setOpaque(false);
        arrowTextPanel.add(nextOperationArrow, BorderLayout.CENTER);

        expressionArrowPanel.add(currentExpressionLabel);
        expressionArrowPanel.add(arrowTextPanel);

        // Add both panels to input panel
        inputPanel.add(controlsPanel);
        inputPanel.add(expressionArrowPanel);

        add(inputPanel, BorderLayout.NORTH);
    }

    private void createStackPanel() {
        stackPanel = new JPanel();
        stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.Y_AXIS));
        stackPanel.setBorder(BorderFactory.createTitledBorder("Stack Visualization"));

        // Create a fixed-size panel to contain the stack
        JPanel fixedSizePanel = new JPanel();
        fixedSizePanel.setPreferredSize(new Dimension(400, 400));
        fixedSizePanel.setLayout(new BorderLayout());

        // Create a panel that will align stack elements to the bottom
        JPanel stackAlignPanel = new JPanel();
        stackAlignPanel.setLayout(new BoxLayout(stackAlignPanel, BoxLayout.Y_AXIS));
        stackAlignPanel.add(Box.createVerticalGlue()); // Push elements to bottom
        stackAlignPanel.add(stackPanel);

        fixedSizePanel.add(stackAlignPanel, BorderLayout.CENTER);

        // Create wrapper panel for the entire content
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.Y_AXIS));

        JPanel stackWrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        stackWrapperPanel.add(fixedSizePanel);
        wrapperPanel.add(stackWrapperPanel);

        // Add credit label
        JLabel creditLabel = new JLabel("Made by Abdullah Irshad ©");
        creditLabel.setForeground(new Color(46, 139, 87));
        creditLabel.setFont(new Font("Arial", Font.BOLD, 18));
        JPanel creditPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        creditPanel.add(creditLabel);
        wrapperPanel.add(creditPanel);

        add(wrapperPanel, BorderLayout.CENTER);
    }

    private void createInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Conversion Information"));

        expressionLabel = new JLabel("Current Expression: ");
        topLabel = new JLabel("Top of Stack: ");
        resultLabel = new JLabel("Final Result: ");
        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.RED);

        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(expressionLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(topLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(resultLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(messageLabel);

        add(infoPanel, BorderLayout.EAST);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        nextStepButton = new JButton("Next Step");
        autoConvertButton = new JButton("Auto Convert");

        styleButton(nextStepButton);
        styleButton(autoConvertButton);

        nextStepButton.addActionListener(e -> processNextStep());
        autoConvertButton.addActionListener(e -> autoConvert());

        nextStepButton.setEnabled(false);
        autoConvertButton.setEnabled(false);

        controlPanel.add(nextStepButton);
        controlPanel.add(autoConvertButton);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void createNotificationPanel() {
        notificationPanel = new JPanel();
        notificationPanel.setLayout(new BorderLayout());
        notificationPanel.setBorder(BorderFactory.createTitledBorder("Operation Steps"));
        notificationPanel.setPreferredSize(new Dimension(300, 200));

        notificationArea = new JTextPane();
        notificationArea.setContentType("text/html");
        notificationArea.setEditable(false);
        notificationArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        notificationArea.setFont(new Font("Monaco", Font.PLAIN, 14));
        notificationArea.setBackground(new Color(248, 248, 248));

        JScrollPane scrollPane = new JScrollPane(notificationArea);
        notificationPanel.add(scrollPane, BorderLayout.CENTER);

        add(notificationPanel, BorderLayout.WEST);
    }

    private String[] tokenizeExpression(String input) {
        // If the input is infix, we need special handling for brackets and operators
        if (isInfixInput) {
            return tokenizeInfixExpression(input);
        }

        ArrayList<String> tokenList = new ArrayList<>();

        // For prefix and postfix inputs, just split by characters
        // Each character is either an operand or operator
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

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

        // For infix input, we need to handle multi-character tokens
        // and keep track of brackets and operators
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String charStr = String.valueOf(c);

            // If it's a space, finish the current token and skip the space
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    tokenList.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                continue;
            }

            // Special handling for operators and brackets
            if (operators.contains(charStr) || brackets.contains(charStr)) {
                // If we have a pending token, add it first
                if (currentToken.length() > 0) {
                    tokenList.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                // Add the operator or bracket as its own token
                tokenList.add(charStr);
            } else {
                // For operands (like variables or numbers), keep building the token
                currentToken.append(c);
            }
        }

        // Add any remaining token (in case there's no space at the end)
        if (currentToken.length() > 0) {
            tokenList.add(currentToken.toString());
        }

        // Print tokens for debugging - I needed this a lot while testing
        StringBuilder debugMsg = new StringBuilder("Infix Tokens: ");
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
            return false;
        }

        // Handle simpler expressions without brackets (like A+B+C)
        // These should just alternate between operands and operators
        if (!brackets.contains(tokens[0]) && !brackets.contains(tokens[tokens.length - 1])) {
            boolean expectOperand = true; // Start expecting an operand

            for (String token : tokens) {
                if (brackets.contains(token)) {
                    // If we have a bracket in an unbracketed expression format, fail
                    continue;
                }

                boolean isOp = operators.contains(token);

                if (expectOperand && isOp) {
                    // Expected operand but got operator - this is wrong
                    System.out.println("Invalid: expected operand but got operator: " + token);
                    continue; // Still try to validate with the standard method below
                }

                if (!expectOperand && !isOp) {
                    // Expected operator but got operand - this is wrong
                    System.out.println("Invalid: expected operator but got operand: " + token);
                    continue; // Still try to validate with the standard method below
                }

                expectOperand = !expectOperand; // Flip expectation for next token
            }

            // If we end on expecting an operand, then we ended with an operator, which is invalid
            if (expectOperand) {
                System.out.println("Invalid: expression ends with an operator");
                // We'll still try the standard validation below
            } else {
                // If it was a simple alternating pattern, it's valid without further checks
                return true;
            }
        }

        // For more complex expressions with brackets

        // First, check if all brackets are balanced
        Stack<String> bracketStack = new Stack<>();
        for (String token : tokens) {
            if (token.equals("(") || token.equals("[") || token.equals("{")) {
                // Push opening brackets onto stack
                bracketStack.push(token);
            } else if (token.equals(")") || token.equals("]") || token.equals("}")) {
                // For closing brackets, check if they match with the last opening bracket
                if (bracketStack.isEmpty()) {
                    System.out.println("Unbalanced brackets: extra closing bracket");
                    return false;
                }

                String openBracket = bracketStack.pop();
                if ((token.equals(")") && !openBracket.equals("(")) ||
                        (token.equals("]") && !openBracket.equals("[")) ||
                        (token.equals("}") && !openBracket.equals("{"))) {
                    System.out.println("Mismatched brackets");
                    return false;
                }
            }
        }

        // If there are still brackets on the stack, we're missing closing brackets
        if (!bracketStack.isEmpty()) {
            System.out.println("Unbalanced brackets: missing closing bracket");
            return false;
        }

        // Now check for proper operator-operand sequence
        boolean expectingOperand = true; // Start expecting an operand
        int openBrackets = 0;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];

            if (brackets.contains(token)) {
                if (token.equals("(") || token.equals("[") || token.equals("{")) {
                    openBrackets++;
                    expectingOperand = true; // After an open bracket, expect an operand
                } else {
                    openBrackets--;
                    expectingOperand = false; // After a closing bracket, expect an operator
                }
            } else if (operators.contains(token)) {
                if (expectingOperand) {
                    // If we expect an operand but got an operator, it's invalid
                    // (My code doesn't handle unary operators yet)
                    System.out.println("Invalid operator sequence: expected operand but got operator");
                    return false;
                }
                expectingOperand = true; // After an operator, expect an operand
            } else {
                // This is an operand
                if (!expectingOperand && i > 0) {
                    // If we expect an operator but got an operand, it's invalid
                    System.out.println("Invalid operand sequence: expected operator but got operand");
                    return false;
                }
                expectingOperand = false; // After an operand, expect an operator
            }
        }

        // The expression should end with an operand or closing bracket
        if (expectingOperand) {
            System.out.println("Expression ends with an operator");
            return false;
        }

        // Ensure all brackets are balanced
        if (openBrackets != 0) {
            System.out.println("Unbalanced brackets");
            return false;
        }

        return true; // If we passed all checks, the expression is valid
    }

    private boolean isValidPostfixExpression(String[] tokens) {
        if (tokens == null || tokens.length == 0) {
            return false;
        }

        // Special case: simple expressions like "AB+" (two operands, one operator)
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

        // Special case: one operator and two operands (e.g., "+AB")
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

    private String[] convertInfixToPrefix(String[] infixTokens) {
        // To convert infix to prefix:
        // 1. Reverse the infix expression (swapping brackets)
        // 2. Convert the reversed infix to postfix
        // 3. Reverse the postfix to get prefix

        // Step 1: Reverse the infix expression
        String[] reversedInfix = new String[infixTokens.length];
        for (int i = 0; i < infixTokens.length; i++) {
            String token = infixTokens[infixTokens.length - 1 - i];
            // Swap brackets (this is important)
            if (token.equals("(")) reversedInfix[i] = ")";
            else if (token.equals(")")) reversedInfix[i] = "(";
            else if (token.equals("[")) reversedInfix[i] = "]";
            else if (token.equals("]")) reversedInfix[i] = "[";
            else if (token.equals("{")) reversedInfix[i] = "}";
            else if (token.equals("}")) reversedInfix[i] = "{";
            else reversedInfix[i] = token;
        }

        // Step 2: Convert reversed infix to postfix (using the same algorithm)
        String[] postfix = convertInfixToPostfix(reversedInfix);

        // Step 3: Reverse the postfix to get prefix
        String[] prefix = new String[postfix.length];
        for (int i = 0; i < postfix.length; i++) {
            prefix[i] = postfix[postfix.length - 1 - i];
        }

        return prefix;
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
        return precedence.getOrDefault(op1, 0) >= precedence.getOrDefault(op2, 0);
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

        // Split the input into tokens
        tokens = tokenizeExpression(input);

        // Handle infix inputs specially - they need conversion first
        if (isInfixInput) {
            // Validate the infix expression
            if (!isValidInfixExpression(tokens)) {
                showMessage("Invalid Infix Expression!");
                return;
            }

            // Figure out which conversion we're doing
            boolean toPrefix = conversionModeCombo.getSelectedIndex() == 5; // Infix to Prefix
            boolean toPostfix = conversionModeCombo.getSelectedIndex() == 4; // Infix to Postfix

            // Convert the infix to the target notation
            if (toPrefix) {
                tokens = convertInfixToPrefix(tokens);
            } else if (toPostfix) {
                tokens = convertInfixToPostfix(tokens);
            }

            // We've converted the infix input to either prefix or postfix
            // So now we need to update the flags
            isInfixInput = false;
            isPostfixInput = toPostfix; // true for Infix to Postfix, false for Infix to Prefix
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
        // Handle bracket balancing mode
        if (conversionModeCombo.getSelectedIndex() == 7) {
            if (currentTokenIndex >= tokens.length) {
                // All characters processed, check if stack is empty
                if (stack.isEmpty()) {
                    showMessage("Expression has balanced brackets!");
                    resultLabel.setText("Final Result: Expression is balanced");
                    disableControls();
                    return;
                } else {
                    showMessage("Expression has unbalanced brackets!");
                    resultLabel.setText("Final Result: Expression is unbalanced");
                    disableControls();
                    return;
                }
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

        // Create a timer that processes one token every 2 seconds
        Timer timer = new Timer(2000, new ActionListener() {
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

    // Check if we have a valid result at the end
    private void checkFinalResult() {
        if (stack.size() == 1) {
            // Success - we should have exactly one item on the stack
            resultLabel.setText("Final Result: " + stack.get(0));
            showMessage("Conversion Complete!");
        } else if (!isAnimating) {
            // Only show error if we're not in the middle of an animation
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
                addNotification("Created expression: " + result);
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
        label.setFont(new Font("Arial", Font.BOLD, 18));
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
        for (int i = stack.size() - 1; i >= 0; i--) {
            // Create a green rectangle for each stack element
            JPanel element = new JPanel(new GridBagLayout());
            element.setMaximumSize(new Dimension(350, 50));
            element.setPreferredSize(new Dimension(350, 50));
            element.setBackground(new Color(46, 139, 87));
            element.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            JLabel label = new JLabel(stack.get(i));
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Arial", Font.BOLD, 18));
            element.add(label);
            stackPanel.add(element);
            stackPanel.add(Box.createVerticalStrut(5));
        }
        stackPanel.revalidate();
        stackPanel.repaint();
    }

    // Show error messages or status messages temporarily
    private void showMessage(String message) {
        messageLabel.setText(message);
        Timer timer = new Timer(3000, e -> messageLabel.setText(""));
        timer.setRepeats(false);
        timer.start();
    }

    // Make all buttons look nice with a green color
    private void styleButton(JButton button) {
        button.setBackground(new Color(46, 139, 87));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
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
            label.setFont(new Font("Arial", Font.BOLD, 18));
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
}
