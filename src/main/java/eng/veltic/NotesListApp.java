package eng.veltic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author valkarinc
 * This application is going to be a top tier Notes application
 */

public class NotesListApp extends JFrame {
    // Dark theme colors
    private static final Color DARK_BG = new Color(28, 28, 30);
    private static final Color SIDEBAR_BG = new Color(44, 44, 46);
    private static final Color CONTENT_BG = new Color(58, 58, 60);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(174, 174, 178);
    private static final Color ACCENT_YELLOW = new Color(255, 214, 10);
    private static final Color BUTTON_BG = new Color(72, 72, 74);
    private static final Color HOVER_BG = new Color(88, 88, 90);
    private static final Color SUCCESS_GREEN = new Color(48, 209, 88);
    private static final Color SEARCH_BG = new Color(38, 38, 40);

    // Components
    private DefaultListModel<NoteItem> notesModel;
    private DefaultListModel<NoteItem> filteredModel;
    private JList<NoteItem> notesList;
    private JTextArea contentArea;
    private JLabel titleLabel;
    private JLabel dateLabel;
    private JLabel statusLabel;
    private JButton newNoteButton;
    private JButton deleteButton;
    private JButton exportButton;
    private JTextField searchField;
    private JLabel wordCountLabel;
    private JProgressBar saveIndicator;
    private Timer saveTimer;

    // Data
    private List<NoteItem> notes;
    private NoteItem currentNote;
    private boolean isSearching = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new NotesListApp().setVisible(true);
        });
    }

    public NotesListApp() {
        initializeData();
        setupUI();
        setupEventHandlers();
        setupAutoSave();
        loadSampleNotes();
    }

    private void initializeData() {
        notes = new ArrayList<>();
        notesModel = new DefaultListModel<>();
        filteredModel = new DefaultListModel<>();
    }

    private void setupUI() {
        setTitle("Noted");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout());

        JPanel sidebarPanel = createSidebarPanel();
        JPanel contentPanel = createContentPanel();
        JPanel statusPanel = createStatusPanel();

        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        setupKeyboardShortcuts();
    }

    private void setupKeyboardShortcuts() {
        // Cmd/Ctrl + N for new note
        KeyStroke newNoteStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(newNoteStroke, "newNote");
        getRootPane().getActionMap().put("newNote", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewNote();
            }
        });

        // Cmd/Ctrl + F for search
        KeyStroke searchStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(searchStroke, "search");
        getRootPane().getActionMap().put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
                searchField.selectAll();
            }
        });

        // Delete key for delete note
        KeyStroke deleteStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(deleteStroke, "deleteNote");
        getRootPane().getActionMap().put("deleteNote", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentNote != null && notesList.hasFocus()) {
                    deleteCurrentNote();
                }
            }
        });
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(320, 0));
        sidebar.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(SIDEBAR_BG);

        JLabel appTitle = new JLabel("Notes");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        appTitle.setForeground(TEXT_PRIMARY);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(SIDEBAR_BG);

        newNoteButton = createStyledButton("+ New");
        exportButton = createStyledButton("Export");
        exportButton.setBackground(SUCCESS_GREEN);

        buttonPanel.add(exportButton);
        buttonPanel.add(newNoteButton);

        headerPanel.add(appTitle, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        searchField = new JTextField();
        searchField.setBackground(SEARCH_BG);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BG, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.setToolTipText("Search notes... (Ctrl+F)");

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(TEXT_SECONDARY);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(SIDEBAR_BG);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchIcon, BorderLayout.WEST);

        JLabel notesCountLabel = new JLabel();
        notesCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        notesCountLabel.setForeground(TEXT_SECONDARY);
        updateNotesCount(notesCountLabel);

        notesList = new JList<>(notesModel);
        notesList.setBackground(SIDEBAR_BG);
        notesList.setForeground(TEXT_PRIMARY);
        notesList.setSelectionBackground(ACCENT_YELLOW.darker());
        notesList.setSelectionForeground(Color.BLACK);
        notesList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        notesList.setCellRenderer(new NotesListRenderer());
        notesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPopupMenu contextMenu = createContextMenu();
        notesList.setComponentPopupMenu(contextMenu);

        JScrollPane listScrollPane = new JScrollPane(notesList);
        listScrollPane.setBackground(SIDEBAR_BG);
        listScrollPane.setBorder(BorderFactory.createEmptyBorder());
        listScrollPane.getViewport().setBackground(SIDEBAR_BG);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(SIDEBAR_BG);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(Box.createVerticalStrut(15));
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(notesCountLabel, BorderLayout.SOUTH);

        sidebar.add(topPanel, BorderLayout.NORTH);
        sidebar.add(listScrollPane, BorderLayout.CENTER);

        return sidebar;
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(CONTENT_BG);
        menu.setBorder(BorderFactory.createLineBorder(BUTTON_BG));

        JMenuItem duplicateItem = new JMenuItem("Duplicate Note");
        duplicateItem.setBackground(CONTENT_BG);
        duplicateItem.setForeground(TEXT_PRIMARY);
        duplicateItem.addActionListener(e -> duplicateCurrentNote());

        JMenuItem renameItem = new JMenuItem("Rename Note");
        renameItem.setBackground(CONTENT_BG);
        renameItem.setForeground(TEXT_PRIMARY);
        renameItem.addActionListener(e -> renameCurrentNote());

        JMenuItem deleteItem = new JMenuItem("Delete Note");
        deleteItem.setBackground(CONTENT_BG);
        deleteItem.setForeground(new Color(255, 59, 48));
        deleteItem.addActionListener(e -> deleteCurrentNote());

        menu.add(duplicateItem);
        menu.add(renameItem);
        menu.addSeparator();
        menu.add(deleteItem);

        return menu;
    }

    private JPanel createContentPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CONTENT_BG);
        content.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CONTENT_BG);

        titleLabel = new JLabel("Select a note to edit");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_PRIMARY);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(CONTENT_BG);

        dateLabel = new JLabel("");
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dateLabel.setForeground(TEXT_SECONDARY);

        wordCountLabel = new JLabel("");
        wordCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        wordCountLabel.setForeground(TEXT_SECONDARY);

        infoPanel.add(dateLabel, BorderLayout.WEST);
        infoPanel.add(wordCountLabel, BorderLayout.EAST);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controlsPanel.setBackground(CONTENT_BG);

        deleteButton = createStyledButton("Delete");
        deleteButton.setBackground(new Color(255, 59, 48));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setEnabled(false);

        controlsPanel.add(deleteButton);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(CONTENT_BG);
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(controlsPanel, BorderLayout.EAST);

        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(infoPanel, BorderLayout.SOUTH);

        contentArea = new JTextArea();
        contentArea.setBackground(CONTENT_BG);
        contentArea.setForeground(TEXT_PRIMARY);
        contentArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(new EmptyBorder(20, 0, 0, 0));
        contentArea.setEnabled(false);
        contentArea.setCaretColor(ACCENT_YELLOW);

        contentArea.setTabSize(4);

        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        contentScrollPane.setBackground(CONTENT_BG);
        contentScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentScrollPane.getViewport().setBackground(CONTENT_BG);
        contentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        content.add(headerPanel, BorderLayout.NORTH);
        content.add(contentScrollPane, BorderLayout.CENTER);

        return content;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(DARK_BG);
        statusPanel.setBorder(new EmptyBorder(8, 20, 8, 20));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_SECONDARY);

        saveIndicator = new JProgressBar();
        saveIndicator.setIndeterminate(true);
        saveIndicator.setVisible(false);
        saveIndicator.setPreferredSize(new Dimension(100, 4));
        saveIndicator.setBackground(DARK_BG);
        saveIndicator.setForeground(SUCCESS_GREEN);

        JLabel shortcutLabel = new JLabel("Ctrl+N: New • Ctrl+F: Search • Del: Delete");
        shortcutLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        shortcutLabel.setForeground(TEXT_SECONDARY.darker());

        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(saveIndicator, BorderLayout.CENTER);
        statusPanel.add(shortcutLabel, BorderLayout.EAST);

        return statusPanel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_BG);
        button.setForeground(TEXT_PRIMARY);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(HOVER_BG);
                    if (button.getBackground().equals(SUCCESS_GREEN)) {
                        button.setBackground(SUCCESS_GREEN.brighter());
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    if (button == exportButton) {
                        button.setBackground(SUCCESS_GREEN);
                    } else if (button == deleteButton) {
                        button.setBackground(new Color(255, 59, 48));
                    } else {
                        button.setBackground(BUTTON_BG);
                    }
                }
            }
        });

        return button;
    }

    private void setupEventHandlers() {
        newNoteButton.addActionListener(e -> createNewNote());

        exportButton.addActionListener(e -> exportNotes());
        deleteButton.addActionListener(e -> deleteCurrentNote());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterNotes(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterNotes(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterNotes(); }
        });

        notesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectNote();
            }
        });

        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { handleContentChange(); }
            @Override
            public void removeUpdate(DocumentEvent e) { handleContentChange(); }
            @Override
            public void changedUpdate(DocumentEvent e) { handleContentChange(); }
        });

        contentArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleContentKeyPress(e);
            }
        });
    }

    private void setupAutoSave() {
        saveTimer = new Timer(2000, e -> {
            if (currentNote != null) {
                showSaveIndicator();
                statusLabel.setText("Auto-saved");
                Timer hideTimer = new Timer(1500, hideEvent -> {
                    statusLabel.setText("Ready");
                    hideSaveIndicator();
                });
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });
        saveTimer.setRepeats(false);
    }

    private void handleContentChange() {
        if (currentNote != null) {
            currentNote.setContent(contentArea.getText());
            currentNote.setModified(LocalDateTime.now());
            updateNoteDisplay();
            updateWordCount();

            if (saveTimer.isRunning()) {
                saveTimer.restart();
            } else {
                saveTimer.start();
            }
        }
    }

    private void handleContentKeyPress(KeyEvent e) {
        if (currentNote == null) return;

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            handleEnterKey(e);
        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
            handleTabKey(e);
        }
    }

    private void handleEnterKey(KeyEvent e) {
        String text = contentArea.getText();
        int caretPos = contentArea.getCaretPosition();

        try {
            int lineStart = contentArea.getLineStartOffset(contentArea.getLineOfOffset(caretPos));
            int lineEnd = contentArea.getLineEndOffset(contentArea.getLineOfOffset(caretPos));
            String currentLine = text.substring(lineStart, lineEnd).trim();

            if (currentLine.startsWith("•") || currentLine.startsWith("-") || currentLine.startsWith("*")) {
                e.consume();
                String bullet = currentLine.substring(0, 1);
                contentArea.insert("\n" + bullet + " ", caretPos);
            } else if (currentLine.matches("\\d+\\..*")) {
                e.consume();
                String[] parts = currentLine.split("\\.", 2);
                int num = Integer.parseInt(parts[0]) + 1;
                contentArea.insert("\n" + num + ". ", caretPos);
            }
        } catch (Exception ex) {
            if (text.length() > 0 && caretPos > 0) {
                String prevChar = text.substring(Math.max(0, caretPos - 2), caretPos);
                if (prevChar.contains("•") || prevChar.contains("-")) {
                    e.consume();
                    contentArea.insert("\n• ", caretPos);
                }
            }
        }
    }

    private void handleTabKey(KeyEvent e) {
        e.consume();
        int caretPos = contentArea.getCaretPosition();
        if (e.isShiftDown()) {
            try {
                String text = contentArea.getText();
                int lineStart = contentArea.getLineStartOffset(contentArea.getLineOfOffset(caretPos));
                String lineText = text.substring(lineStart, caretPos);
                if (lineText.startsWith("    ")) {
                    contentArea.replaceRange("", lineStart, lineStart + 4);
                }
            } catch (Exception ex) {}
        } else {
            contentArea.insert("    ", caretPos);
        }
    }

    private void filterNotes() {
        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            notesList.setModel(notesModel);
            isSearching = false;
        } else {
            filteredModel.clear();
            List<NoteItem> filtered = notes.stream()
                    .filter(note -> note.getTitle().toLowerCase().contains(searchText) ||
                            note.getContent().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());

            for (NoteItem note : filtered) {
                filteredModel.addElement(note);
            }
            notesList.setModel(filteredModel);
            isSearching = true;
        }

        statusLabel.setText(isSearching ? "Searching: " + searchText : "Ready");
    }

    private void createNewNote() {
        String title = JOptionPane.showInputDialog(this,
                "Enter note title:",
                "New Note",
                JOptionPane.PLAIN_MESSAGE);

        if (title != null && !title.trim().isEmpty()) {
            NoteItem newNote = new NoteItem(title.trim(), "• ");
            notes.add(newNote);
            notesModel.addElement(newNote);
            notesList.setSelectedValue(newNote, true);
            contentArea.requestFocus();
            contentArea.setCaretPosition(contentArea.getText().length());
            statusLabel.setText("Created: " + title);
        }
    }

    private void duplicateCurrentNote() {
        if (currentNote != null) {
            NoteItem duplicate = new NoteItem(
                    currentNote.getTitle() + " (Copy)",
                    currentNote.getContent()
            );
            notes.add(duplicate);
            notesModel.addElement(duplicate);
            notesList.setSelectedValue(duplicate, true);
            statusLabel.setText("Duplicated: " + currentNote.getTitle());
        }
    }

    private void renameCurrentNote() {
        if (currentNote != null) {
            String newTitle = JOptionPane.showInputDialog(this,
                    "Enter new title:",
                    currentNote.getTitle());

            if (newTitle != null && !newTitle.trim().isEmpty()) {
                currentNote.setTitle(newTitle.trim());
                titleLabel.setText(newTitle.trim());
                notesList.repaint();
                statusLabel.setText("Renamed to: " + newTitle);
            }
        }
    }

    private void deleteCurrentNote() {
        if (currentNote != null) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete '" + currentNote.getTitle() + "'?",
                    "Delete Note",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                String title = currentNote.getTitle();
                notes.remove(currentNote);
                notesModel.removeElement(currentNote);
                if (isSearching) {
                    filteredModel.removeElement(currentNote);
                }
                currentNote = null;
                clearContentArea();
                statusLabel.setText("Deleted: " + title);
            }
        }
    }

    private void exportNotes() {
        if (notes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No notes to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Notes");
        fileChooser.setSelectedFile(new java.io.File("notes_export.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                java.io.PrintWriter writer = new java.io.PrintWriter(file);

                for (NoteItem note : notes) {
                    StringBuilder separator = new StringBuilder(50);
                    for (int i = 0; i < 50; i++) {
                        separator.append("=");
                    }

                    writer.println(separator.toString());
                    writer.println("Title: " + note.getTitle());
                    writer.println("Created: " + note.getFormattedDate());
                    writer.println(separator.toString());
                    writer.println(note.getContent());
                }

                writer.close();
                statusLabel.setText("Exported " + notes.size() + " notes to " + file.getName());

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting notes: " + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void selectNote() {
        NoteItem selected = notesList.getSelectedValue();
        if (selected != null) {
            currentNote = selected;
            titleLabel.setText(selected.getTitle());
            dateLabel.setText("Modified: " + selected.getFormattedDate());
            contentArea.setText(selected.getContent());
            contentArea.setEnabled(true);
            deleteButton.setEnabled(true);
            updateWordCount();
            contentArea.requestFocus();
        }
    }

    private void updateNoteDisplay() {
        if (currentNote != null) {
            dateLabel.setText("Modified: " + currentNote.getFormattedDate());
            notesList.repaint();
        }
    }

    private void updateWordCount() {
        if (currentNote != null) {
            String text = contentArea.getText();
            int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            int chars = text.length();
            wordCountLabel.setText(words + " words, " + chars + " characters");
        } else {
            wordCountLabel.setText("");
        }
    }

    private void updateNotesCount(JLabel label) {
        SwingUtilities.invokeLater(() -> {
            label.setText(notes.size() + " notes");
        });
    }

    private void showSaveIndicator() {
        saveIndicator.setVisible(true);
    }

    private void hideSaveIndicator() {
        saveIndicator.setVisible(false);
    }

    private void clearContentArea() {
        titleLabel.setText("Select a note to edit");
        dateLabel.setText("");
        wordCountLabel.setText("");
        contentArea.setText("");
        contentArea.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void loadSampleNotes() {
        NoteItem groceries = new NoteItem("🛒 Grocery List",
                "• Fresh produce:\n    • Apples (Honeycrisp)\n    • Bananas\n    • Spinach\n    • Carrots\n\n• Protein:\n    • Chicken breast\n    • Greek yogurt\n    • Eggs\n\n• Pantry items:\n    • Brown rice\n    • Olive oil\n    • Whole grain bread");

        NoteItem todos = new NoteItem("✅ Weekend Tasks",
                "1. Clean and organize garage\n2. Call mom about dinner plans\n3. Fix the leaky kitchen faucet\n4. Grocery shopping (see grocery list)\n5. Prepare presentation for Monday meeting\n6. Water the plants\n7. Backup computer files");

        NoteItem ideas = new NoteItem("💡 App Ideas",
                "• Recipe Organizer:\n    • Meal planning calendar\n    • Nutrition tracking\n    • Shopping list generator\n\n• Habit Tracker:\n    • Daily streaks\n    • Progress visualization\n    • Reminder system\n\n• Local Events:\n    • Community event discovery\n    • Social meetups\n    • Activity recommendations");

        NoteItem meeting = new NoteItem("📋 Meeting Notes - Q1 Planning",
                "Date: March 15, 2024\nAttendees: Sarah, Mike, Jennifer\n\n• Key Discussion Points:\n    • Budget allocation for Q1\n    • New project timeline\n    • Team resource planning\n\n• Action Items:\n    1. Sarah: Review budget proposal by Friday\n    2. Mike: Update project roadmap\n    3. Jennifer: Schedule team meetings\n\n• Next Steps:\n    • Follow-up meeting scheduled for March 22\n    • Quarterly review preparation");

        notes.add(groceries);
        notes.add(todos);
        notes.add(ideas);
        notes.add(meeting);

        for (NoteItem note : notes) {
            notesModel.addElement(note);
        }

        if (!notes.isEmpty()) {
            notesList.setSelectedIndex(0);
        }
    }

    private class NotesListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(12, 8, 12, 8));

            Color bgColor = isSelected ? ACCENT_YELLOW.darker() : SIDEBAR_BG;
            Color textColor = isSelected ? Color.BLACK : TEXT_PRIMARY;

            panel.setBackground(bgColor);

            if (value instanceof NoteItem) {
                NoteItem note = (NoteItem) value;

                JLabel titleLabel = new JLabel(note.getTitle());
                titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
                titleLabel.setForeground(textColor);
                titleLabel.setOpaque(false);

                JLabel dateLabel = new JLabel(note.getFormattedDate());
                dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
                dateLabel.setForeground(isSelected ? Color.BLACK.brighter() : TEXT_SECONDARY);
                dateLabel.setOpaque(false);

                panel.add(titleLabel, BorderLayout.NORTH);
                panel.add(dateLabel, BorderLayout.SOUTH);

                panel.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(12, 8, 12, 8),
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BUTTON_BG)
                ));
            }

            return panel;
        }
    }

    private static class NoteItem {
        private String title;
        private String content;
        private LocalDateTime created;
        private LocalDateTime modified;

        public NoteItem(String title, String content) {
            this.title = title;
            this.content = content;
            this.created = LocalDateTime.now();
            this.modified = this.created;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
            this.modified = LocalDateTime.now();
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getModified() {
            return modified;
        }

        public void setModified(LocalDateTime modified) {
            this.modified = modified;
        }

        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");
            return modified.format(formatter);
        }

        @Override
        public String toString() {
            return title;
        }
    }
}