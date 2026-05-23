import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import javax.swing.*;

public class RealisticFlightSim2 extends JFrame {

    public RealisticFlightSim2() {
        setTitle("Multiple Aircraft Takeoff Simulation");
        setSize(1200, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new FlightEngine());
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RealisticFlightSim2().setVisible(true);
        });
    }
}

class Aircraft {

    enum Phase {
        TAXI,
        TAKEOFF,
        CLIMB
    }

    double x;
    double y;
    double velocity;
    double pitch;
    double gearPos;
    double propRotation;

    String flightName;
    Color bodyColor;

    Phase currentPhase = Phase.TAXI;

    public Aircraft(String flightName, double startX, Color bodyColor) {
        this.flightName = flightName;
        this.x = startX;
        this.y = 480;
        this.velocity = 1.8;
        this.pitch = 0;
        this.gearPos = 1.0;
        this.propRotation = 0;
        this.bodyColor = bodyColor;
    }
}

class FlightEngine extends JPanel implements ActionListener {

    private final int RUNWAY_Y = 480;

    private ArrayList<Aircraft> flights = new ArrayList<>();

    public FlightEngine() {

        setBackground(new Color(173, 216, 230));

        // Multiple Flights
        flights.add(new Aircraft("Biman BG-101", -100, Color.WHITE));
        flights.add(new Aircraft("Emirates EK-202", -350, Color.LIGHT_GRAY));
        flights.add(new Aircraft("Qatar QR-303", -600, new Color(240, 240, 240)));
        flights.add(new Aircraft("US Bangla BS-404", -850, new Color(220, 220, 255)));
        flights.add(new Aircraft("Singapore SQ-505", -1100, new Color(255, 240, 220)));

        new Timer(16, this).start();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        drawScene(g2d);

        for (Aircraft aircraft : flights) {
            drawAircraft(g2d, aircraft);
        }

        drawTelemetry(g2d);
    }

    private void drawScene(Graphics2D g2d) {

        // Grass
        g2d.setColor(new Color(40, 150, 40));
        g2d.fillRect(0, RUNWAY_Y + 20, getWidth(), getHeight());

        // Runway
        g2d.setColor(new Color(45, 45, 45));
        g2d.fillRect(0, RUNWAY_Y + 10, getWidth(), 60);

        // Runway Center Line
        g2d.setColor(Color.YELLOW);

        for (int i = 0; i < getWidth(); i += 80) {
            g2d.fillRect(i, RUNWAY_Y + 38, 30, 4);
        }

        // Airport Text
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);
        g2d.drawString("INTERNATIONAL AIRPORT", 420, 530);
    }

    private void drawAircraft(Graphics2D g2d, Aircraft plane) {

        AffineTransform old = g2d.getTransform();

        // Move aircraft
        g2d.translate(plane.x, plane.y);

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 50));

        double altitudeEffect =
                Math.max(0.4, 1.0 - (RUNWAY_Y - plane.y) / 300.0);

        g2d.fillOval(
                -40,
                (int) (RUNWAY_Y - plane.y + 45),
                (int) (100 * altitudeEffect),
                10
        );

        // Rotate aircraft
        g2d.rotate(Math.toRadians(plane.pitch));

        // Fuselage
        g2d.setColor(plane.bodyColor);

        g2d.fill(
                new RoundRectangle2D.Double(
                        -50,
                        -10,
                        100,
                        20,
                        15,
                        15
                )
        );

        // Cockpit Window
        g2d.setColor(new Color(50, 50, 150));

        g2d.fill(
                new Arc2D.Double(
                        25,
                        -10,
                        20,
                        15,
                        0,
                        90,
                        Arc2D.PIE
                )
        );

        // Wings
        g2d.setColor(Color.GRAY);

        Polygon wing = new Polygon();

        wing.addPoint(-10, 0);
        wing.addPoint(10, 0);
        wing.addPoint(35, 25);
        wing.addPoint(-35, 25);

        g2d.fillPolygon(wing);

        // Tail
        g2d.setColor(Color.RED);

        Path2D tail = new Path2D.Double();

        tail.moveTo(-50, -10);
        tail.lineTo(-65, -35);
        tail.lineTo(-40, -10);
        tail.closePath();

        g2d.fill(tail);

        // Landing Gear
        g2d.setColor(Color.BLACK);

        if (plane.gearPos > 0) {

            int gY = (int) (10 * plane.gearPos);

            g2d.drawLine(-25, 10, -25, 10 + gY);
            g2d.drawLine(25, 10, 25, 10 + gY);

            g2d.fillOval(-28, 10 + gY, 6, 6);
            g2d.fillOval(22, 10 + gY, 6, 6);
        }

        // Propeller
        g2d.setColor(Color.DARK_GRAY);

        g2d.fillOval(48, -5, 6, 10);

        g2d.setStroke(new BasicStroke(2));

        g2d.rotate(plane.propRotation, 51, 0);

        g2d.drawLine(51, -20, 51, 20);

        // Flight Name
        g2d.setColor(Color.BLACK);

        g2d.setFont(new Font("Arial", Font.BOLD, 10));

        g2d.drawString(plane.flightName, -25, -18);

        g2d.setTransform(old);
    }

    private void drawTelemetry(Graphics2D g2d) {

        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));

        g2d.setColor(Color.BLACK);

        g2d.drawString("UPCOMING FLIGHTS", 20, 40);

        int y = 70;

        for (Aircraft aircraft : flights) {

            if (aircraft.currentPhase == Aircraft.Phase.TAKEOFF) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }

            g2d.drawString(
                    aircraft.flightName
                            + "  [" + aircraft.currentPhase + "]",
                    20,
                    y
            );

            y += 25;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        for (Aircraft plane : flights) {

            plane.propRotation += plane.velocity * 0.4;

            switch (plane.currentPhase) {

                case TAXI:

                    plane.x += plane.velocity;

                    if (plane.x > 150) {
                        plane.currentPhase = Aircraft.Phase.TAKEOFF;
                    }

                    break;

                case TAKEOFF:

                    plane.velocity += 0.05;

                    plane.x += plane.velocity;

                    if (plane.velocity > 6) {

                        plane.pitch = -10;

                        plane.currentPhase = Aircraft.Phase.CLIMB;
                    }

                    break;

                case CLIMB:

                    plane.x += plane.velocity;

                    plane.y -= 2.0;

                    if (plane.gearPos > 0) {
                        plane.gearPos -= 0.02;
                    }

                    // Reset aircraft after leaving screen
                    if (plane.x > getWidth() + 200) {

                        plane.x = -200 - (Math.random() * 800);

                        plane.y = RUNWAY_Y;

                        plane.velocity = 1.8;

                        plane.pitch = 0;

                        plane.gearPos = 1.0;

                        plane.currentPhase = Aircraft.Phase.TAXI;
                    }

                    break;
            }
        }

        repaint();
    }
}