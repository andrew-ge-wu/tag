package dsv.pis.gotag;

import java.util.UUID;

/**
 * @author andrew, Innometrics
 */
public interface IdentifiedAgent {
    enum NotificationType {
        LISTING, JOINING, LEAVING
    }

    void init();

    UUID getUUID();

    boolean tag();

    boolean passTag(IdentifiedAgent toTag);

    boolean isTagged();

    void notify(NotificationType type, IdentifiedAgent who);
}
