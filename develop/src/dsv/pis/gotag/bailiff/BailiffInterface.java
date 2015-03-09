// BailiffInterface.java
// Fredrik Kilander, DSV
// 18-nov-2004/FK Adapted for PIS course.
// 2000-12-13/FK Adapted from earlier version.

package dsv.pis.gotag.bailiff;

import dsv.pis.gotag.IdentifiedAgent;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;

/**
 * This interface is for the Bailiff's clients. This is mobile code which
 * move into the Bailiff's JVM for execution.
 */
public interface BailiffInterface
        extends
        java.rmi.Remote {


    public UUID getUUID()
            throws
            RemoteException;

    /**
     * Returns a string which confirms communication with the Bailiff
     * service instance.
     */
    public String ping()
            throws
            RemoteException;

    /**
     * Returns a property of the Bailiff.
     *
     * @param key The case-insensitive property key to retrieve.
     * @return The property string or null.
     */
    public String getProperty(String key)
            throws
            RemoteException;

    /**
     * The entry point for mobile code.
     * The client sends and object (itself perhaps), a string
     * naming the callback method and an array of arguments which must
     * map against the parameters of the callback method.
     *
     * @param obj  The object (to execute).
     * @param cb   The name of the method to call as the program of obj.
     * @param args The parameters for the callback method. Note that if
     *             the method has a signature without arguments the value of args
     *             should be an empty array. Setting args to null will not work.
     * @throws RemoteException                 Thrown if there is an RMI problem.
     * @throws java.lang.NoSuchMethodException Thrown if the proposed
     *                                         callback is not found (which happen if the name is spelled wrong,
     *                                         the number of arguments is wrong or are of the wrong type).
     */
    public void migrate(IdentifiedAgent obj, String cb, Object[] args)
            throws
            RemoteException,
            java.lang.NoSuchMethodException;

    public Map<UUID, IdentifiedAgent> getRunningChildren(IdentifiedAgent who)
            throws
            RemoteException,
            java.lang.NoSuchMethodException;

    public void leave(IdentifiedAgent who)
            throws
            RemoteException,
            java.lang.NoSuchMethodException;


}
