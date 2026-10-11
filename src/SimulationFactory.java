import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Сборка мира перед запуском. Сначала перемешиваем свободные координаты. */
public final class SimulationFactory {
    private SimulationFactory() {}

    public static Environment create(SimulationConfig config) {
        config.validate();
        Environment env = new Environment(
                config.width(), config.height(),
                config.plantReproduceThreshold(),
                config.herbivoreReproduceThreshold(),
                config.predatorReproduceThreshold(),
                config.plantGrowthRate());

        List<Integer> cells = new ArrayList<>(config.width() * config.height());
        for (int i = 0; i < config.width() * config.height(); i++) {
            cells.add(i);
        }
        Collections.shuffle(cells);

        int offset = 0;
        for (int i = 0; i < config.initialPlants(); i++) {
            int cell = cells.get(offset++);
            env.addAgent(new Plant(cell % config.width(), cell / config.width(),
                    config.plantEnergy()));
        }
        for (int i = 0; i < config.initialHerbivores(); i++) {
            int cell = cells.get(offset++);
            env.addAgent(new Herbivore(cell % config.width(), cell / config.width(),
                    config.herbivoreEnergy()));
        }
        for (int i = 0; i < config.initialPredators(); i++) {
            int cell = cells.get(offset++);
            env.addAgent(new Predator(cell % config.width(), cell / config.width(),
                    config.predatorEnergy()));
        }
        return env;
    }
}
