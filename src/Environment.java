import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Environment {

    private final int plantReproduceThreshold;
    private final int herbivoreReproduceThreshold;
    private final int predatorReproduceThreshold;
    private final double plantGrowthRate;


    private int width, height;

    private Agent[][] grid;
    private List<Agent> agents;

    public Environment(int width, int height) {
    	this(width, height, 70, 180, 260, 1.0);
    }

    public Environment(
            int width, int height,
            int plantReproduceThreshold,
            int herbivoreReproduceThreshold,
            int predatorReproduceThreshold,
            double plantGrowthRate) {

        if (width <= 0 || height <= 0 ||
                plantReproduceThreshold < 2 ||
                herbivoreReproduceThreshold < 2 ||
                predatorReproduceThreshold < 2) {
            throw new IllegalArgumentException("Некорректные параметры среды");
        }
        
        if (!Double.isFinite(plantGrowthRate) || plantGrowthRate <= 0) {
            throw new IllegalArgumentException("Некорректная скорость роста");
        }

        this.plantGrowthRate = plantGrowthRate;
        this.plantReproduceThreshold = plantReproduceThreshold;
        this.herbivoreReproduceThreshold = herbivoreReproduceThreshold;
        this.predatorReproduceThreshold = predatorReproduceThreshold;
        this.width = width;
        this.height = height;

        grid =
                new Agent[height][width];

        agents =
                new ArrayList<>();
    }

    public boolean isValid(
            int x,
            int y) {

        return x >= 0
                && x < width
                && y >= 0
                && y < height;
    }

    public Agent getAgent(
            int x,
            int y) {

        if (isValid(x, y)) {
            return grid[y][x];
        }

        return null;
    }

    public void addAgent(
            Agent agent) {

        if (agent == null) {
            return;
        }

        if (!isValid(
                agent.getX(),
                agent.getY())) {

            return;
        }

        /*
         * Защита от случайного
         * перезаписывания занятой клетки.
         */
        if (grid[
                agent.getY()
                ][
                agent.getX()
                ] != null) {

            return;
        }

        agents.add(agent);

        grid[
                agent.getY()
                ][
                agent.getX()
                ] = agent;
    }

    public void removeAgent(
            int x,
            int y) {

        if (!isValid(x, y)) {
            return;
        }

        Agent agent =
                grid[y][x];

        if (agent != null) {
            removeAgent(agent);
        }
    }

    /*
     * Полное удаление:
     * - из grid;
     * - из agents;
     * - alive = false.
     */
    private void removeAgent(
            Agent agent) {

        if (agent == null) {
            return;
        }

        int ax = agent.getX();
        int ay = agent.getY();

        if (isValid(ax, ay)
                && grid[ay][ax] == agent) {

            grid[ay][ax] = null;
        }

        agents.remove(agent);

        agent.markDead();
    }

    public void moveAgent(
            Agent agent,
            int nx,
            int ny) {

        if (agent == null
                || !agent.isAlive()) {

            return;
        }

        if (!isValid(nx, ny)) {
            return;
        }

        /*
         * Сюда передаётся только уже
         * освобождённая клетка.
         */
        if (grid[ny][nx] != null) {
            return;
        }

        grid[
                agent.getY()
                ][
                agent.getX()
                ] = null;

        agent.setX(nx);
        agent.setY(ny);

        grid[ny][nx] = agent;
    }

    public void printCompact() {

        char[][] display =
                new char[height][width];

        for (int y = 0;
             y < height;
             y++) {

            for (int x = 0;
                 x < width;
                 x++) {

                display[y][x] = '.';
            }
        }

        for (Agent agent : agents) {

            if (!agent.isAlive()) {
                continue;
            }

            if (agent instanceof Plant) {

                display[
                        agent.getY()
                        ][
                        agent.getX()
                        ] = 'P';

            } else if (
                    agent instanceof Herbivore) {

                display[
                        agent.getY()
                        ][
                        agent.getX()
                        ] = 'H';

            } else if (
                    agent instanceof Predator) {

                display[
                        agent.getY()
                        ][
                        agent.getX()
                        ] = 'X';
            }
        }

        for (int y = 0;
             y < height;
             y++) {

            for (int x = 0;
                 x < width;
                 x++) {

                System.out.print(
                        display[y][x]
                );
            }

            System.out.println();
        }
    }

    public int getPlantCount() {

        return (int) agents
                .stream()
                .filter(
                        a ->
                        a instanceof Plant
                        && a.isAlive()
                )
                .count();
    }

    public int getHerbivoreCount() {

        return (int) agents
                .stream()
                .filter(
                        a ->
                        a instanceof Herbivore
                        && a.isAlive()
                )
                .count();
    }

    public int getPredatorCount() {

        return (int) agents
                .stream()
                .filter(
                        a ->
                        a instanceof Predator
                        && a.isAlive()
                )
                .count();
    }

    /*
     * Один глобальный шаг:
     *
     * 1. Plant
     * 2. Herbivore
     * 3. Predator
     *
     * Копия создаётся один раз в начале шага,
     * поэтому новорождённые уже существуют
     * на поле, но сами в этот тик не действуют.
     */
    public void step() {

        // Снимок всех агентов, которые существовали
        // в начале этого глобального шага.
        // Новорождённые в этом же шаге сами ходить не будут.
        List<Agent> currentAgents =
                new ArrayList<>(agents);

        // 1. Сначала всегда действуют растения.
        runPhase(
                currentAgents,
                Plant.class
        );

        // 2. Затем травоядные и хищники
        // действуют в случайном порядке.
        runRandomAnimalPhase(currentAgents);

        // 3. На всякий случай очищаем умерших.
        cleanupDeadAgents();
    }

    private void runPhase(
            List<Agent> currentAgents,
            Class<? extends Agent> type) {

        for (Agent agent : currentAgents) {

            if (!type.isInstance(agent)) {
                continue;
            }

            /*
             * Агент мог быть съеден
             * более ранней фазой.
             */
            if (!agent.isAlive()) {
                continue;
            }

            if (!isAgentOnGrid(agent)) {
                continue;
            }

            agent.act(this);

            /*
             * Если агент умер от голода
             * во время своего act(),
             * сразу очищаем его клетку.
             */
            if (!agent.isAlive()) {
                removeAgent(agent);
            }
        }
    }
    
    private void runRandomAnimalPhase(
            List<Agent> currentAgents) {

        List<Agent> animals =
                new ArrayList<>();

        // Берём только животных,
        // которые существовали в начале шага.
        for (Agent agent : currentAgents) {

            if (agent instanceof Herbivore
                    || agent instanceof Predator) {

                animals.add(agent);
            }
        }

        // Каждый глобальный шаг порядок будет новый.
        Collections.shuffle(animals);

        for (Agent agent : animals) {

            // Агент мог уже быть съеден
            // другим животным раньше в этом же шаге.
            if (!agent.isAlive()) {
                continue;
            }

            // Дополнительная проверка:
            // объект действительно ещё находится на карте.
            if (!isAgentOnGrid(agent)) {
                continue;
            }

            agent.act(this);

            // Если умер от голода во время своего хода,
            // полностью удаляем его из среды.
            if (!agent.isAlive()) {
                removeAgent(agent);
            }
        }
    }

    private boolean isAgentOnGrid(
            Agent agent) {

        int ax = agent.getX();
        int ay = agent.getY();

        return isValid(ax, ay)
                && grid[ay][ax] == agent;
    }

    /*
     * Дополнительная страховочная очистка.
     */
    private void cleanupDeadAgents() {

        List<Agent> deadAgents =
                new ArrayList<>();

        for (Agent agent : agents) {

            if (!agent.isAlive()) {
                deadAgents.add(agent);
            }
        }

        for (Agent dead : deadAgents) {
            removeAgent(dead);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPlantReproduceThreshold() {
        return plantReproduceThreshold;
    }

    public int getHerbivoreReproduceThreshold() {
        return herbivoreReproduceThreshold;
    }

    public int getPredatorReproduceThreshold() {
        return predatorReproduceThreshold;
    }
    
    public double getPlantGrowthRate() {
        return plantGrowthRate;
    }
}
