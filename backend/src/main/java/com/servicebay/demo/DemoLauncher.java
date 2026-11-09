package com.servicebay.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.nio.file.Path;

/**
 * Makes the demo jar work when simply double-clicked on Windows/macOS:
 * turns on the "demo" profile, writes servicebay.log next to the jar and shows a small status window.
 */
public final class DemoLauncher {
    private DemoLauncher() {}

    static final String EMBEDDED_PG = "io.zonky.test.db.postgres.embedded.EmbeddedPostgres";
    private static JLabel status;

    public static boolean isDemoBuild() {
        try { Class.forName(EMBEDDED_PG); return true; } catch (ClassNotFoundException e) { return false; }
    }

    public static void configure(SpringApplication app) {
        boolean profileGiven = System.getProperty("spring.profiles.active") != null || System.getenv("SPRING_PROFILES_ACTIVE") != null;
        if (!profileGiven) app.setAdditionalProfiles("demo");
        if (System.getProperty("logging.file.name") == null) {
            System.setProperty("logging.file.name", jarDir().resolve("servicebay.log").toString());
        }
        boolean gui = !GraphicsEnvironment.isHeadless() && System.getProperty("servicebay.nogui") == null
                && !"true".equalsIgnoreCase(System.getenv("CI"));
        app.setHeadless(!gui);
        if (gui) {
            showWindow();
            app.addListeners((ApplicationListener<ApplicationReadyEvent>) e -> ready(e.getApplicationContext()));
        }
    }

    public static void failed(Throwable t) {
        if (status != null) SwingUtilities.invokeLater(() -> status.setText(
                "<html><b style='color:#FF8A8A'>Could not start.</b><br>See servicebay.log next to the app file.<br>" + escape(String.valueOf(t.getMessage())) + "</html>"));
    }

    static Path jarDir() {
        try {
            String cp = System.getProperty("java.class.path", ".").split(java.io.File.pathSeparator)[0];
            Path p = Path.of(cp).toAbsolutePath();
            return p.getFileName().toString().endsWith(".jar") ? p.getParent() : Path.of(".").toAbsolutePath();
        } catch (Exception e) {
            return Path.of(".").toAbsolutePath();
        }
    }

    private static void showWindow() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame f = new JFrame("ServiceBay demo");
                status = new JLabel("<html><b>Starting ServiceBay…</b><br>This takes about 30 seconds.</html>");
                status.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
                JButton open = new JButton("Open app");
                open.addActionListener(a -> browse());
                JButton stop = new JButton("Stop");
                stop.addActionListener(a -> System.exit(0));
                JPanel buttons = new JPanel();
                buttons.add(open);
                buttons.add(stop);
                f.getContentPane().add(status, BorderLayout.CENTER);
                f.getContentPane().add(buttons, BorderLayout.SOUTH);
                f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                f.setSize(420, 190);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            });
        } catch (Exception ignored) { }
    }

    private static void ready(ConfigurableApplicationContext ctx) {
        String port = ctx.getEnvironment().getProperty("local.server.port", "8080");
        SwingUtilities.invokeLater(() -> status.setText("<html><b style='color:#1a7f4b'>ServiceBay is running.</b><br>"
                + "Open <b>http://localhost:" + port + "</b> in your browser.<br>Keep this window open; close it to stop.</html>"));
        browse();
    }

    private static void browse() {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
        } catch (Exception ignored) { }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
