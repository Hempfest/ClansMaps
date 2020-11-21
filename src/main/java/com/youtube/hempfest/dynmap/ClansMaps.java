package com.youtube.hempfest.dynmap;

import com.youtube.hempfest.clans.HempfestClans;
import com.youtube.hempfest.clans.util.StringLibrary;
import com.youtube.hempfest.clans.util.construct.Claim;
import com.youtube.hempfest.clans.util.construct.Clan;
import com.youtube.hempfest.clans.util.construct.ClanUtil;
import com.youtube.hempfest.clans.util.data.Config;
import com.youtube.hempfest.clans.util.data.ConfigType;
import com.youtube.hempfest.clans.util.data.DataManager;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.markers.AreaMarker;
import org.spigotmc.hessentials.listener.events.hempfest.WarpGateEvent;

public final class ClansMaps extends JavaPlugin implements Listener {

	public DynmapIntegration integration = new DynmapIntegration();

	@Override
	public void onEnable() {
		// Plugin startup logic
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
			if (Bukkit.getPluginManager().isPluginEnabled("dynmap")) {
				getLogger().info("- Dynmap found initializing API...");
				integration.registerDynmap();
				getLogger().info("- API successfully initialized");
			}
		}, 2);
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}

	public ClanUtil getUtil() {
		return Clan.clanUtil;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onWarpGate(WarpGateEvent e) {
		if (Claim.claimUtil.isInClaim(e.getGateBlock().getLocation())) {
			e.setGateMessage("Youve been poofed mate!! POOFED!!");
		}

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		String[] strs = e.getMessage().split(" ");
		String label = strs[0].replace("/", "");
		String[] args = new String[strs.length - 1];
		System.arraycopy(strs, 1, args, 0, strs.length - 1);
		int length = args.length;
		StringLibrary lib = new StringLibrary();
		if (label.equalsIgnoreCase("clan") || label.equalsIgnoreCase("c")) {
		if (length == 1) {
				if (args[0].equalsIgnoreCase("map")) {
					if (Clan.clanUtil.getClan(p) != null) {
						Clan clan = HempfestClans.clanManager(p);
						lib.sendMessage(p, "&e&oUpdating dynmap with claim information..");
						if (Arrays.asList(clan.getOwnedClaims()).size() == 0) {
							lib.sendMessage(p, "&c&oClaim mapping task failed. No claims to map.");
							e.setCancelled(true);
							return;
						}
						if (integration.getFailedAttempt() != null) {
							lib.sendMessage(p, integration.getFailedAttempt());
						}
						if (getUtil().getRankPower(p) >= 2) {
							long time = System.currentTimeMillis();
							integration.fillMap(clan.getOwnedClaims());
							long complete = (System.currentTimeMillis() - time) / 1000;
							int second = Integer.parseInt(String.valueOf(complete));
							lib.sendMessage(p, "&a&oClaim mapping task completed in &f" + second + "&a&os");
							HempfestClans.getInstance().getLogger().info("- (" + clan.getClanTag() + ") Marker sets successfully updated in accordance to claims.");
						} else {
							lib.sendMessage(p, "&c&oYou do not have clan clearance.");
							e.setCancelled(true);
							return;
						}
					}
					e.setCancelled(true);
					return;
				}
				if (args[0].equalsIgnoreCase("unmap")) {
					if (Clan.clanUtil.getClan(p) != null) {
						Clan clan = HempfestClans.clanManager(p);
						if (Claim.claimUtil.isInClaim(p.getLocation())) {
							Claim claim = new Claim(Claim.claimUtil.getClaimID(p.getLocation()));
							if (Arrays.asList(clan.getOwnedClaims()).contains(claim.getClaimID())) {
								Set<AreaMarker> markers = integration.markerset.getAreaMarkers();
								if (getUtil().getRankPower(p) >= 2) {
									for (AreaMarker am : markers) {
										if (am.getMarkerID().equals(claim.getClaimID())) {
											am.deleteMarker();
											lib.sendMessage(p, "&b&oCurrent claim visibility has been removed from the map.");
											e.setCancelled(true);
											return;
										}
									}
								} else {
									lib.sendMessage(p, "&c&oYou do not have clan clearance.");
									e.setCancelled(true);
									return;
								}
								e.setCancelled(true);
								return;
							}
							lib.sendMessage(p, lib.notClaimOwner(claim.getOwner()));
						} else {
							lib.sendMessage(p, "This land belongs to: &4&nWilderness&r, and is free to claim.");
							e.setCancelled(true);
							return;
						}
					} else {
						lib.sendMessage(p, lib.notInClan());
						e.setCancelled(true);
						return;
					}
					e.setCancelled(true);
					return;
				}
				if (args[0].equalsIgnoreCase("unclaim")) {
					if (Clan.clanUtil.getClan(p) != null) {
						if (getUtil().getRankPower(p) >= getUtil().claimingClearance()) {
							Clan clan = HempfestClans.clanManager(p);
							if (Claim.claimUtil.isInClaim(p.getLocation())) {
								if (Arrays.asList(clan.getOwnedClaims()).contains(Claim.claimUtil.getClaimID(p.getLocation()))) {
									integration.removeMarker(Claim.claimUtil.getClaimID(p.getLocation()));
								} else {
									if (getUtil().shieldStatus()) {
										if (getUtil().overPowerBypass()) {
											Claim claim = new Claim(Claim.claimUtil.getClaimID(p.getLocation()));
											Clan clan2 = claim.getClan();
											if (clan.getPower() > clan2.getPower()) {
												integration.removeMarker(claim.getClaimID());
											}
										}
									} else {
										Claim claim = new Claim(Claim.claimUtil.getClaimID(p.getLocation()));
										Clan clan2 = new Clan(claim.getOwner());
										if (clan.getPower() > clan2.getPower()) {
											integration.removeMarker(claim.getClaimID());
										}
									}
								}
							}
						}
					}
					return;
				}
			}
		if (length == 2) {
				if (args[0].equalsIgnoreCase("unclaim")) {
					if (args[1].equalsIgnoreCase("all")) {
						DataManager dm = new DataManager("Regions", "Configuration");
						Config regions = dm.getFile(ConfigType.MISC_FILE);
						FileConfiguration d = regions.getConfig();
						if (Clan.clanUtil.getClan(p) != null) {
							if (getUtil().getRankPower(p) >= getUtil().unclaimAllClearance()) {
								if (!d.isConfigurationSection(getUtil().getClan(p) + ".Claims")) {
									return;
								}
								if (!Objects.requireNonNull(d.getConfigurationSection(getUtil().getClan(p) + ".Claims")).getKeys(false).isEmpty()) {
									for (String claimID : d.getConfigurationSection(getUtil().getClan(p) + ".Claims").getKeys(false)) {
										integration.removeMarker(claimID);
										return;
									}
								}
							}
						}
					}
				}
		}
		}
	}


}
