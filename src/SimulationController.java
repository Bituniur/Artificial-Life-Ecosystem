import javax.swing.Timer;

/** Логика запуска, паузы и шагов. Swing Timer работает в EDT, как и отрисовка. */
public class SimulationController {
    private Environment environment;
    private SimulationConfig config;
    private SimulationListener listener;
    private Timer timer;
    private int step;
    private boolean finished;

    public void setListener(SimulationListener listener) {
        this.listener = listener;
    }

    public void newSimulation(SimulationConfig next) {
        next.validate();
        if (timer != null) timer.stop();
        this.config = next;
        this.environment = SimulationFactory.create(next);
        this.step = 0;
        this.finished = false;
        this.timer = new Timer(next.delayMs(), e -> performStep());
        if (listener != null) {
            listener.onSimulationReset(environment, point());
            listener.onRunningStateChanged(false);
        }
    }

    public boolean isRunning() {
        return timer != null && timer.isRunning();
    }

    public void start() {
        if (environment == null || finished || isRunning()) return;
        timer.start();
        if (listener != null) listener.onRunningStateChanged(true);
    }

    public void pause() {
        if (timer != null) timer.stop();
        if (listener != null) listener.onRunningStateChanged(false);
    }

    public void toggleRunning() {
        if (isRunning()) pause();
        else start();
    }

    public void stepOnce() {
        if (!isRunning() && !finished && environment != null) performStep();
    }

    public void setDelay(int delayMs) {
        if (delayMs < 1) return;
        if (timer != null) {
            timer.setDelay(delayMs);
            timer.setInitialDelay(delayMs);
        }
    }

    private void performStep() {
        if (finished || environment == null) return;
        environment.step();
        step++;
        PopulationPoint now = point();
        if (listener != null) listener.onSimulationUpdated(environment, now);
        if (now.plants() == 0 || now.herbivores() == 0 || now.predators() == 0) {
            finish("Один из видов исчез.", now);
        } else if (step >= config.maxSteps()) {
            finish("Достигнут лимит шагов.", now);
        }
    }

    private PopulationPoint point() {
        return new PopulationPoint(step, environment.getPlantCount(),
                environment.getHerbivoreCount(), environment.getPredatorCount());
    }

    private void finish(String reason, PopulationPoint point) {
        if (finished) return;
        finished = true;
        pause();
        if (listener != null) listener.onSimulationFinished(reason, point);
    }
}
