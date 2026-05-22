package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.exception.VehicleException;
import com.femzyk.vehiclesystem.interfaces.CarVehicle;
import com.femzyk.vehiclesystem.model.Car;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

/**
 * AddCarDialog
 *
 * PURPOSE:
 * Provides a modal form for adding a new car or editing an existing car.
 *
 * FEATURES:
 * - Add mode: creates a new Car object.
 * - Edit mode: pre-fills existing car values and creates an updated Car object.
 * - Optional renter name and renter phone fields.
 * - Styled dark-theme Swing controls.
 * - Custom combo box UI to fix Windows Look & Feel selected-text issues.
 *
 * DESIGN NOTE:
 * This dialog does not directly add a car to the fleet. Instead, it returns
 * the created/updated Car object through getResult(). The calling panel/window
 * decides how to store it.
 */
public class AddCarDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JTextField makeField;
    private JTextField modelField;
    private JTextField renterNameField;
    private JTextField renterPhoneField;

    private JSpinner yearSpinner;
    private JSpinner doorsSpinner;

    private JComboBox<String> fuelCombo;

    private JLabel errorLabel;

    /** Result is null if the dialog is cancelled or validation fails. */
    private Car result = null;

    /** Existing car is non-null only when editing. */
    private final Car existingCar;

    /** True when editing an existing car. */
    private final boolean editMode;

    public AddCarDialog(Frame parent) {
        this(parent, null);
    }

    public AddCarDialog(Frame parent, Car existingCar) {
        super(parent, existingCar == null ? "Add New Car" : "Edit Car", true);

        this.existingCar = existingCar;
        this.editMode = existingCar != null;

        setSize(460, 590);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();

        if (editMode) {
            populateExistingValues();
        }
    }

    /**
     * Builds the complete dialog UI.
     */
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConstants.BG_CARD);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(ThemeConstants.ACCENT_CAR_DARK);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel(editMode ? "Edit Car Information" : "Add New Car to Fleet");
        title.setFont(ThemeConstants.FONT_SUBHEADING);
        title.setForeground(Color.WHITE);

        header.add(title);
        return header;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ThemeConstants.BG_CARD);
        form.setBorder(new EmptyBorder(20, 24, 10, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        addFormRow(form, gbc, 0, "Make (Manufacturer):", makeField = createTextField());
        addFormRow(form, gbc, 1, "Model:", modelField = createTextField());

        int currentYear = java.time.Year.now().getValue();
        yearSpinner = new JSpinner(
            new SpinnerNumberModel(currentYear, 1886, currentYear + 1, 1)
        );
        styleSpinner(yearSpinner);
        addFormRow(form, gbc, 2, "Year of Manufacture:", yearSpinner);

        doorsSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 5, 1));
        styleSpinner(doorsSpinner);
        addFormRow(form, gbc, 3, "Number of Doors (2-5):", doorsSpinner);

        fuelCombo = new JComboBox<>(CarVehicle.FUEL_TYPES);
        styleCombo(fuelCombo, ThemeConstants.ACCENT_CAR);
        addFormRow(form, gbc, 4, "Fuel Type:", fuelCombo);

        addFormRow(form, gbc, 5, "Renter Name (optional):", renterNameField = createTextField());
        addFormRow(form, gbc, 6, "Renter Phone (optional):", renterPhoneField = createTextField());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeConstants.FONT_SMALL);
        errorLabel.setForeground(ThemeConstants.TEXT_ERROR);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        form.add(errorLabel, gbc);

        return form;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttons.setBackground(ThemeConstants.BG_CARD);
        buttons.setBorder(new EmptyBorder(0, 14, 10, 14));

        JButton cancelBtn = createButton("Cancel", ThemeConstants.BTN_NEUTRAL, 110);
        JButton saveBtn = createButton(
            editMode ? "Save Changes" : "Add Car",
            ThemeConstants.ACCENT_CAR,
            editMode ? 140 : 110
        );

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> handleSave());

        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        return buttons;
    }

    /**
     * Copies existing vehicle values into the form during edit mode.
     */
    private void populateExistingValues() {
        makeField.setText(existingCar.getMake());
        modelField.setText(existingCar.getModel());
        yearSpinner.setValue(existingCar.getYear());
        doorsSpinner.setValue(existingCar.getNumberOfDoors());
        fuelCombo.setSelectedItem(existingCar.getFuelType());
        renterNameField.setText(existingCar.getRenterName());
        renterPhoneField.setText(existingCar.getRenterPhone());
    }

    /**
     * Reads form values and creates the result Car object.
     *
     * Renter fields are optional. Blank values are allowed.
     */
    private void handleSave() {
        errorLabel.setText(" ");

        try {
            String make = makeField.getText().trim();
            String model = modelField.getText().trim();
            int year = (Integer) yearSpinner.getValue();
            int doors = (Integer) doorsSpinner.getValue();
            String fuelType = (String) fuelCombo.getSelectedItem();
            String renterName = renterNameField.getText().trim();
            String renterPhone = renterPhoneField.getText().trim();

            result = new Car(make, model, year, doors, fuelType, renterName, renterPhone);
            dispose();

        } catch (VehicleException e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc,
                            int row, String labelText, JComponent field) {

        JLabel label = new JLabel(labelText);
        label.setFont(ThemeConstants.FONT_BODY);
        label.setForeground(ThemeConstants.TEXT_SECONDARY);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.40;
        form.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.60;
        form.add(field, gbc);
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();

        tf.setBackground(ThemeConstants.BG_INPUT);
        tf.setForeground(ThemeConstants.TEXT_PRIMARY);
        tf.setCaretColor(ThemeConstants.TEXT_PRIMARY);
        tf.setFont(ThemeConstants.FONT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeConstants.BORDER_CARD),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        return tf;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(ThemeConstants.BG_INPUT);
        spinner.setForeground(ThemeConstants.TEXT_PRIMARY);
        spinner.setFont(ThemeConstants.FONT_BODY);

        JComponent editor = spinner.getEditor();

        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setBackground(ThemeConstants.BG_INPUT);
            defaultEditor.getTextField().setForeground(ThemeConstants.TEXT_PRIMARY);
            defaultEditor.getTextField().setCaretColor(ThemeConstants.TEXT_PRIMARY);
            defaultEditor.getTextField().setFont(ThemeConstants.FONT_BODY);
        }
    }

    /**
     * Styles combo boxes and fixes selected item visibility under Windows L&F.
     */
    private void styleCombo(JComboBox<String> combo, Color accentColor) {
        combo.setFont(ThemeConstants.FONT_BODY);
        combo.setBackground(ThemeConstants.BG_INPUT);
        combo.setForeground(ThemeConstants.TEXT_PRIMARY);
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setEditable(false);
        combo.setMaximumRowCount(6);

        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeConstants.BORDER_CARD),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("v");
                button.setFont(ThemeConstants.FONT_SMALL);
                button.setBackground(ThemeConstants.BG_INPUT);
                button.setForeground(ThemeConstants.TEXT_PRIMARY);
                button.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                button.setOpaque(true);
                return button;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g,
                                                    Rectangle bounds,
                                                    boolean hasFocus) {
                g.setColor(ThemeConstants.BG_INPUT);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            public void paintCurrentValue(Graphics g,
                                          Rectangle bounds,
                                          boolean hasFocus) {

                @SuppressWarnings({ "rawtypes", "unchecked" })
                ListCellRenderer renderer = comboBox.getRenderer();

                Component component = renderer.getListCellRendererComponent(
                    listBox,
                    comboBox.getSelectedItem(),
                    -1,
                    false,
                    false
                );

                if (component instanceof JLabel label) {
                    label.setOpaque(true);
                    label.setBackground(ThemeConstants.BG_INPUT);
                    label.setForeground(ThemeConstants.TEXT_PRIMARY);
                    label.setFont(ThemeConstants.FONT_BODY);
                    label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                }

                currentValuePane.paintComponent(
                    g,
                    component,
                    comboBox,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    true
                );
            }
        });

        combo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );

                label.setFont(ThemeConstants.FONT_BODY);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                if (index == -1) {
                    label.setBackground(ThemeConstants.BG_INPUT);
                    label.setForeground(ThemeConstants.TEXT_PRIMARY);
                } else if (isSelected) {
                    label.setBackground(accentColor);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(ThemeConstants.BG_CARD);
                    label.setForeground(ThemeConstants.TEXT_PRIMARY);
                }

                return label;
            }
        });

        combo.repaint();
    }

    private JButton createButton(String text, Color bg, int width) {
        JButton btn = new JButton(text);

        btn.setFont(ThemeConstants.FONT_BUTTON);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(width, ThemeConstants.BTN_HEIGHT));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }

    public Car getResult() {
        return result;
    }
}