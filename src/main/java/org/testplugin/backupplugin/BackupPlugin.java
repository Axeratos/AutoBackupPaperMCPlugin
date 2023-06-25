package org.testplugin.backupplugin;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.testplugin.backupplugin.utils.GoogleDriveOperationsController;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class BackupPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Server server = Bukkit.getServer();
                Bukkit.getLogger().info("[PLUGIN] Started periodic task");
                if (server.getOnlinePlayers().size() > 0) {
                    try {
                        Bukkit.getLogger().info("[START] Players on the server. Start backup.");
                        GoogleDriveOperationsController.backup();
                        Bukkit.getLogger().info("[START] Backup is successful");
                    } catch (GeneralSecurityException e) {
                        Bukkit.getLogger().info("Error while connecting to google drive");
                    } catch (IOException e) {
                        Bukkit.getLogger().info("I/O Exception occurred: " + e);
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 1000, 2 * 60 * 60);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
//    public static void main(String[] args) {
//        try {
//            GoogleDriveOperationsController.backup();
//        } catch (GeneralSecurityException e) {
//            System.out.println(e);
//        } catch (IOException e) {
//
//            System.out.println(e);
//        }
//    }
}
