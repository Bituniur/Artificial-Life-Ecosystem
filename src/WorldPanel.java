import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Отрисовка текущего состояния поля. Никакой логики агентов внутри панели нет. */
public class WorldPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private Environment environment;

    public WorldPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(920, 530));
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (environment == null) {
                g.setColor(Color.DARK_GRAY);
                g.drawString("Создайте симуляцию", 20, 30);
                return;
            }
            int w = environment.getWidth();
            int h = environment.getHeight();
            int margin = 12;
            double cell = Math.min((getWidth() - 2.0 * margin) / w,
                    (getHeight() - 2.0 * margin) / h);
            if (cell <= 0) return;
            int fieldW = (int) Math.round(w * cell);
            int fieldH = (int) Math.round(h * cell);
            int ox = (getWidth() - fieldW) / 2;
            int oy = (getHeight() - fieldH) / 2;

            g.setColor(new Color(247, 250, 247));
            g.fillRect(ox, oy, fieldW, fieldH);
            if (cell >= 6) {
                g.setColor(new Color(222, 228, 223));
                for (int x = 0; x <= w; x++) {
                    int px = ox + (int) Math.round(x * cell);
                    g.drawLine(px, oy, px, oy + fieldH);
                }
                for (int y = 0; y <= h; y++) {
                    int py = oy + (int) Math.round(y * cell);
                    g.drawLine(ox, py, ox + fieldW, py);
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Agent a = environment.getAgent(x, y);
                    if (a == null || !a.isAlive()) continue;
                    int px = ox + (int) Math.round(x * cell);
                    int py = oy + (int) Math.round(y * cell);
                    int pw = Math.max(1, ox + (int) Math.round((x + 1) * cell) - px);
                    int ph = Math.max(1, oy + (int) Math.round((y + 1) * cell) - py);
                    if (a instanceof Plant) {
                        g.setColor(new Color(63, 163, 75));
                        g.fillRect(px + 1, py + 1, Math.max(1, pw - 2), Math.max(1, ph - 2));
                    } else if (a instanceof Herbivore) {
                        g.setColor(new Color(55, 108, 225));
                        g.fillOval(px + 1, py + 1, Math.max(1, pw - 2), Math.max(1, ph - 2));
                    } else if (a instanceof Predator) {
                        g.setColor(new Color(223, 66, 60));
                        g.fillOval(px + 1, py + 1, Math.max(1, pw - 2), Math.max(1, ph - 2));
                    }
                }
            }
            g.setColor(new Color(130, 130, 130));
            g.drawRect(ox, oy, fieldW, fieldH);
        } finally {
            g.dispose();
        }
    }
}
