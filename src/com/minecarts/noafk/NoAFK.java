package com.minecarts.noafk;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class NoAFK extends JavaPlugin implements Listener {
    static final Logger logger = Logger.getLogger("com.minecarts.noafk");
    
    protected Map<Player, DatedVector> lastDirection = new WeakHashMap<Player, DatedVector>();
    protected Map<Player, String> lastDisplayName = new WeakHashMap<Player, String>();
    protected Map<Player, String> lastPlayerListName = new WeakHashMap<Player, String>();
    
    protected List<Map<String, Object>> kickSettings;
    
    
    public void onEnable() {
        reloadConfig();
        
        // internal plugin commands
        getCommand("noafk").setExecutor(new CommandExecutor() {
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if(!sender.hasPermission("noafk.admin.reload")) return true; // "hide" command output for nonpermissibles
                
                if(args[0].equalsIgnoreCase("reload")) {
                    NoAFK.this.reloadConfig();
                    sender.sendMessage("NoAFK config reloaded.");
                    log("Config reloaded by {0}", sender.getName());
                    return true;
                }
                
                return false;
            }
        });
        

        getServer().getPluginManager().registerEvents(this, this);
        
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for(Player player : getServer().getOnlinePlayers()) {
                    DatedVector last = lastDirection.get(player);
                    DatedVector now = new DatedVector(player.getLocation().getDirection());

                    if(last == null) {
                        lastDirection.put(player, now);
                    }
                    else if(!last.equals(now)) {
                        wakePlayer(player);
                    }
                    else if(last.elapsed() > 1000 * 60 * 5) {
                        sleepPlayer(player);
                    }
                    
                    if(isAFK(player)) {
                        Integer timeout = null;
                        String message = null;
                        
                        if(kickSettings != null) {
                            for(Map<String, Object> settings : kickSettings) {
                                String permission = (String) settings.get("permission");
                                if(permission != null) {
                                    if(player.isPermissionSet(permission) && player.hasPermission(permission)) {
                                        if(timeout == null) timeout = (Integer) settings.get("timeout");
                                        if(message == null) message = (String) settings.get("message");
                                    }
                                }
                                else {
                                    if(timeout == null) timeout = (Integer) settings.get("timeout");
                                    if(message == null) message = (String) settings.get("message");
                                }
                            }
                        }
                        
                        if(timeout != null && timeout > 0 && last.elapsed() > timeout * 1000) {
                            log("Kicking {0} for {1} seconds of inactivity with message: {2}", player.getName(), timeout, message);
                            player.kickPlayer(message);
                        }
                    }
                }
            }
        }, 0, 20 * 30);

        
        log("Version {0} enabled.", getDescription().getVersion());
    }
    
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        final FileConfiguration config = getConfig();
        
        try {
            logger.setLevel(Level.parse(config.getString("log.level")));
        
            kickSettings = config.getMapList("kick");
            kickSettings.addAll(config.getDefaultSection().getMapList("kick"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public boolean isAFK(Player player) {
        return lastDisplayName.containsKey(player);
    }
    
    
    private void sleepPlayer(Player player) {
        if(isAFK(player)) return;
        
        String name = org.bukkit.ChatColor.GRAY + player.getName();
        lastDisplayName.put(player, player.getDisplayName());
        player.setDisplayName(name);
        lastPlayerListName.put(player, player.getPlayerListName());
        player.setPlayerListName(name);
        
        log("{0} is AFK", player.getName());
    }
    
    private void wakePlayer(Player player) {
        if(isAFK(player)) {
            player.setDisplayName(lastDisplayName.remove(player));
            player.setPlayerListName(lastPlayerListName.remove(player));

            log("{0} is no longer AFK", player.getName());
        }
        lastDirection.put(player, new DatedVector(player.getLocation().getDirection()));
    }
    
    
    
    @EventHandler
    public void on(org.bukkit.event.player.PlayerChatEvent event) {
        wakePlayer(event.getPlayer());
    }
    @EventHandler
    public void on(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        wakePlayer(event.getPlayer());
    }
    @EventHandler
    public void on(org.bukkit.event.player.PlayerLoginEvent event) {
        wakePlayer(event.getPlayer());
    }
    @EventHandler
    public void on(org.bukkit.event.player.PlayerQuitEvent event) {
        lastDirection.remove(event.getPlayer());
        lastDisplayName.remove(event.getPlayer());
        lastPlayerListName.remove(event.getPlayer());
    }
    
    
    public void log(String message) {
        log(Level.INFO, message);
    }
    public void log(Level level, String message) {
        logger.log(level, MessageFormat.format("{0}> {1}", getDescription().getName(), message));
    }
    public void log(String message, Object... args) {
        log(MessageFormat.format(message, args));
    }
    public void log(Level level, String message, Object... args) {
        log(level, MessageFormat.format(message, args));
    }
    
    public void debug(String message) {
        log(Level.FINE, message);
    }
    public void debug(String message, Object... args) {
        log(Level.FINE, message, args);
    }
}