public class Plant extends Agent {

    /*
     * Плотность растений смотрим в квадрате 5x5.
     */
    private static final int VIEW_RADIUS_DENSITY = 2;

    /*
     * Восстановительный бонус смотрим в 3x3.
     */
    private static final int VIEW_RADIUS_RECOVERY = 1;

    /*
     * Здесь сохраняются дробные части энергии:
     * 0.1, 0.2, 0.4 и т.д.
     */
    private double growthBuffer = 0.0;


    public Plant(
            int x,
            int y) {

        this(x, y, 75);
    }


    /*
     * Используется при размножении.
     */
    public Plant(
            int x,
            int y,
            int energy) {

        super(x, y, energy);
    }


    @Override
    public void act(
            Environment env) {

        /*
         * 1. Определяем прирост энергии.
         */
        double energyGain =
                calculateEnergyGain(env);


        /*
         * 2. Сохраняем дробную энергию.
         */
        growthBuffer += energyGain;


        /*
         * В обычную int energy переводим
         * только накопившуюся целую часть.
         */
        int wholeEnergy =
                (int) Math.floor(
                        growthBuffer
                );


        if (wholeEnergy > 0) {

            addEnergy(wholeEnergy);

            growthBuffer -= wholeEnergy;
        }


        /*
         * 3. Проверяем размножение.
         */
        if (energy >= env.getPlantReproduceThreshold()) {

            reproduce(env);
        }
    }


    /*
     * -------------------------------------------------
     * РОСТ В ЗАВИСИМОСТИ ОТ ПЛОТНОСТИ
     * -------------------------------------------------
     */
    private double calculateDensityEnergy(
            Environment env) {

        /*
         * Считается и само растение.
         *
         * Поэтому:
         * plantCount == 1
         * означает, что в 5x5 оно одно.
         */
        int plantCount =
                countPlantsInRadius(
                        env,
                        VIEW_RADIUS_DENSITY
                );


        if (plantCount <= 1) {
            return 3.0;
        }


        if (plantCount <= 3) {
            return 2.0;
        }


        if (plantCount <= 5) {
            return 1.0;
        }


        if (plantCount <= 7) {
            return 0.4;
        }


        if (plantCount == 8) {
            return 0.3;
        }


        if (plantCount == 9) {
            return 0.2;
        }


        return 0.1;
    }


    /*
     * -------------------------------------------------
     * БОНУС ВОССТАНОВЛЕНИЯ
     * -------------------------------------------------
     *
     * Если квадрат 3x3 вокруг растения
     * ещё не заполнен растениями,
     * рост увеличивается в 2 раза.
     */
    private double calculateRecoveryBonus(
            Environment env) {

        for (int dy = -VIEW_RADIUS_RECOVERY;
             dy <= VIEW_RADIUS_RECOVERY;
             dy++) {

            for (int dx = -VIEW_RADIUS_RECOVERY;
                 dx <= VIEW_RADIUS_RECOVERY;
                 dx++) {


                int nx = x + dx;
                int ny = y + dy;


                /*
                 * Клетки вне карты не считаются
                 * пустыми.
                 */
                if (!env.isValid(nx, ny)) {
                    continue;
                }


                /*
                 * Если внутри 3x3 есть хотя бы
                 * одна клетка, которая НЕ Plant,
                 * колония ещё не заполнена.
                 */
                if (!(env.getAgent(nx, ny)
                        instanceof Plant)) {

                    return 2.0;
                }
            }
        }


        /*
         * Вся доступная область 3x3
         * заполнена растениями.
         */
        return 1.0;
    }


    /*
     * Считаем количество растений
     * внутри заданного квадрата.
     */
    private int countPlantsInRadius(
            Environment env,
            int radius) {

        int count = 0;


        for (int dy = -radius;
             dy <= radius;
             dy++) {

            for (int dx = -radius;
                 dx <= radius;
                 dx++) {


                int nx = x + dx;
                int ny = y + dy;


                if (env.isValid(nx, ny)
                        && env.getAgent(nx, ny)
                        instanceof Plant) {

                    count++;
                }
            }
        }


        return count;
    }


    /*
     * Общая формула роста.
     */private double calculateEnergyGain(
    	        Environment env) {

    	    /*
    	     * Обычный прирост энергии,
    	     * зависящий от количества
    	     * Plant в квадрате 5x5.
    	     */
    	    double densityEnergy =
    	            calculateDensityEnergy(
    	                    env
    	            );


    	    /*
    	     * Бонус восстановления:
    	     *
    	     * если область 3x3 вокруг
    	     * растения ещё не заполнена,
    	     * получаем x2.
    	     */
    	    double recoveryBonus =
    	            calculateRecoveryBonus(
    	                    env
    	            );


    	    /*
    	     * Аварийный глобальный коэффициент,
    	     * когда растений во всей
    	     * экосистеме становится мало.
    	     */
    	    double emergencyRecovery =
    	            getEmergencyRecoveryMultiplier(
    	                    env
    	            );


    	    return densityEnergy
    	            * recoveryBonus
    	            * emergencyRecovery
    	            * env.getPlantGrowthRate();
    	}
    
    
    /*
     * -------------------------------------------------
     * РАЗМНОЖЕНИЕ
     * -------------------------------------------------
     *
     * Plant может размножиться
     * в любую из 8 соседних клеток.
     */
    private void reproduce(
            Environment env) {

        /*
         * -----------------------------------------
         * ЭТАП 1
         *
         * Находим минимальную плотность Plant
         * среди всех свободных соседних клеток.
         * -----------------------------------------
         */

        int minDensity =
                Integer.MAX_VALUE;

        boolean hasFreeCell = false;


        for (int dx = -1;
             dx <= 1;
             dx++) {

            for (int dy = -1;
                 dy <= 1;
                 dy++) {


                if (dx == 0
                        && dy == 0) {

                    continue;
                }


                int nx =
                        x + dx;

                int ny =
                        y + dy;


                if (!env.isValid(nx, ny)) {
                    continue;
                }


                /*
                 * Размножение только
                 * в соседнюю ПУСТУЮ клетку.
                 */
                if (env.getAgent(nx, ny) != null) {
                    continue;
                }


                hasFreeCell = true;


                /*
                 * Считаем плотность Plant
                 * вокруг потенциального ребёнка.
                 */
                int density =
                        countPlantsAroundCell(
                                env,
                                nx,
                                ny,
                                VIEW_RADIUS_DENSITY
                        );


                if (density < minDensity) {

                    minDensity =
                            density;
                }
            }
        }


        /*
         * Все 8 соседних клеток заняты
         * или находятся за границей мира.
         *
         * Энергию НЕ теряем.
         */
        if (!hasFreeCell) {
            return;
        }


        /*
         * -----------------------------------------
         * ЭТАП 2
         *
         * Выбираем случайно среди хороших клеток.
         *
         * Хорошая клетка:
         *
         * density == minDensity
         *
         * ИЛИ
         *
         * density == minDensity + 1
         *
         * Благодаря этому Plant
         * продолжает расширяться,
         * но не строит постоянно одну
         * длинную линию.
         * -----------------------------------------
         */

        int chosenX = -1;
        int chosenY = -1;

        int options = 0;


        for (int dx = -1;
             dx <= 1;
             dx++) {

            for (int dy = -1;
                 dy <= 1;
                 dy++) {


                if (dx == 0
                        && dy == 0) {

                    continue;
                }


                int nx =
                        x + dx;

                int ny =
                        y + dy;


                if (!env.isValid(nx, ny)) {
                    continue;
                }


                if (env.getAgent(nx, ny) != null) {
                    continue;
                }


                int density =
                        countPlantsAroundCell(
                                env,
                                nx,
                                ny,
                                VIEW_RADIUS_DENSITY
                        );


                /*
                 * Разрешаем:
                 *
                 * минимум
                 * или
                 * минимум + 1.
                 */
                if (density
                        > minDensity + 1) {

                    continue;
                }


                options++;


                /*
                 * Reservoir sampling:
                 *
                 * все подходящие клетки
                 * имеют одинаковый шанс.
                 */
                if (Math.random()
                        < 1.0 / options) {

                    chosenX = nx;
                    chosenY = ny;
                }
            }
        }


        /*
         * На всякий случай.
         */
        if (chosenX == -1) {
            return;
        }


        /*
         * Делим именно текущую
         * реальную энергию.
         */
        int childEnergy =
                energy / 2;


        energy -=
                childEnergy;


        env.addAgent(
                new Plant(
                        chosenX,
                        chosenY,
                        childEnergy
                )
        );
    }
    /*
     * Считает количество Plant
     * вокруг произвольной клетки.
     *
     * В отличие от countPlantsInRadius(),
     * центр здесь не обязательно
     * является текущим растением.
     *
     * Это нужно для оценки:
     * "насколько хорошим будет место
     * для ребёнка?"
     */
    private int countPlantsAroundCell(
            Environment env,
            int centerX,
            int centerY,
            int radius) {

        int count = 0;


        for (int dy = -radius;
             dy <= radius;
             dy++) {

            for (int dx = -radius;
                 dx <= radius;
                 dx++) {


                int nx =
                        centerX + dx;

                int ny =
                        centerY + dy;


                if (!env.isValid(nx, ny)) {
                    continue;
                }


                if (env.getAgent(nx, ny)
                        instanceof Plant) {

                    count++;
                }
            }
        }


        return count;
    }
    
    private double getEmergencyRecoveryMultiplier(
            Environment env) {

        int plantCount =
                env.getPlantCount();


        /*
         * Когда растений много —
         * никаких дополнительных бонусов.
         */
        if (plantCount >= 50) {
            return 1.0;
        }


        /*
         * Растений уже мало.
         */
        if (plantCount >= 5) {
            return 2.0;
        }


        /*
         * Растения находятся
         * в опасной зоне.
         */
        if (plantCount >= 5) {
            return 2.0;
        }


        /*
         * Почти полное вымирание.
         *
         * Последние 1–4 растения
         * получают сильный бонус,
         * чтобы успеть восстановить популяцию.
         */
        return 3.0;
    }
    
}
