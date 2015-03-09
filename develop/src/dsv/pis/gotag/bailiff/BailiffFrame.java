// BailiffFrame.java
// Fredrik Kilander, DSV
// 18-nov-2004/FK Adapted for PIS course.
// 2001-03-28/FK First version

package dsv.pis.gotag.bailiff;

import dsv.pis.gotag.IdentifiedAgent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.UUID;

/**
 * This class creates a rudimentary GUI for a Bailiff instance by wrapping
 * a JFrame around it and presenting a simple menu structure. The purpose
 * is to make the Bailiff visible and to provide an easy way to shut it down.
 */
public class BailiffFrame extends JFrame {

    /**
     * The Bailiff service instance we front a GUI for.
     */
    private final Bailiff bf;

    /**
     * Creates a new Bailiff service GUI.
     *
     * @param managedBf The Bailiff service instance we manage a GUI for.
     */
    public BailiffFrame(Bailiff managedBf) {

        // Set the title of the JFrame.
        super(managedBf.getRoom() + " : Bailiff");

        // Copy from method argument to instance field.
        bf = managedBf;

        // Create a menu bar to hold our menus.
        JMenuBar menuBar = new JMenuBar();

        // Create a menu labelled 'File'.
        JMenu fileMenu = (JMenu) menuBar.add(new JMenu("File"));
        // Bind ALT+F to the File menu.
        fileMenu.setMnemonic(KeyEvent.VK_F);

        // Create and add a menu item labelled 'Exit' to the File menu.
        JMenuItem item = (JMenuItem) fileMenu.add(new JMenuItem("Exit"));
        // Bind ALT+X to the Exit item.
        item.setMnemonic(KeyEvent.VK_X);
        // Install the code to execute when the Exit item is selected.
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bf.shutdown();    // Shut down the Bailiff
                System.exit(0);    // Exit the JVM
            }
        });

        // Create a menu labelled 'Info'.
        JMenu options = (JMenu) menuBar.add(new JMenu("Info"));
        // Bind ALT+I to the Info menu.
        options.setMnemonic(KeyEvent.VK_I);

        // Create and add a menu item labelled 'About...' to the Info menu.
        item = (JMenuItem) options.add(new JMenuItem("About..."));
        // Bind ALT+A to the About item.
        item.setMnemonic(KeyEvent.VK_A);
        // Install the code to execute when the About item is selected.
        // Notice the use of the adapter class ActionListener which is
        // anonymously subclassed as the argument to addActionListener().
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });

        // Install the menubar.
        setJMenuBar(menuBar);


        JList<String> runningChildren = new JList<>();
        setLayout(new FlowLayout());

        getContentPane().add(runningChildren, BorderLayout.CENTER);

        new Thread(new ListUpdater(runningChildren, bf)).start();

        // Install code to execute for certain window events.
        // Adapter class WindowAdapter...
        addWindowListener(new WindowAdapter() {
            // If the windows is closed, shut down the Bailiff.
            public void windowClosing(WindowEvent e) {
                bf.shutdown();
                System.exit(0);
            }

            // If we are minimized or maximized, keep working.
            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }
        });

        // Do qualitative layout
        pack();

        // Determine actual sizes.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        // The window is located 1/8th of the screen size from upper left corner.
        setLocation(d.width / 8, d.height / 8);
        // The window is 1/12th wide, 1/10th high, or screen size.
        setSize(new Dimension((d.width / 4), (d.height / 4)));
        // Show it.
        setVisible(true);
    }

    private class ListUpdater implements Runnable {
        private final Bailiff bailiff;
        private final JList<String> toUpdate;
        private final DefaultListModel<String> model;

        public ListUpdater(JList<String> toUpdate, Bailiff bailiff) {
            this.toUpdate = toUpdate;
            this.bailiff = bailiff;
            this.model = new DefaultListModel<>();
            toUpdate.setModel(this.model);
        }

        @Override
        public void run() {
            System.out.println("Start GUI updater");
            while (true) {
                model.clear();
                try {
                    if (bailiff.children.size() > 0) {
                        for (Map.Entry<UUID, IdentifiedAgent> entry : bailiff.children.entrySet()) {
                            model.addElement(entry.getKey().toString() + ": is tagged[" + entry.getValue().isTagged() + "]");
                        }
                    } else {
                        model.addElement("No one is running on this bailiff");
                    }
                } finally {
                    toUpdate.repaint();
                    try {
                        Thread.sleep(Bailiff.OPT_DELAY/10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * The 'about' dialog.
     */
    public void showAboutDialog() {
        // Note that a new thread is created here to run the dialogue.
        // That way control returns at once to the caller, while the user
        // interacts with the dialogue. This ok since its just a read-only
        // information box.
        new Thread(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null,
                        bf.toString(),
                        "Bailiff information",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }).start();
    }

}
