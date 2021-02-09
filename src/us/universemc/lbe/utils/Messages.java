package us.universemc.lbe.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public enum Messages {
	
	EVENT_SIBLINGS_JOINED("event.siblings.joined", "&l{USERNAME} and {SIBLINGS} are sibling accounts! Do not ban them for evading a ban!"),
	
	CMD_SIBLINGS_USAGE("cmd.siblings.usage", "&7Usage: /siblings <add/remove/reload> [args...]"),
	
	CMD_SIBLINGS_ADD_USAGE("cmd.siblings.add.usage", "&7Usage: /siblings add <username1> <username2> [username3]... [4]..."),
	CMD_SIBLINGS_ADD_ALLSIBLINGS("cmd.siblings.add.all-siblings", "&cThese users are already all siblings!"),
	CMD_SIBLINGS_ADD_SUCCESS("cmd.siblings.add.success", "&aSuccessfully linked {USERNAMES} as siblings!"),
	
	CMD_SIBLINGS_REMOVE_USAGE("cmd.siblings.remove.usage", "&7Usage: /siblings remove <username>"),
	CMD_SIBLINGS_REMOVE_SUCCESS("cmd.siblings.remove.success", "&aRemoved all siblings for {USERNAME}!"),
	
	CMD_RELOAD_SUCCESS("cmd.reload.success", "&aReloaded all configuration!"),
	
	;
	
	private static File file;
//	private static String longBoi = "";
//	static {
//		for (int i = 0; i < (280/6); i++)
//			longBoi += "e";
//	}
	private static Pattern functionPattern = Pattern.compile("\\['(.*)':(.*)\\]");
	
	private String path;
	private String[] defaultLines;
	private String[] customLines;
	
	Messages(String path, String... deflines) {
		this.path = path;
		this.defaultLines = deflines;
	}
	
	public void msg(CommandSender sender) {
		for (String line : getCustomLines()) {
			line = ChatColor.translateAlternateColorCodes('&', (line == null ? "" : line));
			if (line.isEmpty() && getCustomLines().length == 1) { continue; }
//			if (line.toLowerCase().startsWith("[center]")) {
//				line = line.substring("[center]".length());
//				TableGenerator gen = new TableGenerator(Alignment.CENTER);
//				gen.addRow(line);
//				gen.addRow(longBoi);
//				sender.sendMessage(gen.generate(sender instanceof Player ? Receiver.CLIENT : Receiver.CONSOLE, true).get(0));
//			} else {
				sender.sendMessage(line);
//			}
		}
	}
	
	public void msg(CommandSender sender, Map<String, String> replMap) {
		for (String line : getCustomLines()) {
			line = stringReplace(line, replMap);
			line = ChatColor.translateAlternateColorCodes('&', (line == null ? "" : line));
			if (line.isEmpty() && getCustomLines().length == 1) { continue; }
//			if (line.toLowerCase().startsWith("[center]")) {
//				line = line.substring("[center]".length());
//				TableGenerator gen = new TableGenerator(Alignment.CENTER);
//				gen.addRow(line);
//				gen.addRow(longBoi);
//				sender.sendMessage(gen.generate(sender instanceof Player ? Receiver.CLIENT : Receiver.CONSOLE, true).get(0));
//			} else {
				sender.sendMessage(line);
//			}
		}
	}
	
	public void broadcastPermission(String permission, HashMap<String,String> replMap) {
		for (String line : getCustomLines()) {
			line = stringReplace(line, replMap);
			line = ChatColor.translateAlternateColorCodes('&', (line == null ? "" : line));
			if (line.isEmpty() && getCustomLines().length == 1) { continue; }
			Bukkit.broadcast(line, permission);
		}
	}
	
	public void msgFunctions(CommandSender sender, Map<String,ComponentFunctionData> functions) {
		msgFunctions(sender, null, functions);
	}
	
	public void msgFunctions(CommandSender sender, Map<String,String> replMap, Map<String,ComponentFunctionData> functions) {
		for (String line : getCustomLines()) {
			ArrayList<BaseComponent> finalComponents = new ArrayList<>();
			line = stringReplace(line, replMap);
			line = ChatColor.translateAlternateColorCodes('&', (line == null ? "" : line));
			Matcher m = functionPattern.matcher(line);
			StringBuilder finalLine = new StringBuilder(line);
			while (m.find()) {
				String text = m.group(1);
				String function = m.group(2);
				if (functions.containsKey(function)) {
					ComponentFunctionData func = functions.get(function);
					BaseComponent[] comp = TextComponent.fromLegacyText(text);
					Arrays.stream(comp).forEach(bc -> {
						bc.setClickEvent(func.getClick());
						bc.setHoverEvent(func.getHover());
						bc.setInsertion(func.getInsertion());
					});
					String textBefore = finalLine.subSequence(0, m.start()).toString();
					finalComponents.addAll(Arrays.asList(TextComponent.fromLegacyText(textBefore)));
					finalLine.replace(0, m.end(), "");
					
					finalComponents.addAll(Arrays.asList(comp));
				}
			}
			finalComponents.addAll(Arrays.asList(TextComponent.fromLegacyText(finalLine.toString())));
			sender.spigot().sendMessage(finalComponents.toArray(new BaseComponent[0]));
		}
	}
	
	//---GETTERS---//
	public String getPath() {
		return path;
	}
	public String[] getCustomLines() {
		return (customLines == null ? defaultLines : customLines);
	}
	public String[] getDefaultLines() {
		return defaultLines;
	}
	
	//---STATIC---//
	public static void onEnable(JavaPlugin plugin, String filename) {
		onEnable(new File(plugin.getDataFolder(), filename));
	}
	public static void onEnable(File infile) {
		file = infile;
		saveDefaultMessages(file);
		loadCustomMessages(file);
	}
	public static void reload() {
		if (file == null) { return; }
		loadCustomMessages(file);
	}
	
	public static void loadCustomMessages(File file) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (Messages msg : values()) {
			if (!config.isSet(msg.path)) { continue; }
			
			if (config.isList(msg.path)) {
				msg.customLines = config.getStringList(msg.path).toArray(new String[]{});
			} else if (config.isString(msg.path)) {
				msg.customLines = new String[]{ config.getString(msg.path) };
			}
		}
	}
	
	public static void saveDefaultMessages(File file) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (Messages msg : values()) {
			if (config.isSet(msg.path)) { continue; }
			
			String[] lines = msg.defaultLines;
			if (lines.length > 1) {
				//multiple lines, set whole array
				config.set(msg.path, lines);
			} else if (lines.length == 1) {
				//one line, set only first element as to not make an array
				config.set(msg.path, lines[0]);
			}
		}
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String stringReplace(String input, Map<String,?> replMap) {
		if (input == null || input.equals("")) { return ""; }
		if (replMap == null) { return input; }
		
		for (Entry<String,?> replacement : replMap.entrySet()) {
			input = input.replace(replacement.getKey(), "" + replacement.getValue());
		}
		return input;
	}
	
	@Data @AllArgsConstructor
	public static class ComponentFunctionData {
		private HoverEvent hover;
		private ClickEvent click;
		private String insertion;
	}
}
