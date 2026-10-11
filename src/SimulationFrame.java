import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;

/** Главное окно с полем, графиком и редактируемыми настройками запуска. */
public class SimulationFrame extends JFrame implements SimulationListener {
    private static final long serialVersionUID = 1L;

    private final SimulationController controller;
    private final WorldPanel worldPanel = new WorldPanel();
    private final PopulationChartPanel chartPanel = new PopulationChartPanel();
    private final JLabel statusLabel = new JLabel("Готово");
    private final JLabel stepLabel = new JLabel("Шаг: 0");
    private final JLabel countLabel = new JLabel("P: 0    H: 0    X: 0");

    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner plantCountSpinner;
    private final JSpinner herbivoreCountSpinner;
    private final JSpinner predatorCountSpinner;
    private final JSpinner plantEnergySpinner;
    private final JSpinner herbivoreEnergySpinner;
    private final JSpinner predatorEnergySpinner;
    private final JSpinner plantThresholdSpinner;
    private final JSpinner herbivoreThresholdSpinner;
    private final JSpinner predatorThresholdSpinner;
    private final JSpinner plantGrowthSpinner;
    private final JSpinner delaySpinner;
    private final JSpinner maxStepsSpinner;
    private final List<JSpinner> allSpinners = new ArrayList<>();

    private final JButton newButton = new JButton("Новая симуляция");
    private final JButton startButton = new JButton("Старт");
    private final JButton stepButton = new JButton("Один шаг");

    public SimulationFrame(SimulationController controller) {
        super("Искусственная жизнь — Экосистема");
        this.controller = controller;
        controller.setListener(this);

        SimulationConfig d = SimulationConfig.defaults();
        widthSpinner = spinner(d.width(), 10, 250, 1);
        heightSpinner = spinner(d.height(), 10, 250, 1);
        plantCountSpinner = spinner(d.initialPlants(), 1, 62500, 1);
        herbivoreCountSpinner = spinner(d.initialHerbivores(), 1, 62500, 1);
        predatorCountSpinner = spinner(d.initialPredators(), 1, 62500, 1);
        plantEnergySpinner = spinner(d.plantEnergy(), 1, 1000000, 1);
        herbivoreEnergySpinner = spinner(d.herbivoreEnergy(), 1, 1000000, 1);
        predatorEnergySpinner = spinner(d.predatorEnergy(), 1, 1000000, 1);
        plantThresholdSpinner = spinner(d.plantReproduceThreshold(), 2, 1000000, 1);
        herbivoreThresholdSpinner = spinner(d.herbivoreReproduceThreshold(), 2, 1000000, 1);
        predatorThresholdSpinner = spinner(d.predatorReproduceThreshold(), 2, 1000000, 1);
        
        plantGrowthSpinner = new JSpinner(
                new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1)
        );
        plantGrowthSpinner.setPreferredSize(new Dimension(124, 30));
        allSpinners.add(plantGrowthSpinner);
        
        delaySpinner = spinner(d.delayMs(), 1, 5000, 1);
        maxStepsSpinner = spinner(d.maxSteps(), 1, 1000000, 100);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1020, 670));
        setSize(1360, 840);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel status = new JPanel(new BorderLayout(14, 4));
        status.setBorder(BorderFactory.createEmptyBorder(9, 12, 2, 12));
        status.add(stepLabel, BorderLayout.WEST);
        status.add(countLabel, BorderLayout.CENTER);
        status.add(statusLabel, BorderLayout.EAST);
        add(status, BorderLayout.NORTH);

        JPanel settings = new JPanel(new GridBagLayout());
        settings.setBorder(BorderFactory.createTitledBorder("Параметры запуска"));
        int row = 0;
        addField(settings, "Ширина:", widthSpinner, row++);
        addField(settings, "Высота:", heightSpinner, row++);
        addField(settings, "Растения:", plantCountSpinner, row++);
        addField(settings, "Травоядные:", herbivoreCountSpinner, row++);
        addField(settings, "Хищники:", predatorCountSpinner, row++);
        addField(settings, "Нач. энергия P:", plantEnergySpinner, row++);
        addField(settings, "Нач. энергия H:", herbivoreEnergySpinner, row++);
        addField(settings, "Нач. энергия X:", predatorEnergySpinner, row++);
        addField(settings, "Порог размнож. P:", plantThresholdSpinner, row++);
        addField(settings, "Рост энергии P:", plantGrowthSpinner, row++);
        
        addField(settings, "Порог размнож. H:", herbivoreThresholdSpinner, row++);
        addField(settings, "Порог размнож. X:", predatorThresholdSpinner, row++);
        addField(settings, "Задержка, мс:", delaySpinner, row++);
        addField(settings, "Макс. шагов:", maxStepsSpinner, row++);

        GridBagConstraints buttons = new GridBagConstraints();
        buttons.gridx = 0;
        buttons.gridy = row;
        buttons.gridwidth = 2;
        buttons.weightx = 1.0;
        buttons.fill = GridBagConstraints.HORIZONTAL;
        buttons.insets = new Insets(10, 6, 2, 6);
        settings.add(newButton, buttons);
        buttons.gridy++;
        buttons.insets = new Insets(5, 6, 2, 6);
        settings.add(startButton, buttons);
        buttons.gridy++;
        settings.add(stepButton, buttons);

        GridBagConstraints bottom = new GridBagConstraints();
        bottom.gridx = 0;
        bottom.gridy = row + 3;
        bottom.weighty = 1.0;
        bottom.fill = GridBagConstraints.VERTICAL;
        settings.add(new JLabel(" "), bottom);

        JScrollPane scroll = new JScrollPane(settings);
        scroll.setPreferredSize(new Dimension(315, 700));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.WEST);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                worldPanel, chartPanel);
        split.setResizeWeight(0.70);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(0.70);
        add(split, BorderLayout.CENTER);

        newButton.addActionListener(e -> createNewSimulation());
        startButton.addActionListener(e -> controller.toggleRunning());
        stepButton.addActionListener(e -> controller.stepOnce());
        delaySpinner.addChangeListener(e -> controller.setDelay(value(delaySpinner)));

        controller.newSimulation(d);
    }

    private JSpinner spinner(int value, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setPreferredSize(new Dimension(124, 30));
        allSpinners.add(spinner);
        return spinner;
    }

    private void addField(JPanel panel, String label, JSpinner spinner, int row) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.LINE_START;
        l.insets = new Insets(5, 7, 5, 6);
        panel.add(new JLabel(label), l);

        GridBagConstraints r = new GridBagConstraints();
        r.gridx = 1;
        r.gridy = row;
        r.weightx = 1.0;
        r.fill = GridBagConstraints.HORIZONTAL;
        r.insets = new Insets(5, 6, 5, 7);
        panel.add(spinner, r);
    }

    private static int value(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private SimulationConfig readConfig() {
        for (JSpinner spinner : allSpinners) {
            try {
                spinner.commitEdit();
            } catch (ParseException e) {
                throw new IllegalArgumentException("Некорректное число в настройках");
            }
        }
        return new SimulationConfig(
                value(widthSpinner), value(heightSpinner),
                value(maxStepsSpinner), value(delaySpinner),
                value(plantCountSpinner), value(herbivoreCountSpinner),
                value(predatorCountSpinner), value(plantEnergySpinner),
                value(herbivoreEnergySpinner), value(predatorEnergySpinner),
                value(plantThresholdSpinner), value(herbivoreThresholdSpinner),
                value(predatorThresholdSpinner),
                ((Number) plantGrowthSpinner.getValue()).doubleValue());
    }

    private void createNewSimulation() {
        try {
            SimulationConfig config = readConfig();
            config.validate();
            controller.newSimulation(config);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Неверные параметры", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showPoint(PopulationPoint point) {
        stepLabel.setText("Шаг: " + point.step());
        countLabel.setText("P: " + point.plants()
                + "       H: " + point.herbivores()
                + "       X: " + point.predators());
    }

    @Override
    public void onSimulationReset(Environment env, PopulationPoint point) {
        worldPanel.setEnvironment(env);
        chartPanel.reset(point);
        showPoint(point);
        statusLabel.setText("Пауза");
        startButton.setEnabled(true);
        stepButton.setEnabled(true);
    }

    @Override
    public void onSimulationUpdated(Environment env, PopulationPoint point) {
        worldPanel.repaint();
        chartPanel.addPoint(point);
        showPoint(point);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        startButton.setText(running ? "Пауза" : "Старт");
        statusLabel.setText(running ? "Работает" : "Пауза");
        stepButton.setEnabled(!running);
    }

    @Override
    public void onSimulationFinished(String reason, PopulationPoint point) {
        statusLabel.setText("Завершено");
        startButton.setEnabled(false);
        stepButton.setEnabled(false);
        JOptionPane.showMessageDialog(this,
                reason + "\n\nШаг: " + point.step()
                        + "\nP: " + point.plants()
                        + "\nH: " + point.herbivores()
                        + "\nX: " + point.predators(),
                "Симуляция завершена", JOptionPane.INFORMATION_MESSAGE);
    }
}
