// Dexter.java
// Bailiff excerciser and demo.
// Fredrik Kilander, DSV
// 30-jan-2009/FK Replaced f.show() (deprecated) with f.setVisible();
// 07-feb-2008/FK Code smarted up a bit.
// 18-nov-2004/FK Adapted for PRIS course.
// 2000-12-18/FK Runs for the first time.
// 2000-12-13/FK

package dsv.pis.gotag.dexter;

import dsv.pis.gotag.IdentifiedAgent;
import dsv.pis.gotag.bailiff.BailiffInterface;
import dsv.pis.gotag.util.CmdlnOption;
import dsv.pis.gotag.util.Commandline;
import dsv.pis.gotag.util.Sleeper;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceDiscoveryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

/**
 * Dexter jumps around randomly among the Bailiffs. He is can be used
 * test that the system is operating, or as a template for more
 * evolved agents.
 */
public class Dexter implements IdentifiedAgent, Serializable {
    public static final int OPT_DELAY = 3000;

    private final UUID uuid = UUID.randomUUID();

    private volatile boolean tagged = false;

    /**
     * The string name of the Bailiff service interface, used when
     * querying the Jini lookup server.
     */
    protected static final String bfi =
            "dsv.pis.gotag.bailiff.BailiffInterface";

    /**
     * The debug flag controls the amount of diagnostic info we put out.
     */
    protected boolean debug = false;

    /**
     * The noFace flag disables the graphical frame when true.
     */
    protected boolean noFace = false;

    /**
     * Dexter uses a ServiceDiscoveryManager to find Bailiffs.
     * The SDM is not serializable so it must recreated on each new Bailiff.
     * That is why it is marked as transient.
     */
    protected transient ServiceDiscoveryManager SDM;

    /**
     * This service template is created in Dexter's constructor and used
     * in the topLevel method to find Bailiffs. The service
     * template IS serializable so Dexter only needs to instantiate it once.
     */
    protected ServiceTemplate bailiffTemplate;
    private List<BailiffInterface> bailiffs;

    /**
     * Outputs a diagnostic message on standard output. This will be on
     * the host of the launching JVM before Dexter moves. Once he has migrated
     * to another Bailiff, the text will appear on the console of that Bailiff.
     *
     * @param msg The message to print.
     */
    protected void debugMsg(String msg) {
        if (debug) System.out.println(msg);
    }

    /**
     * This creates a new Dexter. All the constructor needs to do is to
     * instantiate the service template.
     *
     * @param debug True if this instance is being debugged.
     * @param tag
     * @throws ClassNotFoundException Thrown if the class for the Bailiff
     *                                service interface could not be found.
     */
    public Dexter(boolean debug, boolean noFace, boolean tag)
            throws
            java.lang.ClassNotFoundException {
        if (this.debug == false) this.debug = debug;

        this.noFace = noFace;
        this.tagged = tag;
        // This service template is used to query the Jini lookup server
        // for services which implement the BailiffInterface. The string
        // name of that interface is passed in the bfi argument. At this
        // point we only create and configure the service template, no
        // query has yet been issued.

        bailiffTemplate =
                new ServiceTemplate
                        (null,
                                new Class[]{java.lang.Class.forName(bfi)},
                                null);
    }

    /**
     * This is Dexter's main program once he is on his way. In short, he
     * gets himself a service discovery manager and asks it about Bailiffs.
     * If the list is long enough, he then selects one randomly and pings it.
     * If the ping returned without a remote exception, Dexter then tries
     * to migrate to that Bailiff. If the ping or the migrates fails, Dexter
     * gives up on that Bailiff and tries another.
     */
    public void topLevel()
            throws
            IOException, InterruptedException {
        // Create a Jini service discovery manager to help us interact with
        // the Jini lookup service.

        DexterFace dexFace = null;
        JFrame f = null;

        if (!noFace) {
            // Create a small GUI for this Dexter instance.
            f = new JFrame("Dexter");
            f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            dexFace = new DexterFace();
            f.getContentPane().add("Center", dexFace);
            dexFace.init();
            f.pack();
            f.setSize(new Dimension(256, 192));
            f.setVisible(true);
            dexFace.startAnimation();
        }
        try {
            BailiffInterface container = getContainer(bailiffs);
            if (container != null) {
                while (true) {
                    if (tagged && container.getRunningChildren(this).size() > 1) {
                        System.out.println("Started tagging!");
                        Map<UUID, IdentifiedAgent> children = container.getRunningChildren(this);
                        children.remove(getUUID());
                        if (children.size() > 0) {
                            ArrayList<IdentifiedAgent> toTagFrom = new ArrayList<>(children.values());
                            Collections.shuffle(toTagFrom);
                            if (container.tag(getUUID(), toTagFrom.get(0).getUUID())) {
                                System.out.println("Successfully tagged!");
                            } else {
                                System.out.println("Failed to tag!");
                            }
                        }
                    } else if (move(container, bailiffs)) {
                        return;
                    } else {
                        Sleeper.sleep(OPT_DELAY / 4, OPT_DELAY);
                    }
                } // for ever // go back up and try to find more Bailiffs
            } else {
                move(null, bailiffs);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } finally {
            if (!noFace && dexFace != null) {
                dexFace.stopAnimation();
                f.setVisible(false);
            }
        }
    }


    @Override
    public void init() {
        try {
            SDM = new ServiceDiscoveryManager(null, null);
            this.bailiffs = getBailiffs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public boolean tag() {
        if (!tagged) {
            tagged = true;
            System.out.println("I'm tagged:" + getUUID());
            return tagged;
        } else {
            return false;
        }
    }

    @Override
    public boolean passTag(IdentifiedAgent toTag) {
        if (toTag.tag()) {
            tagged = false;
        }
        return !tagged;
    }

    @Override
    public boolean isTagged() {
        return tagged;
    }

    @Override
    public void notify(NotificationType type, IdentifiedAgent who) {
    }


    private boolean move(BailiffInterface container, List<BailiffInterface> bailiffs) throws NoSuchMethodException, RemoteException {
        BailiffInterface target = getBailiffToMove(bailiffs);
        return target != null && migrateTo(container, target);
    }


    private List<BailiffInterface> getBailiffs() {
        List<BailiffInterface> toReturn;
        int retryInterval = 0;
        ServiceItem[] svcItems;
        do {

            if (0 < retryInterval) {
                debugMsg("No Bailiffs detected - sleeping.");
                Sleeper.sleep(retryInterval);
                debugMsg("Waking up.");
            }

            // Put our query, expressed as a service template, to the Jini
            // service discovery manager.

            svcItems = SDM.lookup(bailiffTemplate, 8, null);
            retryInterval = 20 * 1000;

            // If no lookup servers are found, go back up to the beginning
            // of the loop, sleep a bit and then try again.
        } while (svcItems.length == 0);
        toReturn = new ArrayList<>(svcItems.length);
        for (ServiceItem eachItem : svcItems) {
            Object obj = eachItem.service;
            if (obj instanceof BailiffInterface) {
                try {
                    BailiffInterface bailiff = BailiffInterface.class.cast(obj);
                    String result = bailiff.ping();
                    debugMsg(result);
                    toReturn.add(bailiff);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.shuffle(toReturn);
        return toReturn;
    }

    private BailiffInterface getContainer(Collection<BailiffInterface> options) throws NoSuchMethodException, RemoteException {
        for (BailiffInterface option : options) {
            if (option.getRunningChildren(this).containsKey(getUUID())) {
                return option;
            }
        }
        System.out.println("Non of the bailiffs contains me? ID:" + getUUID());
        return null;
    }


    private boolean migrateTo(BailiffInterface from, BailiffInterface migrateTo) {
        debugMsg("Trying to jump...");
        // This is the spot where Dexter tries to migrate.
        try {
            if (from != null) {
                from.leave(this);
            }
            migrateTo.migrate(this, "topLevel", new Object[]{});
            SDM.terminate();
            return true;
        } catch (java.rmi.RemoteException | NoSuchMethodException e) { // FAILURE
            if (debug) {
                e.printStackTrace();
            }
        }
        debugMsg("Didn't make the jump...");
        return false;
    }


    private BailiffInterface getBailiffToMove(Collection<BailiffInterface> options) {
        try {
            BailiffInterface toReturn = getContainer(options);
            StringBuilder stringBuilder = new StringBuilder("Agent:" + getUUID() + " is ");
            for (BailiffInterface option : options) {
                if (tagged) {
                    stringBuilder.append("\nlooking for agent to tag");
                    Map<UUID, IdentifiedAgent> optionChildren = option.getRunningChildren(this);
                    if (toReturn == null || toReturn.getRunningChildren(this).size() <= optionChildren.size()) {
                        toReturn = option;
                    }
                } else {
                    stringBuilder.append("\nrunning away");
                    boolean containsTagged = false;
                    Map<UUID, IdentifiedAgent> optionChildren = option.getRunningChildren(this);
                    for (IdentifiedAgent eachChild : optionChildren.values()) {
                        if (eachChild.isTagged()) {
                            containsTagged = true;
                            break;
                        }
                    }
                    if (!containsTagged && (toReturn == null || optionChildren.size() < toReturn.getRunningChildren(this).size())) {
                        toReturn = option;
                    }
                }
            }
            if (toReturn != null) {
                Map<UUID, IdentifiedAgent> targetChildren = toReturn.getRunningChildren(this);
                if (targetChildren.containsKey(getUUID())) {
                    stringBuilder = new StringBuilder("No changing bailiff");
                    System.out.println(stringBuilder.toString());
                    return null;
                } else {
                    stringBuilder.append(", final target:").append(toReturn.ping());
                    System.out.println(stringBuilder.toString());
                    return toReturn;
                }
            } else {
                return null;
            }
        } catch (RemoteException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The main program of Dexter. It is only used when a Dexter is launched.
     */
    public static void main(String[] argv)
            throws
            java.lang.ClassNotFoundException,
            IOException, InterruptedException {
        CmdlnOption helpOption = new CmdlnOption("-help");
        CmdlnOption debugOption = new CmdlnOption("-debug");
        CmdlnOption noFaceOption = new CmdlnOption("-noface");
        CmdlnOption tagOption = new CmdlnOption("-tag");


        CmdlnOption[] opts =
                new CmdlnOption[]{helpOption, debugOption, noFaceOption, tagOption};

        String[] restArgs = Commandline.parseArgs(System.out, argv, opts);

        if (restArgs == null) {
            System.exit(1);
        }

        if (helpOption.getIsSet()) {
            System.out.println("Usage: [-help]|[-debug][-noface]");
            System.out.println("where -help shows this message");
            System.out.println("      -debug turns on debugging.");
            System.out.println("      -noface disables the GUI.");
            System.exit(0);
        }

        boolean debug = debugOption.getIsSet();
        boolean noFace = noFaceOption.getIsSet();
        boolean tag = tagOption.getIsSet();

        // We will try without it first
        // System.setSecurityManager (new RMISecurityManager ());
        Dexter dx = new Dexter(debug, noFace, tag);
        dx.init();
        dx.topLevel();
        System.exit(0);
    }
}
