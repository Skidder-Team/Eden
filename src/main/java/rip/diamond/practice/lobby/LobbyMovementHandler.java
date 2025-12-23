package rip.diamond.practice.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import rip.diamond.practice.Eden;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;

public class LobbyMovementHandler implements Listener {

    public LobbyMovementHandler(Eden plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile == null) {
            return;
        }

        // Check if player is in lobby or queue
        if (profile.getPlayerState() != PlayerState.IN_LOBBY &&
                profile.getPlayerState() != PlayerState.IN_QUEUE) {
            return;
        }

        Location to = event.getTo();

        // Check if player fell below Y=0
        if (to.getY() < 0) {
            // Teleport player back to spawn
            Eden.INSTANCE.getLobbyManager().teleport(player);
        }
    }
}