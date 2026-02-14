import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class ProAirportSimulation extends JFrame {
    public ProAirportSimulation() {
        setTitle("Professional Flight Dynamics Simulation");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new FlightPanel());
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProAirportSimulation().setVisible(true));
    }
}

class FlightPanel extends JPanel implements ActionListener {
    private enum FlightState { TAXI, TAKEOFF_ROLL, ROTATION, CLIMB, LANDING, BRAKING }
    private FlightState state = FlightState.TAXI;

    // Physics Variables
    private double x = -150, y = 500;
    private double velocity = 1.5;
    private double altitude = 0;
    private double pitch = 0;
    private double gearLevel = 1.0; // 1.0 = down, 0.0 = retracted
    private double propAngle = 0;
    private final double GROUND_Y = 500;

    public FlightPanel() {
        setBackground(new Color(135, 206, 235)); // Sky Blue
        new Timer(16, this).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawEnvironment(g2d);
        drawAircraftSystem(g2d);
        drawUI(g2d);
    }

    private void drawEnvironment(Graphics2D g2d) {
        // Grass and Runway
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 500, getWidth(), getHeight() - 500);

        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(0, 510, getWidth(), 100);

        // Runway center lines
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < getWidth(); i += 100) {
            g2d.fillRect(i + 40, 555, 40, 5);
        }
    }

    private void drawAircraftSystem(Graphics2D g2d) {
        // 1. Draw Shadow (moves with X, but stays on GROUND_Y)
        g2d.setColor(new Color(0, 0, 0, 60));
        double shadowScale = 1.0 - (altitude / 500.0);
        g2d.fillOval((int)x, 580, (int)(100 * shadowScale), (int)(20 * shadowScale));

        // 2. Setup Aircraft Transform
        AffineTransform old = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(pitch), 80, 20); // Rotate around center of mass

        drawPlaneBody(g2d);
        g2d.setTransform(old);
    }

    private void drawPlaneBody(Graphics2D g2d) {
        // Landing Gear (Retracts based on gearLevel)
        if (gearLevel > 0) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(20, (int)(25 * gearLevel), 5, (int)(15 * gearLevel));
            g2d.fillRect(60, (int)(25 * gearLevel), 5, (int)(15 * gearLevel));
        }

        // Fuselage
        g2d.setColor(Color.WHITE);
        g2d.fill(new RoundRectangle2D.Double(0, 0, 120, 30, 15, 15));

        // Tail
        Path2D tail = new Path2D.Double();
        tail.moveTo(100, 0);
        tail.lineTo(120, -25);
        tail.lineTo(120, 0);
        tail.closePath();
        g2d.fill(tail);

        // Wings (with depth perspective)
        g2d.setColor(new Color(220, 220, 220));
        g2d.fill(new Ellipse2D.Double(30, 10, 60, 15)); // Bottom wing
        g2d.fill(new Ellipse2D.Double(30, -5, 60, 15));  // Top wing

        // Propeller (High speed rotation)
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(2));
        AffineTransform propTrans = g2d.getTransform();
        g2d.translate(0, 15);
        g2d.rotate(propAngle);
        g2d.drawLine(0, -20, 0, 20);
        g2d.setTransform(propTrans);
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String info = String.format("STATE: %s | SPD: %.1f kn | ALT: %.0f ft | GEAR: %s",
                state, velocity * 25, altitude, (gearLevel > 0.1 ? "DOWN" : "UP"));
        g2d.drawString(info, 20, 30);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        propAngle += velocity * 0.5; // Propeller spins faster with speed

        switch (state) {
            case TAXI:
                x += velocity;
                if (x > 100) state = FlightState.TAKEOFF_ROLL;
                break;

            case TAKEOFF_ROLL:
                velocity += 0.04;
                x += velocity;
                if (velocity > 6.0) state = FlightState.ROTATION;
                break;

            case ROTATION:
                x += velocity;
                pitch = Math.max(-15, pitch - 0.5); // Tilt nose up
                y -= 1.0;
                altitude = GROUND_Y - y;
                if (altitude > 50) state = FlightState.CLIMB;
                break;

            case CLIMB:
                x += velocity;
                y -= 2.0;
                altitude = GROUND_Y - y;
                if (gearLevel > 0) gearLevel -= 0.02; // Retract gear
                if (x > getWidth() + 200) {
                    prepareForLanding();
                }
                break;

            case LANDING:
                x += velocity;
                y += 1.5;
                altitude = GROUND_Y - y;
                pitch = 5; // Nose slightly down
                if (gearLevel < 1.0) gearLevel += 0.02; // Extend gear
                if (y >= GROUND_Y) {
                    y = GROUND_Y;
                    state = FlightState.BRAKING;
                }
                break;

            case BRAKING:
                x += velocity;
                velocity -= 0.05;
                pitch = 0;
                if (velocity <= 1.5) {
                    velocity = 1.5;
                    if (x > getWidth()) x = -150; // Loop simulation
                    state = FlightState.TAXI;
                }
                break;
        }
        repaint();
    }

    private void prepareForLanding() {
        state = FlightState.LANDING;
        x = -200;
        y = 200;
        velocity = 5.0;
    }
}