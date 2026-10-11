
/**
 * Параметры нового запуска.
 * Не меняются в процессе эксперимента.
 */
public record SimulationConfig(
        int width,
        int height,
        int maxSteps,
        int delayMs,

        int initialPlants,
        int initialHerbivores,
        int initialPredators,

        int plantEnergy,
        int herbivoreEnergy,
        int predatorEnergy,

        int plantReproduceThreshold,
        int herbivoreReproduceThreshold,
        int predatorReproduceThreshold,

        double plantGrowthRate
) {

	public static SimulationConfig defaults() {
	    return new SimulationConfig(
	            80, 50,          // Ширина, высота
	            10000, 5,        // Макс. шагов, задержка (мс)

	            720, 24, 24,     // Количество P, H, X

	            75, 120, 350,   // Начальная энергия P, H, X

	            35, 110, 330,   // Пороги размножения P, H, X

	            3             // Скорость получения энергии P
	    );
	}

    public void validate() {

        if (width < 10 || height < 10
                || width > 250 || height > 250) {
            throw new IllegalArgumentException(
                    "Размеры поля должны быть от 10 до 250"
            );
        }

        if (maxSteps < 1 || delayMs < 1) {
            throw new IllegalArgumentException(
                    "Максимум шагов и задержка должны быть положительными"
            );
        }

        if (initialPlants < 1
                || initialHerbivores < 1
                || initialPredators < 1) {
            throw new IllegalArgumentException(
                    "При запуске должен присутствовать каждый из трёх видов"
            );
        }

        long count = (long) initialPlants
                + initialHerbivores
                + initialPredators;

        if (count > (long) width * height) {
            throw new IllegalArgumentException(
                    "Агентов больше, чем клеток поля"
            );
        }

        if (plantEnergy <= 0
                || herbivoreEnergy <= 0
                || predatorEnergy <= 0) {
            throw new IllegalArgumentException(
                    "Начальная энергия должна быть положительной"
            );
        }

        if (plantReproduceThreshold < 2
                || herbivoreReproduceThreshold < 2
                || predatorReproduceThreshold < 2) {
            throw new IllegalArgumentException(
                    "Порог размножения должен быть не меньше 2"
            );
        }

        if (!Double.isFinite(plantGrowthRate)
                || plantGrowthRate <= 0) {
            throw new IllegalArgumentException(
                    "Скорость роста растений должна быть больше 0"
            );
        }
    }
}
