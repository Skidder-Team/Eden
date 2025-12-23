package rip.diamond.practice.util.tablist.util.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Collection;

//方案3：使用 ProtocolLib 的 EnumWrappers（如果可用）
public class WrapperPlayServerScoreboardTeam extends AbstractPacket {
    public static final PacketType TYPE = PacketType.Play.Server.SCOREBOARD_TEAM;

    // Team action constants - replacing deprecated IntEnum
    // 直接常量定义
    public static final int TEAM_CREATED = 0;
    public static final int TEAM_REMOVED = 1;
    public static final int TEAM_UPDATED = 2;
    public static final int PLAYERS_ADDED = 3;
    public static final int PLAYERS_REMOVED = 4;

    public WrapperPlayServerScoreboardTeam() {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerScoreboardTeam(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieve an unique name for the team. (Shared with scoreboard).
     * @return The current Team Name
     */
    public String getTeamName() {
        return handle.getStrings().read(0);
    }

    /**
     * Set an unique name for the team. (Shared with scoreboard).
     * @param value - new value.
     */
    public void setTeamName(String value) {
        handle.getStrings().write(0, value);
    }

    /**
     * Retrieve the current packet mode.
     * <p>
     * This determines whether or not team information is added or removed.
     * Use TEAM_CREATED, TEAM_REMOVED, TEAM_UPDATED, PLAYERS_ADDED, PLAYERS_REMOVED constants.
     * @return The current packet mode.
     */
    public int getPacketMode() {
        return handle.getIntegers().read(0);
    }

    /**
     * Set the current packet mode.
     * <p>
     * This determines whether or not team information is added or removed.
     * Use TEAM_CREATED, TEAM_REMOVED, TEAM_UPDATED, PLAYERS_ADDED, PLAYERS_REMOVED constants.
     * @param value - new value.
     */
    public void setPacketMode(int value) {
        handle.getIntegers().write(0, value);
    }

    /**
     * Retrieve the team display name.
     * <p>
     * A team must be created or updated.
     * @return The current display name.
     */
    public String getTeamDisplayName() {
        return handle.getStrings().read(1);
    }

    /**
     * Set the team display name.
     * <p>
     * A team must be created or updated.
     * @param value - new value.
     */
    public void setTeamDisplayName(String value) {
        handle.getStrings().write(1, value);
    }

    /**
     * Retrieve the team prefix. This will be inserted before the name of each team member.
     * <p>
     * A team must be created or updated.
     * @return The current Team Prefix
     */
    public String getTeamPrefix() {
        return handle.getStrings().read(2);
    }

    /**
     * Set the team prefix. This will be inserted before the name of each team member.
     * <p>
     * A team must be created or updated.
     * @param value - new value.
     */
    public void setTeamPrefix(String value) {
        handle.getStrings().write(2, value);
    }

    /**
     * Retrieve the team suffix. This will be inserted after the name of each team member.
     * <p>
     * A team must be created or updated.
     * @return The current Team Suffix
     */
    public String getTeamSuffix() {
        return handle.getStrings().read(3);
    }

    /**
     * Set the team suffix. This will be inserted after the name of each team member.
     * <p>
     * A team must be created or updated.
     * @param value - new value.
     */
    public void setTeamSuffix(String value) {
        handle.getStrings().write(3, value);
    }

    /**
     * Retrieve whether or not friendly fire is enabled.
     * <p>
     * A team must be created or updated.
     * @return The current Friendly fire setting
     */
    public byte getFriendlyFire() {
        return handle.getIntegers().read(1).byteValue();
    }

    /**
     * Set whether or not friendly fire is enabled.
     * <p>
     * A team must be created or updated.
     * @param value - new value.
     */
    public void setFriendlyFire(byte value) {
        handle.getIntegers().write(1, (int) value);
    }

    /**
     * Retrieve the list of player names.
     * <p>
     * Packet mode must be one of the following for this to be valid:
     * <ul>
     *  <li>TEAM_CREATED</li>
     *  <li>PLAYERS_ADDED</li>
     *  <li>PLAYERS_REMOVED</li>
     * </ul>
     * @return A list of player names.
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getPlayers() {
        return handle.getSpecificModifier(Collection.class).read(0);
    }

    /**
     * Set the list of player names.
     * <p>
     * Packet mode must be one of the following for this to be valid:
     * <ul>
     *  <li>TEAM_CREATED</li>
     *  <li>PLAYERS_ADDED</li>
     *  <li>PLAYERS_REMOVED</li>
     * </ul>
     * @param players - new players.
     */
    public void setPlayers(Collection<String> players) {
        handle.getSpecificModifier(Collection.class).write(0, players);
    }
}