import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/** Линейный график P/H/X во времени. Это график статистики, не UML. */
public class PopulationChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final List<PopulationPoint> points = new ArrayList<>();

    public PopulationChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(920, 230));
    }

    public void reset(PopulationPoint first) {
        points.clear();
        points.add(first);
        repaint();
    }

    public void addPoint(PopulationPoint next) {
        points.add(next);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 58, top = 30, right = 22, bottom = 40;
            int chartW = Math.max(1, getWidth() - left - right);
            int chartH = Math.max(1, getHeight() - top - bottom);
            g.setColor(new Color(100, 100, 100));
            g.drawLine(left, top, left, top + chartH);
            g.drawLine(left, top + chartH, left + chartW, top + chartH);
            if (points.isEmpty()) return;

            int maxStep = Math.max(1, points.get(points.size() - 1).step());
            int maxValue = 1;
            for (PopulationPoint p : points) {
                maxValue = Math.max(maxValue, Math.max(p.plants(),
                        Math.max(p.herbivores(), p.predators())));
            }
            for (int i = 0; i <= 4; i++) {
                int py = top + chartH - i * chartH / 4;
                g.setColor(new Color(233, 237, 239));
                g.drawLine(left + 1, py, left + chartW, py);
                g.setColor(new Color(80, 80, 80));
                g.drawString(Integer.toString(i * maxValue / 4), 4, py + 5);
            }
            g.drawString("0", left - 4, top + chartH + 17);
            String last = Integer.toString(maxStep);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(last, left + chartW - fm.stringWidth(last),
                    top + chartH + 17);
            g.drawString("Шаги", left + chartW / 2 - 15,
                    top + chartH + 34);

            // Для 20 000 шагов рисуем не больше ~2 точек на пиксель по оси X.
            int stride = Math.max(1, points.size() / Math.max(1, chartW * 2));
            drawSeries(g, left, top, chartW, chartH, maxStep, maxValue,
                    stride, new Color(63, 163, 75), 0);
            drawSeries(g, left, top, chartW, chartH, maxStep, maxValue,
                    stride, new Color(55, 108, 225), 1);
            drawSeries(g, left, top, chartW, chartH, maxStep, maxValue,
                    stride, new Color(223, 66, 60), 2);
            drawLegend(g, left + 8, 13, new Color(63, 163, 75), "P");
            drawLegend(g, left + 78, 13, new Color(55, 108, 225), "H");
            drawLegend(g, left + 148, 13, new Color(223, 66, 60), "X");
        } finally {
            g.dispose();
        }
    }

    private void drawLegend(Graphics2D g, int x, int y, Color color, String text) {
        g.setColor(color);
        g.fillRect(x, y - 9, 16, 9);
        g.setColor(Color.DARK_GRAY);
        g.drawString(text, x + 22, y);
    }

    private void drawSeries(Graphics2D g, int left, int top, int w, int h,
            int maxStep, int maxValue, int stride, Color color, int species) {
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(color);
        int oldX = -1, oldY = -1;
        for (int i = 0; i < points.size(); i += stride) {
            PopulationPoint p = points.get(i);
            int x = left + (int) Math.round((double) p.step() * w / maxStep);
            int value = switch (species) {
                case 0 -> p.plants();
                case 1 -> p.herbivores();
                default -> p.predators();
            };
            int y = top + h - (int) Math.round((double) value * h / maxValue);
            if (oldX >= 0) g.drawLine(oldX, oldY, x, y);
            oldX = x;
            oldY = y;
        }
        PopulationPoint p = points.get(points.size() - 1);
        int x = left + (int) Math.round((double) p.step() * w / maxStep);
        int value = species == 0 ? p.plants() : species == 1 ? p.herbivores() : p.predators();
        int y = top + h - (int) Math.round((double) value * h / maxValue);
        if (oldX >= 0) g.drawLine(oldX, oldY, x, y);
    }
}
