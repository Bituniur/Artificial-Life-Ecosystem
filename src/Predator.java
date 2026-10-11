import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

public class Predator extends Agent {

    /*
     * Сенсор 5x5.
     */
    private static final int VIEW_RADIUS = 2;

    /*
     * Расход энергии.
     */
    private static final int ENERGY_COST = 3;


    /*
     * Ортогональное движение.
     */
    private static final int[][] DIRECTIONS = {
            { 1,  0},
            { 0,  1},
            {-1,  0},
            { 0, -1}
    };


    /*
     * Состояние широкого поиска.
     */
    private int searchDirection = 0;

    private int searchStepsInDirection = 0;

    private int searchSegmentLength = 2;

    private int searchSegmentsAtCurrentLength = 0;
    
    /*
     * Короткая память о последней
     * замеченной добыче.
     */
    private static final int HERBIVORE_MEMORY_DURATION = 3;

    private int lastSeenHerbivoreX = -1;
    private int lastSeenHerbivoreY = -1;

    private int herbivoreMemorySteps = 0;


    public Predator(
            int x,
            int y) {

        this(x, y, 350);
    }


    public Predator(
            int x,
            int y,
            int energy) {

        super(x, y, energy);
    }

    @Override
    public void act(
            Environment env) {

        /*
         * 1. Расход энергии.
         */
        consumeEnergy(ENERGY_COST);

        if (!isAlive()) {
            return;
        }


        /*
         * 2. Смотрим, есть ли вообще
         * Herbivore в текущем квадрате 5x5.
         */
        int[] visibleHerbivore =
                findNearestVisibleHerbivore(env);


        /*
         * ПРИОРИТЕТ №1:
         * живая видимая добыча.
         */
        if (visibleHerbivore != null) {

            /*
             * Запоминаем последнее место,
             * где видели H.
             */
            lastSeenHerbivoreX =
                    visibleHerbivore[0];

            lastSeenHerbivoreY =
                    visibleHerbivore[1];

            herbivoreMemorySteps =
                    HERBIVORE_MEMORY_DURATION;


            resetSearch();


            /*
             * Сначала пытаемся найти
             * полноценный путь BFS
             * к любому достижимому H.
             */
            int[] step =
                    findStepTowardNearestReachableHerbivore(
                            env
                    );


            if (step != null) {

                moveAndEatHerbivore(
                        env,
                        step[0],
                        step[1]
                );

            } else {

                /*
                 * H виден, но полного пути
                 * внутри текущего 5x5 нет.
                 *
                 * НЕ ЗАБЫВАЕМ H.
                 *
                 * Делаем лучший возможный
                 * обходной шаг в его сторону.
                 */
                int[] fallback =
                        findBestStepTowardPoint(
                                env,
                                visibleHerbivore[0],
                                visibleHerbivore[1]
                        );


                if (fallback != null) {

                    moveAndEatHerbivore(
                            env,
                            fallback[0],
                            fallback[1]
                    );
                }
            }

        /*
         * ПРИОРИТЕТ №2:
         * H уже вышел из зрения,
         * но Predator ещё помнит,
         * куда он убежал.
         */
        } else if (herbivoreMemorySteps > 0) {

            int[] step =
                    findBestStepTowardPoint(
                            env,
                            lastSeenHerbivoreX,
                            lastSeenHerbivoreY
                    );


            if (step != null) {

                /*
                 * Здесь тоже используем
                 * общий метод движения.
                 *
                 * Если внезапно в клетке окажется H,
                 * он будет съеден.
                 */
                moveAndEatHerbivore(
                        env,
                        step[0],
                        step[1]
                );
            }


            herbivoreMemorySteps--;


            /*
             * Дошли в последнее известное место —
             * дальше преследовать старую координату
             * бессмысленно.
             */
            if (x == lastSeenHerbivoreX
                    && y == lastSeenHerbivoreY) {

                herbivoreMemorySteps = 0;
            }


        /*
         * ПРИОРИТЕТ №3:
         * добычи действительно нет.
         *
         * Ищем её и одновременно
         * рассредоточиваемся.
         */
        } else {

            wideSpiralSearch(env);
        }


        /*
         * Размножение после действия.
         */
        if (energy >= env.getPredatorReproduceThreshold()) {

            reproduce(env);
        }
    }
    
    /*
     * Ищем ближайшего ВИДИМОГО Herbivore.
     *
     * Здесь пока не важно,
     * можно ли до него полностью дойти.
     *
     * Главное — Predator знает,
     * что добыча существует.
     */
    private int[] findNearestVisibleHerbivore(
            Environment env) {

        int bestX = -1;
        int bestY = -1;

        int bestDistance =
                Integer.MAX_VALUE;


        for (int dx = -VIEW_RADIUS;
             dx <= VIEW_RADIUS;
             dx++) {

            for (int dy = -VIEW_RADIUS;
                 dy <= VIEW_RADIUS;
                 dy++) {


                if (dx == 0
                        && dy == 0) {

                    continue;
                }


                int nx = x + dx;
                int ny = y + dy;


                if (!env.isValid(nx, ny)) {
                    continue;
                }


                if (env.getAgent(nx, ny)
                        instanceof Herbivore) {


                    int distance =
                            Math.abs(dx)
                            + Math.abs(dy);


                    if (distance < bestDistance) {

                        bestDistance = distance;

                        bestX = nx;
                        bestY = ny;
                    }
                }
            }
        }


        if (bestX == -1) {
            return null;
        }


        return new int[] {
                bestX,
                bestY
        };
    }
    
    /*
     * Если полного пути BFS до цели сейчас нет,
     * всё равно делаем разумный шаг
     * в её направлении.
     *
     * В отличие от старой логики,
     * разрешаем временно не уменьшать расстояние.
     */
    private int[] findBestStepTowardPoint(
            Environment env,
            int targetX,
            int targetY) {

        int[] best = null;

        int bestDistance =
                Integer.MAX_VALUE;

        int equalOptions = 0;


        for (int[] dir : DIRECTIONS) {

            int nx = x + dir[0];
            int ny = y + dir[1];


            if (!env.isValid(nx, ny)) {
                continue;
            }


            Agent agent =
                    env.getAgent(nx, ny);


            /*
             * Можно идти:
             *
             * пусто       ✅
             * Herbivore   ✅
             *
             * Plant       ❌
             * Predator    ❌
             */
            if (agent != null
                    && !(agent instanceof Herbivore)) {

                continue;
            }


            int distance =
                    Math.abs(targetX - nx)
                    + Math.abs(targetY - ny);


            if (best == null
                    || distance < bestDistance) {

                bestDistance = distance;

                best =
                        new int[] {
                                nx,
                                ny
                        };

                equalOptions = 1;

            } else if (distance == bestDistance) {

                equalOptions++;


                /*
                 * Равнозначные обходы
                 * выбираем случайно.
                 */
                if (ThreadLocalRandom
                        .current()
                        .nextInt(equalOptions) == 0) {

                    best =
                            new int[] {
                                    nx,
                                    ny
                            };
                }
            }
        }


        return best;
    }
    
    /*
     * =================================================
     * BFS К БЛИЖАЙШЕМУ ДОСТИЖИМОМУ H
     * =================================================
     *
     * Всё выполняется только внутри
     * известного Predator квадрата 5x5.
     *
     * Пустота:
     * можно пройти.
     *
     * Herbivore:
     * цель.
     *
     * Plant:
     * препятствие.
     *
     * другой Predator:
     * препятствие.
     */
    private int[]
    findStepTowardNearestReachableHerbivore(
            Environment env) {

        int sensorSize =
                VIEW_RADIUS * 2 + 1;


        boolean[][] visited =
                new boolean[
                        sensorSize
                        ][
                        sensorSize
                        ];


        int baseX =
                x - VIEW_RADIUS;

        int baseY =
                y - VIEW_RADIUS;


        ArrayDeque<int[]> queue =
                new ArrayDeque<>();


        /*
         * Элемент:
         *
         * [0] текущий X
         * [1] текущий Y
         * [2] первый X маршрута
         * [3] первый Y маршрута
         */
        queue.add(
                new int[] {
                        x,
                        y,
                        x,
                        y
                }
        );


        visited[
                y - baseY
                ][
                x - baseX
                ] = true;


        while (!queue.isEmpty()) {

            int[] current =
                    queue.removeFirst();


            int currentX =
                    current[0];

            int currentY =
                    current[1];


            for (int[] dir : DIRECTIONS) {

                int nx =
                        currentX + dir[0];

                int ny =
                        currentY + dir[1];


                if (!env.isValid(nx, ny)) {
                    continue;
                }


                /*
                 * Зрение остаётся строго 5x5.
                 */
                if (!isInsideSensor(nx, ny)) {
                    continue;
                }


                int localX =
                        nx - baseX;

                int localY =
                        ny - baseY;


                if (visited[localY][localX]) {
                    continue;
                }


                visited[localY][localX] =
                        true;


                Agent target =
                        env.getAgent(nx, ny);


                /*
                 * Определяем первый шаг
                 * найденного маршрута.
                 */
                int firstX;
                int firstY;


                if (currentX == x
                        && currentY == y) {

                    firstX = nx;
                    firstY = ny;

                } else {

                    firstX =
                            current[2];

                    firstY =
                            current[3];
                }


                /*
                 * Нашли ближайшего
                 * достижимого H.
                 */
                if (target instanceof Herbivore) {

                    return new int[] {
                            firstX,
                            firstY
                    };
                }


                /*
                 * Любой другой агент —
                 * препятствие.
                 *
                 * В частности:
                 *
                 * Plant ❌
                 * Predator ❌
                 */
                if (target != null) {
                    continue;
                }


                /*
                 * Пустая клетка:
                 * продолжаем маршрут.
                 */
                queue.addLast(
                        new int[] {
                                nx,
                                ny,
                                firstX,
                                firstY
                        }
                );
            }
        }


        /*
         * Достижимого H внутри зрения нет.
         */
        return null;
    }


    /*
     * Находится ли клетка
     * внутри текущего сенсора 5x5.
     */
    private boolean isInsideSensor(
            int cellX,
            int cellY) {

        return Math.abs(cellX - x)
                <= VIEW_RADIUS
                &&
                Math.abs(cellY - y)
                <= VIEW_RADIUS;
    }


    /*
     * =================================================
     * ДВИЖЕНИЕ + УБИЙСТВО
     * =================================================
     *
     * Поедание теперь реализовано
     * ТОЛЬКО этим способом.
     *
     * X H
     *
     * становится:
     *
     * . X
     */
    private void moveAndEatHerbivore(
            Environment env,
            int nx,
            int ny) {

        Agent target =
                env.getAgent(nx, ny);


        if (target instanceof Herbivore) {

            /*
             * Получаем ВСЮ текущую
             * энергию травоядного.
             */
            addEnergy(
                    target.getEnergy()
            );


            /*
             * Освобождаем его клетку.
             */
            env.removeAgent(
                    nx,
                    ny
            );
        }


        /*
         * После убийства клетка свободна,
         * и Predator занимает её.
         */
        if (env.getAgent(nx, ny) == null) {

            env.moveAgent(
                    this,
                    nx,
                    ny
            );
        }
    }


    /*
     * =================================================
     * ШИРОКИЙ ПОИСК
     * =================================================
     */
    private void wideSpiralSearch(
            Environment env) {

        /*
         * Если направление выводит
         * за границу мира,
         * переходим к следующему сегменту.
         */
        for (int attempts = 0;
             attempts < 4;
             attempts++) {


            int[] dir =
                    DIRECTIONS[
                            searchDirection
                    ];


            int nx =
                    x + dir[0];

            int ny =
                    y + dir[1];


            /*
             * Граница мира.
             */
            if (!env.isValid(nx, ny)) {

                finishSearchSegment();

                keepSearchWithinWorld(env);

                continue;
            }


            /*
             * Основное направление свободно.
             */
            if (env.getAgent(nx, ny) == null) {

                env.moveAgent(
                        this,
                        nx,
                        ny
                );


                searchStepsInDirection++;


                if (searchStepsInDirection
                        >= searchSegmentLength) {

                    finishSearchSegment();
                }


                keepSearchWithinWorld(env);

                return;
            }


            /*
             * Основной путь временно перекрыт
             * Plant / Predator / H.
             *
             * Если это H,
             * BFS выше должен был его обработать.
             *
             * Поэтому здесь ищем обходной
             * свободный шаг.
             */
            int[] fallback =
                    findBestFreeSearchStep(
                            env
                    );


            if (fallback != null) {

                env.moveAgent(
                        this,
                        fallback[0],
                        fallback[1]
                );
            }


            /*
             * Обходной шаг НЕ считается
             * пройденным сегментом спирали.
             */
            return;
        }
    }


    /*
     * =================================================
     * ВТОРИЧНОЕ РАССЕИВАНИЕ X
     * =================================================
     *
     * Используется только когда основное
     * направление поиска заблокировано.
     *
     * То есть Predator НЕ прекращает поиск
     * только потому, что увидел другого X.
     *
     * Из нескольких свободных обходных клеток
     * выбираем ту, которая дальше
     * от ближайшего видимого Predator.
     */
    private int[] findBestFreeSearchStep(
            Environment env) {

        int[] best = null;

        int bestDistance =
                Integer.MIN_VALUE;

        int equalOptions = 0;


        for (int[] dir : DIRECTIONS) {

            int nx =
                    x + dir[0];

            int ny =
                    y + dir[1];


            if (!env.isValid(nx, ny)) {
                continue;
            }


            if (env.getAgent(nx, ny) != null) {
                continue;
            }


            int distance =
                    distanceToNearestVisiblePredator(
                            env,
                            nx,
                            ny
                    );


            if (best == null
                    || distance > bestDistance) {


                best =
                        new int[] {
                                nx,
                                ny
                        };


                bestDistance =
                        distance;

                equalOptions = 1;

            } else if (
                    distance == bestDistance) {


                equalOptions++;


                /*
                 * Полностью одинаковые варианты
                 * выбираем случайно.
                 */
                if (ThreadLocalRandom
                        .current()
                        .nextInt(equalOptions) == 0) {

                    best =
                            new int[] {
                                    nx,
                                    ny
                            };
                }
            }
        }


        return best;
    }


    /*
     * Расстояние от предполагаемой клетки
     * до ближайшего ДРУГОГО Predator,
     * которого текущий X видит в 5x5.
     */
    private int distanceToNearestVisiblePredator(
            Environment env,
            int fromX,
            int fromY) {

        int minDistance =
                Integer.MAX_VALUE;


        for (int dx = -VIEW_RADIUS;
             dx <= VIEW_RADIUS;
             dx++) {

            for (int dy = -VIEW_RADIUS;
                 dy <= VIEW_RADIUS;
                 dy++) {


                int predatorX =
                        x + dx;

                int predatorY =
                        y + dy;


                if (!env.isValid(
                        predatorX,
                        predatorY)) {

                    continue;
                }


                Agent agent =
                        env.getAgent(
                                predatorX,
                                predatorY
                        );


                /*
                 * Самого себя не учитываем.
                 */
                if (agent instanceof Predator
                        && agent != this) {


                    int distance =
                            Math.abs(
                                    fromX
                                    - predatorX
                            )
                            +
                            Math.abs(
                                    fromY
                                    - predatorY
                            );


                    if (distance < minDistance) {

                        minDistance =
                                distance;
                    }
                }
            }
        }


        return minDistance;
    }


    /*
     * Реальное окончание
     * одной стороны поисковой спирали.
     */
    private void finishSearchSegment() {

        searchStepsInDirection = 0;


        searchDirection =
                (searchDirection + 1) % 4;


        searchSegmentsAtCurrentLength++;


        if (searchSegmentsAtCurrentLength >= 2) {

            searchSegmentsAtCurrentLength = 0;

            searchSegmentLength += 2;
        }
    }


    /*
     * Ограничение размера
     * поисковой спирали.
     */
    private void keepSearchWithinWorld(
            Environment env) {

        if (searchSegmentLength
                > Math.max(
                        env.getWidth(),
                        env.getHeight()
                )) {

            resetSearch();
        }
    }


    private void resetSearch() {

        searchDirection = 0;

        searchStepsInDirection = 0;

        searchSegmentLength = 2;

        searchSegmentsAtCurrentLength = 0;
    }


    /*
     * =================================================
     * РАЗМНОЖЕНИЕ
     * =================================================
     */
    private void reproduce(
            Environment env) {

        for (int[] dir : DIRECTIONS) {

            int nx =
                    x + dir[0];

            int ny =
                    y + dir[1];


            if (env.isValid(nx, ny)
                    && env.getAgent(nx, ny) == null) {


                int childEnergy =
                        energy / 2;


                energy -= childEnergy;


                env.addAgent(
                        new Predator(
                                nx,
                                ny,
                                childEnergy
                        )
                );


                return;
            }
        }
    }
}
