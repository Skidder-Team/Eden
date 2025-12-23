package rip.diamond.practice.match;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.ArenaDetail;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitGameRules;
import rip.diamond.practice.match.team.Team;
import rip.diamond.practice.match.team.TeamPlayer;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.profile.cooldown.CooldownType;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Util;
import rip.diamond.practice.util.cuboid.CuboidDirection;

import java.util.Comparator;

public class MatchMovementHandler implements Listener {

    public MatchMovementHandler() {
        // Register this class as an event listener with high priority to handle before other plugins
        Bukkit.getPluginManager().registerEvents(this, Eden.INSTANCE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if player actually moved position (not just looking around)
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        PlayerProfile profile = PlayerProfile.get(player);
        if (profile == null) {
            return;
        }

        // Only process for players in match or spectating states
        PlayerState state = profile.getPlayerState();
        if (state != PlayerState.IN_MATCH && state != PlayerState.IN_SPECTATING) {
            return;
        }

        // Handle based on player state
        if (state == PlayerState.IN_MATCH) {
            handleMatchPlayerMove(event, player, profile, from, to);
        } else if (state == PlayerState.IN_SPECTATING) {
            handleSpectatorMove(event, player, profile, to);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        PlayerProfile profile = PlayerProfile.get(player);
        if (profile == null) {
            return;
        }

        // Only handle spectators teleporting
        if (profile.getPlayerState() == PlayerState.IN_SPECTATING && profile.getMatch() != null) {
            Match match = profile.getMatch();
            ArenaDetail arenaDetail = match.getArenaDetail();
            Arena arena = arenaDetail.getArena();

            // Check if spectator is teleporting outside allowed area
            if (!arenaDetail.getCuboid().clone().outset(CuboidDirection.HORIZONTAL,
                    Config.MATCH_SPECTATE_EXPEND_CUBOID.toInteger()).contains(to) ||
                    arena.getYLimit() > to.getY()) {

                // Teleport them back to spectator location
                event.setTo(arenaDetail.getSpectator());
            }
        }
    }

    /**
     * Handle movement for players in a match
     */
    private void handleMatchPlayerMove(PlayerMoveEvent event, Player player, PlayerProfile profile, Location from, Location to) {
        Match match = profile.getMatch();
        if (match == null) {
            return;
        }

        ArenaDetail arenaDetail = match.getArenaDetail();
        Arena arena = arenaDetail.getArena();
        Kit kit = match.getKit();
        KitGameRules gameRules = kit.getGameRules();
        Block block = to.getBlock();
        Block underBlock = to.clone().add(0, -1, 0).getBlock();

        // Handle starting freeze
        if (gameRules.isStartFreeze() && match.getState() == MatchState.STARTING &&
                (from.getX() != to.getX() || from.getZ() != to.getZ())) {

            Team playerTeam = match.getTeam(player);
            if (playerTeam != null) {
                Location location = playerTeam.getSpawnLocation();
                // Smoother looking by only changing the player's x and z location
                location.setY(from.getY());
                location.setPitch(from.getPitch());
                location.setYaw(from.getYaw());
                Util.teleport(player, location);
            }
            return;
        }

        // Check if player is outside the arena (instant death)
        if ((!arenaDetail.getCuboid().clone().outset(CuboidDirection.HORIZONTAL, 10).contains(player) &&
                Config.MATCH_OUTSIDE_CUBOID_INSTANT_DEATH.toBoolean()) ||
                arena.getYLimit() > to.getY()) {

            Util.damage(player, 99999);
            return;
        }

        // Prevent any duplicate scoring
        // Check if all players' score cooldowns are expired
        boolean allCooldownsExpired = match.getMatchPlayers().stream().allMatch(p -> {
            PlayerProfile pProfile = PlayerProfile.get(p);
            return pProfile != null && pProfile.getCooldowns().get(CooldownType.SCORE).isExpired();
        });

        if (!allCooldownsExpired) {
            return;
        }

        TeamPlayer teamPlayer = match.getTeamPlayer(player);
        if (teamPlayer == null || match.getState() != MatchState.FIGHTING || teamPlayer.isRespawning()) {
            return;
        }

        // Handle water death for specific game modes
        if (handleWaterDeath(player, match, profile, teamPlayer, gameRules, block)) {
            return;
        }

        // Handle portal goal for bridge-like game modes
        handlePortalGoal(event, player, match, profile, teamPlayer, gameRules, block, to);
    }

    /**
     * Handle water death mechanics
     */
    private boolean handleWaterDeath(Player player, Match match, PlayerProfile profile,
                                     TeamPlayer teamPlayer, KitGameRules gameRules, Block block) {
        if (gameRules.isDeathOnWater() &&
                (block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER)) {

            if (gameRules.isPoint(match)) {
                TeamPlayer lastHitDamager = teamPlayer.getLastHitDamager();
                // Player might die without being hit by enemy, e.g., by lava
                // If so, pick a random player from the opponent team
                if (lastHitDamager == null) {
                    Team opponentTeam = match.getOpponentTeam(match.getTeam(player));
                    if (opponentTeam != null && !opponentTeam.getAliveTeamPlayers().isEmpty()) {
                        lastHitDamager = opponentTeam.getAliveTeamPlayers().get(0);
                    }
                }
                if (lastHitDamager != null) {
                    match.score(profile, teamPlayer, lastHitDamager);
                }
            } else {
                Util.damage(player, 99999);
            }
            return true;
        }
        return false;
    }

    /**
     * Handle portal goal mechanics
     */
    private void handlePortalGoal(PlayerMoveEvent event, Player player, Match match, PlayerProfile profile,
                                  TeamPlayer teamPlayer, KitGameRules gameRules, Block block, Location to) {
        if (gameRules.isPortalGoal() && block.getType() == Material.ENDER_PORTAL) {
            Team playerTeam = match.getTeam(player);
            if (playerTeam == null) {
                return;
            }

            // Find which team's portal is closest
            Team portalBelongsTo = match.getTeams().stream()
                    .min(Comparator.comparing(team -> team.getSpawnLocation().distance(to)))
                    .orElse(null);

            if (portalBelongsTo == null) {
                Common.log("An error occurred while finding portalBelongsTo, please contact GoodestEnglish to fix");
                return;
            }

            if (portalBelongsTo != playerTeam) {
                // Player scored in opponent's portal
                match.score(profile, null, teamPlayer);
            } else {
                // Player tried to score in their own portal - penalize
                Util.damage(player, 99999);
            }
        }
    }

    /**
     * Handle movement for spectators
     */
    private void handleSpectatorMove(PlayerMoveEvent event, Player player, PlayerProfile profile, Location to) {
        Match match = profile.getMatch();
        if (match == null) {
            return;
        }

        ArenaDetail arenaDetail = match.getArenaDetail();
        Arena arena = arenaDetail.getArena();

        // Check if spectator is moving outside allowed area
        if (!arenaDetail.getCuboid().clone().outset(CuboidDirection.HORIZONTAL,
                Config.MATCH_SPECTATE_EXPEND_CUBOID.toInteger()).contains(to) ||
                arena.getYLimit() > to.getY()) {

            // Teleport them back to spectator location
            player.teleport(arenaDetail.getSpectator());
        }
    }
}