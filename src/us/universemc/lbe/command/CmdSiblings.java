package us.universemc.lbe.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import lombok.RequiredArgsConstructor;
import us.universemc.lbe.LiteBansExtension;
import us.universemc.lbe.utils.Messages;

@RequiredArgsConstructor
public class CmdSiblings implements CommandExecutor {

	private final LiteBansExtension main;
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (args.length <= 0) {
			Messages.CMD_SIBLINGS_USAGE.msg(sender);
			return true;
		}
		
		HashMap<String,String> replMap = new HashMap<>();
		switch(args[0].toLowerCase()) {
		case "add": {
			if (args.length < 3) {
				Messages.CMD_SIBLINGS_ADD_USAGE.msg(sender);
				return true;
			}
			
			HashSet<UUID> siblings = new HashSet<>();
			for (int i = 1; i < args.length; i++) {
				OfflinePlayer sibling = Bukkit.getOfflinePlayer(args[i]);
				siblings.add(sibling.getUniqueId());
			}
			
			if (main.getSiblings().contains(siblings)) {
				Messages.CMD_SIBLINGS_ADD_ALLSIBLINGS.msg(sender);
				return true;
			}
			
			main.getSiblings().add(siblings);
			main.saveConfiguration();
			replMap.put("{USERNAMES}", 
					siblings.stream()
						.map(Bukkit::getOfflinePlayer)
						.map(OfflinePlayer::getName)
						.collect(Collectors.joining(", ")));
			Messages.CMD_SIBLINGS_ADD_SUCCESS.msg(sender, replMap);
			return true;
		}
		case "remove": {
			if (args.length < 2) {
				Messages.CMD_SIBLINGS_REMOVE_USAGE.msg(sender);
				return true;
			}
			
			OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
			UUID uuid = target.getUniqueId();
			
			Iterator<HashSet<UUID>> siblings = main.getSiblings().iterator();
			while (siblings.hasNext()) {
				HashSet<UUID> next = siblings.next();
				if (next.contains(uuid)) {
					next.remove(uuid);
					if (next.size() <= 1) {
						siblings.remove();
					}
				}
			}
			
			replMap.put("{USERNAME}", target.getName());
			main.saveConfiguration();
			Messages.CMD_SIBLINGS_REMOVE_SUCCESS.msg(sender, replMap);
			return true;
		}
		case "reload": {
			main.loadConfiguration();
			Messages.CMD_RELOAD_SUCCESS.msg(sender);
			return true;
		}
		}
		
		Messages.CMD_SIBLINGS_USAGE.msg(sender);
		return true;
	}

}
