package plugins.fab.MiceProfiler.view;

import java.awt.*;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;


public class MiceProfilerWindow {

    private final IcyFrame mainFrame = new IcyFrame("Mice Profiler", true, true, true, true);
    private final JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();

    //Video Settings
    private final JButton setVideoSourceButton = new JButton("Click to set/change video source");
    private final JSlider sliderTime = new JSlider();
    private final JCheckBox useImageBufferOptimisation = new JCheckBox("Use image load optimisation", true);
    private final JTextField numberOfImageForBufferTextField = new JTextField("200");
    private final JLabel bufferValue = new JLabel("0%");
    private final JButton previousFrame = new JButton("Previous Frame");
    private final JButton nextFrame = new JButton("Next Frame");
    private final JButton previous10Frame = new JButton("Previous 10 Frame");
    private final JButton next10Frame = new JButton("Next 10 Frame");
    private final JCheckBox updatePhysicsGuidesCheckBox = new JCheckBox("update phys. guides", true);
    private final JLabel currentTimeLabel = new JLabel("current time");
    //Other
    private final JButton trackAllButton = new JButton("<html><br><b>Track All Start</b><br><br></html>");
    private final JButton stopTrackAllButton = new JButton("<html><br><b>Track All Stop</b><br><br></html>");
    private final JButton readPositionFromROIButton = new JButton("Read starting position from Line ROI.");
    private final JButton saveXMLButton = new JButton("Save XML Data");
    private final JCheckBox limitTrackingSpeedCheckBox = new JCheckBox("Limit tracking speed to 15fps");
    private final JComboBox<String> mouseColorComboBox = new JComboBox<>(new String[] { "Track black mice", "Track white mice" });
    private final JButton reverseTrackFromTButton = new JButton("Reverse Identity (from now to end of sequence)");
    private final JButton startThreadStepButton = new JButton("Start Step Anim");
    private final JButton stopThreadStepButton = new JButton("Stop Step Anim");
    private final JLabel lastFramePhysicTime = new JLabel("lastFramePhysicTime");
    private final JLabel lastFrameLoadTime = new JLabel("lastFrameLoadTime");
    private final JLabel lastFrameForceMapTime = new JLabel("lastFrameForceMapTime");
    private final JLabel totalImageTime = new JLabel("time last image");
    //Panel for Phy Mouse
    private final JCheckBox displayBinaryMapCheckBox = new JCheckBox("Binary Map", false);
    private final JCheckBox displayGradientMapCheckBox = new JCheckBox("Gradient Map", false);
    private final JCheckBox displayForceCheckBox = new JCheckBox("Forces", false);
    private final JCheckBox displayEnergyAreaCheckBox = new JCheckBox("Energy Area", false);
    private final JCheckBox displayBodyCenterCheckBox = new JCheckBox("Body Center", false);
    private final JCheckBox displayBodyShapeCheckBox = new JCheckBox("Body Shape", true);
    private final JCheckBox displayGlobalSplineCheckBox = new JCheckBox("Global Spline", true);
    private final JCheckBox displaySlideJointCheckBox = new JCheckBox("Slide Joint", true);
    private final JCheckBox displayDistanceJointCheckBox = new JCheckBox("Distance Joint", true);
    private final JCheckBox displayMemoryCheckBox = new JCheckBox("Track Memory", true);
    private final JCheckBox useMotionPredictionCheckBox = new JCheckBox("Use motion prediction", false);
    private final JSpinner binaryThresholdSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 255, 10));
    private final JTextField scaleTextField = new JTextField("0.22");

    public void initialize(ActionListener actionListener, ChangeListener changeListener) {
        //Initialize Mice Profiler window
        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(mainPanel, BorderLayout.CENTER);
        mainPanel.add(GuiUtil.besidesPanel(createVideoPanel(actionListener, changeListener)));
        mainPanel.add(GuiUtil.besidesPanel(trackAllButton, stopTrackAllButton));
        mainPanel.add(GuiUtil.besidesPanel(readPositionFromROIButton));
        mainPanel.add(GuiUtil.besidesPanel(saveXMLButton));
        mainPanel.add(GuiUtil.besidesPanel(limitTrackingSpeedCheckBox));
        mainPanel.add(GuiUtil.besidesPanel(mouseColorComboBox));
        mainPanel.add(GuiUtil.besidesPanel(reverseTrackFromTButton));
        mainPanel.add(GuiUtil.besidesPanel(createPhyMousePanel(actionListener, changeListener)));
        mainPanel.add(GuiUtil.besidesPanel(startThreadStepButton, stopThreadStepButton));
        mainPanel.add(GuiUtil.besidesPanel(lastFramePhysicTime, lastFrameLoadTime));
        mainPanel.add(GuiUtil.besidesPanel(lastFrameForceMapTime, totalImageTime));
        trackAllButton.addActionListener(actionListener);
        stopTrackAllButton.addActionListener(actionListener);
        readPositionFromROIButton.addActionListener(actionListener);
        saveXMLButton.addActionListener(actionListener);
        mouseColorComboBox.addActionListener(actionListener);
        reverseTrackFromTButton.addActionListener(actionListener);
        startThreadStepButton.addActionListener(actionListener);
        stopThreadStepButton.addActionListener(actionListener);

        enableAtStart();

        mainFrame.pack();
        mainFrame.center();
        mainFrame.setVisible(true);
        mainFrame.addToDesktopPane();
    }

    /** Triggered **/
    public boolean hasBeenTriggeredByVideoSourceButton(Object source) {
        return source == setVideoSourceButton;
    }
    public boolean hasBeenTriggeredBySliderTime(Object source) {
        return source == sliderTime;
    }

    public boolean hasBeenTriggeredByPreviousFrame(Object source) {
        return source == previousFrame;
    }
    public boolean hasBeenTriggeredByNextFrame(Object source) {
        return source == nextFrame;
    }
    public boolean hasBeenTriggeredByPrevious10Frame(Object source) {
        return source == previous10Frame;
    }
    public boolean hasBeenTriggeredByNext10Frame(Object source) {
        return source == next10Frame;
    }

    public boolean hasBeenTriggeredByTrackAllButton(Object source) {
        return source == trackAllButton;
    }
    public boolean hasBeenTriggeredByStopTrackAllButton(Object source) {
        return source == stopTrackAllButton;
    }

    public boolean hasBeenTriggeredByReadPositionFromROIButton(Object source) {
        return source == readPositionFromROIButton;
    }

    public boolean hasBeenTriggeredBySaveXML(Object source) {
        return source == saveXMLButton;
    }

    public boolean hasBeenTriggeredByReverseTrackFromTButton(Object source) {
        return source == reverseTrackFromTButton;
    }

    public boolean hasBeenTriggeredByStartThreadStepButton(Object source) {
        return source == startThreadStepButton;
    }
    public boolean hasBeenTriggeredByStopThreadStepButton(Object source) {
        return source == stopThreadStepButton;
    }

    /** Inputs **/
    public synchronized int getCurrentFrame() {
        return sliderTime.getValue();
    }

    public boolean useImageBufferOptimisation() { return useImageBufferOptimisation.isSelected(); }
    public int numberOfImageForBufferOptimisation() throws NumberFormatException { return Integer.parseInt(numberOfImageForBufferTextField.getText()); }

    public boolean updatePhysicsGuides() { return updatePhysicsGuidesCheckBox.isSelected(); }

    public boolean limitTrackingSpeed() { return limitTrackingSpeedCheckBox.isSelected(); }

    public String mouseColor() { return mouseColorComboBox.getActionCommand(); }

    public boolean displayBinaryMap() { return displayBinaryMapCheckBox.isSelected(); }
    public boolean displayGradientMap() { return displayGradientMapCheckBox.isSelected(); }
    public boolean displayForce() { return displayForceCheckBox.isSelected(); }
    public boolean displayEnergyArea() { return displayEnergyAreaCheckBox.isSelected(); }
    public boolean displayBodyCenter() { return displayBodyCenterCheckBox.isSelected(); }
    public boolean displayBodyShape() { return displayBodyShapeCheckBox.isSelected(); }
    public boolean displayGlobalSpline() { return displayGlobalSplineCheckBox.isSelected(); }
    public boolean displaySlideJoint() { return displaySlideJointCheckBox.isSelected(); }
    public boolean displayDistanceJoint() { return displayDistanceJointCheckBox.isSelected(); }
    public boolean displayMemory() { return displayMemoryCheckBox.isSelected(); }

    public boolean useMotionPrediction() { return useMotionPredictionCheckBox.isSelected(); }

    public int binaryThreshold() { return (int)binaryThresholdSpinner.getValue(); }
    public float mouseModelScale() { return Float.parseFloat(scaleTextField.getText()); }

    /** Outputs **/
    public synchronized void setSliderTimeValue(int value) {
        sliderTime.setValue(value);
    }
    public synchronized void setRelativeSliderValue(int offset) {
        sliderTime.setValue(sliderTime.getValue() + offset);
    }

    public void setBufferValue(int newBufferValue) {
        bufferValue.setText(newBufferValue + " %");
    }

    public void setCurrentTimeLabel(long frame, long totalNumberOfFrame, String timeForFrame) {
        String timeString = "(#frame): " + frame + "/" + totalNumberOfFrame + " " + timeForFrame;
        currentTimeLabel.setText(timeString);
    }

    public void setTotalImageTimeText(String text) {
        totalImageTime.setText(text);
    }

    /** Enable/disable elements **/
    private void enableAtStart() {
        setVideoSourceButton.setEnabled(true);
        sliderTime.setEnabled(false);
        sliderTime.setValue(0);
        useImageBufferOptimisation.setEnabled(true);
        bufferValue.setEnabled(true);
        previousFrame.setEnabled(false);
        nextFrame.setEnabled(false);
        previous10Frame.setEnabled(false);
        next10Frame.setEnabled(false);
        updatePhysicsGuidesCheckBox.setEnabled(true);
        trackAllButton.setEnabled(false);
        stopTrackAllButton.setEnabled(false);
        readPositionFromROIButton.setEnabled(false);
        saveXMLButton.setEnabled(false);
        limitTrackingSpeedCheckBox.setEnabled(true);
        mouseColorComboBox.setEnabled(true);
        reverseTrackFromTButton.setEnabled(true);
        displayBinaryMapCheckBox.setEnabled(true);
        displayGradientMapCheckBox.setEnabled(true);
        displayForceCheckBox.setEnabled(true);
        displayEnergyAreaCheckBox.setEnabled(true);
        displayBodyCenterCheckBox.setEnabled(true);
        displayBodyShapeCheckBox.setEnabled(true);
        displayGlobalSplineCheckBox.setEnabled(true);
        displaySlideJointCheckBox.setEnabled(true);
        displayDistanceJointCheckBox.setEnabled(true);
        displayMemoryCheckBox.setEnabled(true);
        useMotionPredictionCheckBox.setEnabled(true);
        binaryThresholdSpinner.setEnabled(true);
        mouseColorComboBox.setEnabled(true);
        startThreadStepButton.setEnabled(false);
        stopThreadStepButton.setEnabled(false);
    }

    public void enableAfterVideoLoading(long videoTotalNumberOfFrame, String timeForFrame) {
        sliderTime.setEnabled(true);
        sliderTime.setValue(0);
        sliderTime.setMaximum((int)videoTotalNumberOfFrame);
        setCurrentTimeLabel(0, videoTotalNumberOfFrame, timeForFrame);

        previousFrame.setEnabled(true);
        nextFrame.setEnabled(true);
        previous10Frame.setEnabled(true);
        next10Frame.setEnabled(true);
        trackAllButton.setEnabled(true);
        readPositionFromROIButton.setEnabled(true);
        saveXMLButton.setEnabled(true);
        startThreadStepButton.setEnabled(true);
        useImageBufferOptimisation.setEnabled(false);
        numberOfImageForBufferTextField.setEnabled(false);
    }

    public void enableAfterTrackAllStarted() {
        stopTrackAllButton.setEnabled(true);
        trackAllButton.setEnabled(false);
        startThreadStepButton.setEnabled(false);
        readPositionFromROIButton.setEnabled(false);
    }

    public void enableAfterTrackAllStopped() {
        stopTrackAllButton.setEnabled(false);
        trackAllButton.setEnabled(true);
        startThreadStepButton.setEnabled(true);
        readPositionFromROIButton.setEnabled(true);
    }

    private JPanel createVideoPanel(ActionListener actionListener, ChangeListener changeListener) {
        JPanel videoPanel = GuiUtil.generatePanel("Video Settings");
        videoPanel.add(GuiUtil.besidesPanel(setVideoSourceButton));
        videoPanel.add(GuiUtil.besidesPanel(sliderTime));
        videoPanel.add(GuiUtil.besidesPanel(useImageBufferOptimisation, numberOfImageForBufferTextField));
        videoPanel.add(GuiUtil.besidesPanel(new JLabel("Current buffer:"), bufferValue));
        videoPanel.add(GuiUtil.besidesPanel(previousFrame, nextFrame));
        videoPanel.add(GuiUtil.besidesPanel(previous10Frame, next10Frame));
        videoPanel.add(GuiUtil.besidesPanel(updatePhysicsGuidesCheckBox));
        videoPanel.add(GuiUtil.besidesPanel(currentTimeLabel));
        setVideoSourceButton.addActionListener(actionListener);
        sliderTime.setMajorTickSpacing(1000);
        sliderTime.setPaintTicks(true);
        sliderTime.setPaintTrack(true);
        sliderTime.addChangeListener(changeListener);
        previousFrame.addActionListener(actionListener);
        nextFrame.addActionListener(actionListener);
        previous10Frame.addActionListener(actionListener);
        next10Frame.addActionListener(actionListener);

        return videoPanel;
    }

    private JPanel createPhyMousePanel(ActionListener actionListener, ChangeListener changeListener) {
        JPanel panel = GuiUtil.generatePanel();
        panel.add(GuiUtil.besidesPanel(displayBinaryMapCheckBox, displayGradientMapCheckBox));
        panel.add(GuiUtil.besidesPanel(displayForceCheckBox, displayEnergyAreaCheckBox));
        panel.add(GuiUtil.besidesPanel(displayBodyCenterCheckBox, displayBodyShapeCheckBox));
        panel.add(GuiUtil.besidesPanel(displayGlobalSplineCheckBox, displaySlideJointCheckBox));
        panel.add(GuiUtil.besidesPanel(displayDistanceJointCheckBox, displayMemoryCheckBox));
        panel.add(GuiUtil.besidesPanel(useMotionPredictionCheckBox));
        panel.add(GuiUtil.besidesPanel(new JLabel("Binary Threshold:"), binaryThresholdSpinner));
        JLabel mouseModelScaleLabel = new JLabel("Mouse Model Scale:");
        mouseModelScaleLabel.setToolTipText("Scale of the model.");
        panel.add(GuiUtil.besidesPanel(mouseModelScaleLabel, scaleTextField)); // applyNewScaleButton
        this.displayBinaryMapCheckBox.addActionListener(actionListener);
        this.displayGradientMapCheckBox.addActionListener(actionListener);
        this.displayForceCheckBox.addActionListener(actionListener);
        this.displayEnergyAreaCheckBox.addActionListener(actionListener);
        this.displayBodyCenterCheckBox.addActionListener(actionListener);
        this.displayBodyShapeCheckBox.addActionListener(actionListener);
        this.displayGlobalSplineCheckBox.addActionListener(actionListener);
        this.displaySlideJointCheckBox.addActionListener(actionListener);
        this.displayDistanceJointCheckBox.addActionListener(actionListener);
        this.binaryThresholdSpinner.addChangeListener(changeListener);

        return panel;
    }
}
