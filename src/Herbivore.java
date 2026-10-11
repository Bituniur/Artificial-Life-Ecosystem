import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

public class Herbivore extends Agent {

    private static final int ENERGY_COST = 1;

    /*
     * Радиус 2 = квадрат зрения 5x5.
     */
    private static final int VIEW_RADIUS = 2;


    /*
     * Только ортогональное движение.
     */
    private static final int[][] DIRECTIONS = {
            { 1,  0},
            { 0,  1},
            {-1,  0},
            { 0, -1}
    };


    /*
     * Состояние широкого поиска.
     *
     * 2, 2,
     * 4, 4,
     * 6, 6...
     */
    private int searchDirection = 0;

    private int searchStepsInDirection = 0;

    private int searchSegmentLength = 2;

    private int searchSegmentsAtCurrentLength = 0;


    public Herbivore(
            int x,
            int y) {

        this(x, y, 120);
    }


    public Herbivore(
            int x,
            int y,
            int energy) {

        super(x, y, energy);
    }


    @Override
    public void act(
            Environment env) {

        /*
         * ---------------------------------------------
         * 1. РАСХОД ЭНЕРГИИ
         * ---------------------------------------------
         */
        consumeEnergy(ENERGY_COST);


        if (!isAlive()) {
            return;
        }


        /*
         * ---------------------------------------------
         * 2. ПРИОРИТЕТ №1 — ОПАСНОСТЬ
         * ---------------------------------------------
         *
         * Если ХОТЯ БЫ ОДИН Predator
         * находится внутри всего квадрата 5x5,
         * начинаем убегать.
         */
        if (hasVisiblePredator(env)) {

            resetSearch();

            fleeFromPredators(env);


            /*
             * Размножение проверяется
             * после действия.
             */
            if (energy >= env.getHerbivoreReproduceThreshold()) {

                reproduce(env);
            }


            return;
        }


        /*
         * ---------------------------------------------
         * 3. ПРИОРИТЕТ №2 — ЕДА
         * ---------------------------------------------
         *
         * BFS ищет не просто ближайший Plant
         * по Manhattan,
         * а ближайший ДОСТИЖИМЫЙ Plant
         * внутри видимых 5x5.
         */
        int[] stepToPlant =
                findStepTowardNearestReachablePlant(
                        env
                );


        if (stepToPlant != null) {

            resetSearch();


            moveAndEatPlant(
                    env,
                    stepToPlant[0],
                    stepToPlant[1]
            );

        } else {

            /*
             * -----------------------------------------
             * 4. ПРИОРИТЕТ №3 — ПОИСК
             * -----------------------------------------
             */
            wideSpiralSearch(env);
        }


        /*
         * ---------------------------------------------
         * 5. РАЗМНОЖЕНИЕ
         * ---------------------------------------------
         */
        if (energy >= env.getHerbivoreReproduceThreshold()) {

            reproduce(env);
        }
    }


    /*
     * =================================================
     * ЗРЕНИЕ ХИЩНИКА
     * =================================================
     *
     * Важно:
     * здесь НЕ используется условие Manhattan <= 2.
     *
     * Любая клетка внутри квадрата 5x5
     * является видимой.
     */
    private boolean hasVisiblePredator(
            Environment env) {

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


                if (env.isValid(nx, ny)
                        && env.getAgent(nx, ny)
                        instanceof Predator) {

                    return true;
                }
            }
        }


        return false;
    }


    /*
     * =================================================
     * ПОБЕГ
     * =================================================
     *
     * Выбираем клетку,
     * максимально удалённую
     * от ближайшего видимого Predator.
     *
     * При равной безопасности:
     *
     * 1. лучше двигаться, чем стоять;
     * 2. лучше клетка с Plant;
     * 3. затем случайный выбор.
     */
    
    private void fleeFromPredators(
            Environment env) {

        /*
         * Насколько безопасна текущая клетка.
         */
        int currentSafety =
                distanceToNearestVisiblePredator(
                        env,
                        x,
                        y
                );


        int bestX = x;
        int bestY = y;

        int bestSafety =
                Integer.MIN_VALUE;

        int bestEscapeOptions =
                Integer.MIN_VALUE;

        boolean bestHasPlant = false;

        boolean foundMove = false;

        int equalOptions = 0;


        /*
         * Проверяем четыре соседние клетки.
         */
        for (int[] dir : DIRECTIONS) {

            int nx = x + dir[0];
            int ny = y + dir[1];


            if (!env.isValid(nx, ny)) {
                continue;
            }


            Agent target =
                    env.getAgent(nx, ny);


            /*
             * H может перейти:
             *
             * пустота     ✅
             * Plant       ✅
             *
             * Herbivore   ❌
             * Predator    ❌
             */
            if (target != null
                    && !(target instanceof Plant)) {

                continue;
            }


            /*
             * Главный критерий:
             * расстояние до ближайшего
             * видимого Predator.
             */
            int safety =
                    distanceToNearestVisiblePredator(
                            env,
                            nx,
                            ny
                    );


            /*
             * Если клетка опаснее текущей,
             * туда не идём.
             */
            if (safety < currentSafety) {
                continue;
            }


            /*
             * Вторичный критерий:
             * сколько вариантов движения
             * останется на следующем ходу.
             *
             * Это позволяет H не лезть
             * добровольно в тупик,
             * но не даёт ему анализировать
             * всю карту.
             */
            int escapeOptions =
                    countEscapeOptions(
                            env,
                            nx,
                            ny
                    );


            boolean hasPlant =
                    target instanceof Plant;


            boolean better = false;


            /*
             * 1. Дальше от ближайшего X.
             */
            if (!foundMove
                    || safety > bestSafety) {

                better = true;


            /*
             * 2. При равной безопасности —
             * больше пространства для побега.
             */
            } else if (safety == bestSafety
                    && escapeOptions
                    > bestEscapeOptions) {

                better = true;


            /*
             * 3. Если всё одинаково —
             * предпочитаем Plant.
             */
            } else if (safety == bestSafety
                    && escapeOptions
                    == bestEscapeOptions
                    && hasPlant
                    && !bestHasPlant) {

                better = true;
            }


            if (better) {

                foundMove = true;

                bestSafety = safety;

                bestEscapeOptions =
                        escapeOptions;

                bestHasPlant =
                        hasPlant;

                bestX = nx;
                bestY = ny;

                equalOptions = 1;


            } else if (
                    foundMove
                    && safety == bestSafety
                    && escapeOptions
                    == bestEscapeOptions
                    && hasPlant == bestHasPlant) {


                /*
                 * Полностью одинаковые варианты
                 * выбираем случайно.
                 */
                equalOptions++;


                if (ThreadLocalRandom
                        .current()
                        .nextInt(equalOptions) == 0) {

                    bestX = nx;
                    bestY = ny;
                }
            }
        }


        /*
         * Либо всё заблокировано,
         * либо любой ход делает положение хуже.
         */
        if (!foundMove) {
            return;
        }


        Agent target =
                env.getAgent(
                        bestX,
                        bestY
                );


        /*
         * H может во время побега
         * съесть растение.
         */
        if (target instanceof Plant) {

            addEnergy(
                    target.getEnergy()
            );


            env.removeAgent(
                    bestX,
                    bestY
            );
        }


        env.moveAgent(
                this,
                bestX,
                bestY
        );
    }
    
    /*
     * Для предполагаемой клетки
     * считаем Manhattan-расстояние
     * до ближайшего Predator,
     * который сейчас видим.
     */
    
    private int distanceToNearestVisiblePredator(
            Environment env,
            int fromX,
            int fromY) {

        int minDistance =
                Integer.MAX_VALUE;


        /*
         * Смотрим только Predator,
         * которых H видит из своей
         * текущей позиции в квадрате 5x5.
         */
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


                if (env.getAgent(
                        predatorX,
                        predatorY)
                        instanceof Predator) {


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


        /*
         * Этот метод вызывается во время
         * побега, поэтому Predator обычно
         * существует.
         *
         * 10 просто означает:
         * "видимой угрозы нет".
         */
        if (minDistance
                == Integer.MAX_VALUE) {

            return 10;
        }


        return minDistance;
    }
    
    /*
     * =================================================
     * BFS К БЛИЖАЙШЕМУ ДОСТИЖИМОМУ PLANT
     * =================================================
     *
     * Поиск идёт ТОЛЬКО внутри текущего
     * сенсора 5x5.
     *
     * Пустота:
     * можно пройти.
     *
     * Plant:
     * цель.
     *
     * H/X:
     * препятствия.
     *
     * Возвращается только ПЕРВЫЙ шаг пути.
     */
    private int[] findStepTowardNearestReachablePlant(
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
         * Элемент очереди:
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
                 * Не позволяем BFS
                 * выйти за сенсор 5x5.
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
                 * Определяем первый шаг пути.
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
                 * Нашли ближайший
                 * достижимый Plant.
                 */
                if (target instanceof Plant) {

                    return new int[] {
                            firstX,
                            firstY
                    };
                }


                /*
                 * Через других агентов
                 * проходить нельзя.
                 */
                if (target != null) {
                    continue;
                }


                /*
                 * Пустая клетка —
                 * продолжаем BFS.
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
         * Достижимых растений
         * в сенсоре нет.
         */
        return null;
    }


    /*
     * Проверка:
     * находится ли клетка
     * внутри квадратного сенсора 5x5.
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
     * ДВИЖЕНИЕ К PLANT + ПОЕДАНИЕ
     * =================================================
     */
    private void moveAndEatPlant(
            Environment env,
            int nx,
            int ny) {

        Agent target =
                env.getAgent(nx, ny);


        if (target instanceof Plant) {

            addEnergy(
                    target.getEnergy()
            );


            env.removeAgent(
                    nx,
                    ny
            );
        }


        /*
         * После удаления Plant
         * клетка становится свободной.
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
     *
     * Нормальный маршрут:
     *
     * 2 →
     * 2 ↓
     * 4 ←
     * 4 ↑
     * 6 →
     * ...
     *
     * ВАЖНО:
     *
     * агент-препятствие больше НЕ считается
     * завершением всего сегмента.
     */
    
    private void wideSpiralSearch(
            Environment env) {

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
             * Если дошли до края карты,
             * действительно заканчиваем
             * текущий сегмент.
             */
            if (!env.isValid(nx, ny)) {

                finishSearchSegment();

                keepSearchWithinWorld(env);

                continue;
            }


            /*
             * Клетка свободна —
             * идём по поисковой спирали.
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
             * Если направление временно
             * перекрыто другим агентом,
             * НЕ считаем весь сегмент законченным.
             *
             * Просто делаем свободный обходной шаг.
             */
            int[] fallback =
                    findRandomFreeSearchStep(
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
             * Обходной шаг не меняет
             * состояние поисковой спирали.
             */
            return;
        }
    }
    
    /*
     * Если выбранное направление временно
     * заблокировано агентом,
     * ищем любую свободную соседнюю клетку.
     */
    private int[] findRandomFreeSearchStep(
            Environment env) {

        int[] chosen = null;

        int options = 0;


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


            options++;


            /*
             * Reservoir sampling:
             * равные свободные варианты
             * выбираются случайно.
             */
            if (ThreadLocalRandom
                    .current()
                    .nextInt(options) == 0) {

                chosen =
                        new int[] {
                                nx,
                                ny
                        };
            }
        }


        return chosen;
    }


    /*
     * Реальное завершение
     * одного сегмента спирали.
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
     * Не позволяем длине спирали
     * бесконечно превышать размеры мира.
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
                        new Herbivore(
                                nx,
                                ny,
                                childEnergy
                        )
                );


                return;
            }
        }
    }
    
    /*
     * Поиск и рассредоточение теперь
     * являются ОДНИМ поведением.
     *
     * Predator не "бросает поиск",
     * чтобы убежать от Plant/X.
     *
     * Он просто выбирает более хороший
     * поисковый шаг.
     */
    
   
    /*
     * Сумма расстояний до всех Predator,
     * которых H сейчас видит.
     *
     * Используется только как
     * вторичный критерий побега.
     */
    /*
     * Сколько вариантов движения останется
     * у Herbivore после перехода
     * в предполагаемую клетку.
     *
     * Чем больше — тем меньше шанс
     * самому загнать себя в угол.
     */
    private int countEscapeOptions(
            Environment env,
            int fromX,
            int fromY) {

        int count = 0;


        for (int[] dir : DIRECTIONS) {

            int nx =
                    fromX + dir[0];

            int ny =
                    fromY + dir[1];


            if (!env.isValid(nx, ny)) {
                continue;
            }


            /*
             * Текущая клетка H после движения
             * освободится.
             */
            if (nx == x
                    && ny == y) {

                count++;

                continue;
            }


            Agent target =
                    env.getAgent(nx, ny);


            /*
             * В будущем H сможет уйти:
             *
             * в пустоту
             * или через Plant.
             */
            if (target == null
                    || target instanceof Plant) {

                count++;
            }
        }


        return count;
    }
    
    
    
}
