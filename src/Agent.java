public abstract class Agent {
    protected int x, y;
    protected int energy;
    protected boolean alive;

    public Agent(int x, int y, int energy) {
        this.x = x;
        this.y = y;
        this.energy = energy;
        this.alive = true;
    }

    public abstract void act(Environment env);

    public boolean isAlive() {
        return alive && energy > 0;
    }

    public int getEnergy() {
        return energy;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    protected void addEnergy(int amount) {
        energy += amount;
    }

    protected void consumeEnergy(int amount) {
        energy -= amount;

        if (energy <= 0) {
            alive = false;
        }
    }

    public void markDead() {
        alive = false;
    }
}
