package dsv.pis.gotag;

import dsv.pis.gotag.bailiff.BailiffInterface;

import java.util.UUID;

/**
 * @author andrew, Innometrics
 */
public interface IdentifiedAgent {
    enum NotificationType {
        LISTING, JOINING, LEAVING
    }

    UUID getUUID();

    boolean tag();

    boolean passTag(BailiffInterface container,IdentifiedAgent toTag);

    boolean isTagged();

    void notify(NotificationType type, IdentifiedAgent who);
}
