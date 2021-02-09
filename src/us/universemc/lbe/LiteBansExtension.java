package us.universemc.lbe;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import litebans.api.Events;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import us.universemc.lbe.command.CmdSiblings;
import us.universemc.lbe.utils.Messages;
import us.universemc.lbe.utils.Pair;

public class LiteBansExtension extends JavaPlugin {

	private static final Pattern SCANNING_PATTERN = Pattern.compile("(?<=Scanning ).*(?=\\.)");
	private File siblingsFile;
	private FileConfiguration siblingsConfig;
	
	private Events.Listener liteBansListener;
	
	@Getter
	private HashSet<HashSet<UUID>> siblings;
	
	@Override
	public void onEnable() {
		
		//LiteBans listener
		Events.get().register(liteBansListener = new Events.Listener() {
			@Override
			@SuppressWarnings("deprecation")
			public void broadcastSent(String message, String type) {
				if (!type.equals("dupeip_join")) return;
				Pair<String,Collection<String>> usernames = getBannedPlayerNamesFromMsg(message);
				OfflinePlayer joining = Bukkit.getOfflinePlayer(usernames.getLeft());
				
				for (String bannedNames : usernames.getRight()) {
					if (!areSiblings(joining.getUniqueId(), Bukkit.getOfflinePlayer(bannedNames).getUniqueId())) {
						return;
					}
				}
				HashMap<String,String> replMap = new HashMap<>();
				replMap.put("{USERNAME}", usernames.getLeft());
				replMap.put("{SIBLINGS}", usernames.getRight().stream().collect(Collectors.joining(", ")));
				Messages.EVENT_SIBLINGS_JOINED.broadcastPermission("litebans.notify.dupe_ip", replMap);
			}
		});
		
		//Commands
		getCommand("siblings").setExecutor(new CmdSiblings(this));
		
		//Configuration
		Messages.onEnable(this, "messages.yml");
		siblingsFile = new File(getDataFolder(), "siblings.yml");
		loadConfiguration();
	}
	
	@Override
	public void onDisable() {
		saveConfiguration();
		Events.get().unregister(liteBansListener);
	}
	
	public boolean areSiblings(UUID p1, UUID p2) {
		if (p1.equals(p2)) return true;
		for (HashSet<UUID> sibling : siblings) {
			if (sibling.contains(p1) && sibling.contains(p2)) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public void loadConfiguration() {
		Messages.reload();
		siblingsConfig = YamlConfiguration.loadConfiguration(siblingsFile);
		
		siblings = new HashSet<>();
		if (siblingsConfig.isSet("siblings")) {
			for (List<String> list : (List<List<String>>) siblingsConfig.getList("siblings")) {
				siblings.add(
					new HashSet<>(
							list.stream()
								.map(UUID::fromString)
								.collect(Collectors.toList())));
			}
		}
	}
	
	public void saveConfiguration() {
		siblingsConfig.set("siblings", siblings.stream().map(hs -> hs.stream().map(UUID::toString).collect(Collectors.toList())).collect(Collectors.toList()));
		try {
			siblingsConfig.save(siblingsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Pair<String,Collection<String>> getBannedPlayerNamesFromMsg(String msg) {
		Pair<String,Collection<String>> ret = new Pair<>();
		String[] split = msg.split("\n");
		String scanning = ChatColor.stripColor(split[0]);
		String otherAccounts = split[1];
		
		Matcher m = SCANNING_PATTERN.matcher(scanning);
		if (m.find()) {
			ret.setLeft(scanning.substring(m.start(), m.end()));
		}
		
		Collection<String> coloredUsers = new HashSet<>(Arrays.asList(otherAccounts.split(", ")));
		coloredUsers = coloredUsers.stream()
			.filter(str -> str.startsWith("§c") && !str.equals(ret.getLeft()))
			.map(str -> str.substring(2,(str.endsWith("§f") ? str.length()-2 : str.length())))
			.collect(Collectors.toList());
		
		ret.setRight(coloredUsers);
		
		return ret;
	}
}
