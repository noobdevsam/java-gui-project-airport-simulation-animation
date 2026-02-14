import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class RealisticFlightSim extends JFrame {
    public RealisticFlightSim() {
        setTitle("Precision Airport Simulation");
        setSize(1200, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new FlightEngine());
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RealisticFlightSim().setVisible(true));
    }
}

class FlightEngine extends JPanel implements ActionListener {
    private enum Phase { TAXI, TAKEOFF, CLIMB, DESCENT, FLARE, BRAKING }
    private Phase currentPhase = Phase.TAXI;

    // Physics constants
    private double x = -100, y = 480;
    private double velocity = 1.8;
    private double pitch = 0;      // Angle in degrees
    private double gearPos = 1.0;  // 1.0 = down, 0.0 = up
    private double propRotation = 0;
    private final int RUNWAY_Y = 480;

    public FlightEngine() {
        setBackground(new Color(173, 216, 230)); // Lighter Sky Blue
        new Timer(16, this).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawScene(g2d);
        drawAircraft(g2d);
        drawTelemetry(g2d);
    }

    private void drawScene(Graphics2D g2d) {
        // Ground & Runway
        g2d.setColor(new Color(40, 150, 40));
        g2d.fillRect(0, RUNWAY_Y + 20, getWidth(), getHeight());

        g2d.setColor(new Color(45, 45, 45));
        g2d.fillRect(0, RUNWAY_Y + 10, getWidth(), 60);

        // Runway center lines
        g2d.setColor(Color.YELLOW);
        for (int i = 0; i < getWidth(); i += 80) {
            g2d.fillRect(i, RUNWAY_Y + 38, 30, 4);
        }
    }

    private void drawAircraft(Graphics2D g2d) {
        AffineTransform old = g2d.getTransform();

        // 1. Move to aircraft center
        g2d.translate(x, y);

        // 2. Draw Shadow on the ground
        g2d.setColor(new Color(0, 0, 0, 50));
        double altitudeEffect = Math.max(0.4, 1.0 - (RUNWAY_Y - y) / 300.0);
        g2d.fillOval(-40, (int)(RUNWAY_Y - y + 45), (int)(100 * altitudeEffect), 10);

        // 3. Apply Aircraft Rotation (Pitch)
        g2d.rotate(Math.toRadians(pitch));

        // DRAWING THE PLANE (Centered at 0,0)
        // Fuselage
        g2d.setColor(Color.WHITE);
        g2d.fill(new RoundRectangle2D.Double(-50, -10, 100, 20, 15, 15));

        // Cockpit Window
        g2d.setColor(new Color(50, 50, 150));
        g2d.fill(new Arc2D.Double(25, -10, 20, 15, 0, 90, Arc2D.PIE));

        // Tail Fin
        g2d.setColor(new Color(200, 0, 0));
        Path2D tail = new Path2D.Double();
        tail.moveTo(-50, -10);
        tail.lineTo(-65, -35);
        tail.lineTo(-40, -10);
        tail.closePath();
        g2d.fill(tail);

        // Landing Gear
        g2d.setColor(Color.BLACK);
        if (gearPos > 0) {
            int gY = (int)(10 * gearPos);
            g2d.drawLine(-25, 10, -25, 10 + gY); // Rear gear
            g2d.drawLine(25, 10, 25, 10 + gY);   // Front gear
            g2d.fillOval(-28, 10 + gY, 6, 6);
            g2d.fillOval(22, 10 + gY, 6, 6);
        }

        // Propeller Hub & Blades
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(48, -5, 6, 10);
        g2d.setStroke(new BasicStroke(2));
        g2d.rotate(propRotation, 51, 0);
        g2d.drawLine(51, -20, 51, 20);

        g2d.setTransform(old);
    }

    private void drawTelemetry(Graphics2D g2d) {
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("FLIGHT PHASE: " + currentPhase, 20, 30);
        g2d.drawString(String.format("PITCH: %.1f°", -pitch), 20, 50);
        g2d.drawString(String.format("ALTITUDE: %.0f ft", (RUNWAY_Y - y) * 5), 20, 70);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        propRotation += velocity * 0.4;

        switch (currentPhase) {
            case TAXI:
                x += velocity;
                if (x > 150) currentPhase = Phase.TAKEOFF;
                break;

            case TAKEOFF:
                velocity += 0.05;
                x += velocity;
                if (velocity > 6) {
                    pitch = -10; // Nose up
                    currentPhase = Phase.CLIMB;
                }
                break;

            case CLIMB:
                x += velocity;
                y -= 2.0;
                if (gearPos > 0) gearPos -= 0.02; // Retract gear
                if (x > getWidth() + 100) resetForDescent();
                break;

            case DESCENT:
                x += velocity;
                y += 1.5;
                if (gearPos < 1.0) gearPos += 0.02; // Deploy gear
                pitch = 5; // Slight nose down for descent
                if (y > RUNWAY_Y - 40) currentPhase = Phase.FLARE;
                break;

            case FLARE:
                x += velocity;
                y += 0.5;
                pitch = -5; // PULL UP nose for smooth landing
                if (y >= RUNWAY_Y) {
                    y = RUNWAY_Y;
                    currentPhase = Phase.BRAKING;
                }
                break;

            case BRAKING:
                x += velocity;
                velocity -= 0.06;
                pitch = 0;
                if (velocity <= 1.8) {
                    velocity = 1.8;
                    if (x > getWidth() + 100) {
                        x = -150;
                        currentPhase = Phase.TAXI;
                    }
                }
                break;
        }
        repaint();
    }

    private void resetForDescent() {
        currentPhase = Phase.DESCENT;
        x = -150;
        y = 150;
        velocity = 5.0;
        pitch = 5;
    }
}