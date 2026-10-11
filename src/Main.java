import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Оставляем стандартный стиль Swing.
            }
            SimulationController controller = new SimulationController();
            SimulationFrame frame = new SimulationFrame(controller);
            frame.setVisible(true);
        });
    }
}
