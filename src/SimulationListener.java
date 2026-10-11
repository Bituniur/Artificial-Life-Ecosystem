public interface SimulationListener {
    void onSimulationReset(Environment env, PopulationPoint point);
    void onSimulationUpdated(Environment env, PopulationPoint point);
    void onRunningStateChanged(boolean running);
    void onSimulationFinished(String reason, PopulationPoint point);
}
