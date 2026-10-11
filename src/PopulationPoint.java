/** Одна запись для графика: число агентов каждого вида на заданном шаге. */
public record PopulationPoint(int step, int plants, int herbivores, int predators) {}
