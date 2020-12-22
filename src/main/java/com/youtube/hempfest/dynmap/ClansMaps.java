package com.youtube.hempfest.dynmap;

import com.youtube.hempfest.clans.HempfestClans;
import com.youtube.hempfest.clans.util.StringLibrary;
import com.youtube.hempfest.clans.util.construct.Claim;
import com.youtube.hempfest.clans.util.construct.Clan;
import com.youtube.hempfest.clans.util.construct.ClanUtil;
import com.youtube.hempfest.clans.util.data.Config;
import com.youtube.hempfest.clans.util.data.ConfigType;
import com.youtube.hempfest.clans.util.data.DataManager;
import com.youtube.hempfest.clans.util.events.SubCommandEvent;
import com.youtube.hempfest.clans.util.events.TabInsertEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.markers.AreaMarker;

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

	@EventHandler
	public void onTab(TabInsertEvent e) {
		List<String> add = new ArrayList<>(Arrays.asList("map", "unmap"));
		for (String a : add) {
			if (!e.getArgs(1).contains(a)) {
				e.add(1, a);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onClanCommand(SubCommandEvent e) {
		Player p = e.getSender();
		int length = e.getArgs().length;
		String[] args = e.getArgs();
		StringLibrary lib = new StringLibrary();
		if (length == 1) {
			if (args[0].equalsIgnoreCase("map")) {
				if (Clan.clanUtil.getClan(p) != null) {
					Clan clan = HempfestClans.clanManager(p);
					lib.sendMessage(p, "&e&oUpdating dynmap with claim information..");
					if (Arrays.asList(clan.getOwnedClaims()).size() == 0) {
						lib.sendMessage(p, "&c&oClaim mapping task failed. No claims to map.");
						e.setReturn(true);
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
						e.setReturn(true);
					}
				}
				e.setReturn(true);
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
										e.setReturn(true);
									}
								}
							} else {
								lib.sendMessage(p, "&c&oYou do not have clan clearance.");
								e.setReturn(true);
							}
							e.setReturn(true);
						}
						lib.sendMessage(p, lib.notClaimOwner(claim.getOwner()));
					} else {
						lib.sendMessage(p, "This land belongs to: &4&nWilderness&r, and is free to claim.");
						e.setReturn(true);
					}
				} else {
					lib.sendMessage(p, lib.notInClan());
					e.setReturn(true);
				}
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
											e.setReturn(true);
										}
									}
								} else {
									Claim claim = new Claim(Claim.claimUtil.getClaimID(p.getLocation()));
									Clan clan2 = new Clan(claim.getOwner());
									if (clan.getPower() > clan2.getPower()) {
										integration.removeMarker(claim.getClaimID());
										e.setReturn(true);
									}
								}
							}
						}
					}
				}

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
								e.setReturn(false);
							}
							if (!Objects.requireNonNull(d.getConfigurationSection(getUtil().getClan(p) + ".Claims")).getKeys(false).isEmpty()) {
								for (String claimID : d.getConfigurationSection(getUtil().getClan(p) + ".Claims").getKeys(false)) {
									integration.removeMarker(claimID);
								}
							}
						}
					}
				}
			}
		}
	}


}
